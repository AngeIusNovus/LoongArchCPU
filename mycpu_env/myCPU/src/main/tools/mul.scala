package mycpu

import chisel3._
import chisel3.util._
import common._
import common.const._
import config.CpuConfig._

class SignedMul extends BlackBox with HasBlackBoxResource {
  val io = IO(new Bundle {
    val CLK = Input(Clock())
    val CE  = Input(Bool())
    val A   = Input(UInt((WORD + 1).W))
    val B   = Input(UInt((WORD + 1).W))
    val P   = Output(UInt((LONG + 2).W))
  })
}

class Mul() extends Module {
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
    val signedMul = Module(new SignedMul()).io
    val cnt       = RegInit(0.U(3.W))

    cnt := MuxCase(cnt, Seq(
        (io.start && !io.ready) -> (cnt + 1.U),
        io.allow_to_go          -> 0.U
      ))

    signedMul.CLK := clock
    signedMul.CE  := io.start
    when(io.signed) {
      signedMul.A := Cat(io.src1(WORD - 1), io.src1)
      signedMul.B := Cat(io.src2(WORD - 1), io.src2)
    }.otherwise {
      signedMul.A := Cat(0.U(1.W), io.src1)
      signedMul.B := Cat(0.U(1.W), io.src2)
    }
    io.ready  := cnt >= cpuMulClkNum
    io.result := signedMul.P(LONG - 1, 0)
  } else {
    val cnt = RegInit(0.U(3.W))
    cnt := MuxCase(cnt, Seq(
        (io.start && !io.ready) -> (cnt + 1.U),
        io.allow_to_go          -> 0.U
      ))

    val signed   = Wire(UInt(LONG.W))
    val unsigned = Wire(UInt(LONG.W))
    
      signed   := (io.src1.asSInt * io.src2.asSInt).asUInt
      unsigned := io.src1 * io.src2
    
    io.result := Mux(io.signed, signed, unsigned)
    io.ready  := true.B
  }
}