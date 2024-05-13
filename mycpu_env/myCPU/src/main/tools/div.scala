package mycpu

import chisel3._
import chisel3.util._
import common._
import common.const._
import config.CpuConfig._

class signed_div extends BlackBox with HasBlackBoxResource {
  val io = IO(new Bundle {
    val aclk = Input(Clock())
    // 除数
    val s_axis_divisor_tvalid = Input(Bool())
    val s_axis_divisor_tready = Output(Bool())
    val s_axis_divisor_tdata  = Input(UInt(WORD.W))
    // 被除数
    val s_axis_dividend_tvalid = Input(Bool())
    val s_axis_dividend_tready = Output(Bool())
    val s_axis_dividend_tdata  = Input(UInt(WORD.W))
    // 结果
    val m_axis_dout_tvalid = Output(Bool())
    val m_axis_dout_tdata  = Output(UInt(LONG.W))
  })
}

class unsigned_div extends BlackBox with HasBlackBoxResource {
  val io = IO(new Bundle {
    val aclk = Input(Clock())
    // 除数
    val s_axis_divisor_tvalid = Input(Bool())
    val s_axis_divisor_tready = Output(Bool())
    val s_axis_divisor_tdata  = Input(UInt(WORD.W))
    // 被除数
    val s_axis_dividend_tvalid = Input(Bool())
    val s_axis_dividend_tready = Output(Bool())
    val s_axis_dividend_tdata  = Input(UInt(WORD.W))
    // 结果
    val m_axis_dout_tvalid = Output(Bool())
    val m_axis_dout_tdata  = Output(UInt(LONG.W))
  })
}

class Div() extends Module {
  val io = IO(new Bundle {
    val src1        = Input(UInt(WORD.W))
    val src2        = Input(UInt(WORD.W))
    val signed      = Input(Bool())
    val start       = Input(Bool())
    val allow_to_go = Input(Bool())

    val ready  = Output(Bool())
    val result = Output(UInt(LONG.W))
  })

  if (cpuBuild) {
    val sdiv = Module(new signed_div()).io
    val udiv = Module(new unsigned_div()).io

    sdiv.aclk   := clock
    udiv.aclk   := clock

    // 0为被除数，1为除数
    val udiv_sent = Seq.fill(2)(RegInit(false.B))
    val udiv_done = RegInit(false.B)
    val sdiv_sent = Seq.fill(2)(RegInit(false.B))
    val sdiv_done = RegInit(false.B)

    when(udiv.s_axis_dividend_tready && udiv.s_axis_dividend_tvalid) {
      udiv_sent(0) := true.B
    }.elsewhen(io.ready && io.allow_to_go) {
      udiv_sent(0) := false.B
    }
    when(udiv.s_axis_divisor_tready && udiv.s_axis_divisor_tvalid) {
      udiv_sent(1) := true.B
    }.elsewhen(io.ready && io.allow_to_go) {
      udiv_sent(1) := false.B
    }

    when(sdiv.s_axis_dividend_tready && sdiv.s_axis_dividend_tvalid) {
      sdiv_sent(0) := true.B
    }.elsewhen(io.ready && io.allow_to_go) {
      sdiv_sent(0) := false.B
    }
    when(sdiv.s_axis_divisor_tready && sdiv.s_axis_divisor_tvalid) {
      sdiv_sent(1) := true.B
    }.elsewhen(io.ready && io.allow_to_go) {
      sdiv_sent(1) := false.B
    }

    when(sdiv.m_axis_dout_tvalid && !io.allow_to_go) {
      sdiv_done := true.B
    }.elsewhen(io.allow_to_go) {
      sdiv_done := false.B
    }

    when(udiv.m_axis_dout_tvalid && !io.allow_to_go) {
      udiv_done := true.B
    }.elsewhen(io.allow_to_go) {
      udiv_done := false.B
    }
    // 被除数和除数的valid信号
    sdiv.s_axis_dividend_tvalid := io.start && !sdiv_sent(0) && io.signed
    sdiv.s_axis_divisor_tvalid  := io.start && !sdiv_sent(1) && io.signed

    udiv.s_axis_dividend_tvalid := io.start && !udiv_sent(0) && !io.signed
    udiv.s_axis_divisor_tvalid  := io.start && !udiv_sent(1) && !io.signed

    // 被除数和除数的值
    sdiv.s_axis_dividend_tdata := io.src1
    sdiv.s_axis_divisor_tdata  := io.src2

    udiv.s_axis_dividend_tdata := io.src1
    udiv.s_axis_divisor_tdata  := io.src2

    io.ready := Mux(
      io.signed,
      sdiv.m_axis_dout_tvalid || sdiv_done,
      udiv.m_axis_dout_tvalid || udiv_done,
    )
    val sres = Cat(sdiv.m_axis_dout_tdata(WORD - 1, 0), sdiv.m_axis_dout_tdata(LONG - 1, WORD))
    val ures = Cat(udiv.m_axis_dout_tdata(WORD - 1, 0), udiv.m_axis_dout_tdata(LONG - 1, WORD))
    io.result := Mux(io.signed, sres, ures)
  } else {
    val cnt = RegInit(0.U(4.W))
    cnt := MuxCase(
      cnt,
      Seq(
        (io.start && !io.ready) -> (cnt + 1.U),
        io.allow_to_go          -> 0.U,
      ),
    )

    val div_signed = io.signed

    val dividend_signed = io.src1(31) & div_signed
    val divisor_signed  = io.src2(31) & div_signed

    val dividend_abs = Mux(dividend_signed, (-io.src1).asUInt, io.src1.asUInt)
    val divisor_abs  = Mux(divisor_signed, (-io.src2).asUInt, io.src2.asUInt)

    val quotient_signed  = (io.src1(31) ^ io.src2(31)) & div_signed
    val remainder_signed = io.src1(31) & div_signed

    val quotient_abs  = dividend_abs / divisor_abs
    val remainder_abs = dividend_abs - quotient_abs * divisor_abs

    val quotient  = RegInit(0.S(WORD.W))
    val remainder = RegInit(0.S(WORD.W))

      quotient  := Mux(quotient_signed, (-quotient_abs).asSInt, quotient_abs.asSInt)
      remainder := Mux(remainder_signed, (-remainder_abs).asSInt, remainder_abs.asSInt)
      
    io.ready  := cnt >= cpuDivClkNum
    io.result := Cat(remainder, quotient)
  }
}