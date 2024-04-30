package mycpu

import chisel3._
import chisel3.util._
import common._
import common.const._

class IF_Stage extends Module {
    val io = IO(new Bundle {
        val inst = new RAM_IO()
        val br = Flipped(new BR_OUT())
        val to_ds = new DS_BUS()
        val ds_allowin = Input(UInt(1.W))
    })

    val to_fs_valid = Wire(Bool())
    val fs_ready_go = Wire(Bool())
    val fs_valid = RegInit(false.B)
    val fs_allowin = Wire(Bool())

    to_fs_valid := RegNext(!reset.asBool) & (!reset.asBool)
    fs_ready_go := true.B
    fs_allowin  := (!fs_valid) | (fs_ready_go & io.ds_allowin)
    when (fs_allowin) {
        fs_valid := to_fs_valid
    }
    io.to_ds.valid := fs_valid & fs_ready_go

    val pc     = RegInit("h1bfffffc".asUInt(32.W))
    val seq_pc = Wire(UInt(32.W))
    val nxt_pc = Wire(UInt(32.W))

    seq_pc := pc + 4.U
    nxt_pc := Mux(io.br.taken, io.br.target, seq_pc)

    io.inst.we    := 0.U(4.W)
    io.inst.en    := to_fs_valid & fs_allowin
    io.inst.addr  := nxt_pc
    io.inst.wdata := 0.U(WORD.W)

    io.to_ds.pc    := pc
    io.to_ds.inst  := io.inst.rdata

    when (to_fs_valid && fs_allowin) {
        pc := nxt_pc
    }
}