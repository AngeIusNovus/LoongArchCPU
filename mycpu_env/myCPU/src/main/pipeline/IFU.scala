package mycpu

import chisel3._
import chisel3.util._
import common._
import common.const._

class IF_Stage extends Module {
    val io = IO(new Bundle {
        val inst = new CPU_AXI_RD_IO()
        val br = Flipped(new BR_OUT())
        val to_ds = new DS_BUS()
        val ds_allowin = Input(Bool())
        val ds_flush   = Input(Bool())
        val csr_taken  = Input(Bool())
        val csr_target = Input(UInt(WORD.W))
    })

    val to_fs_valid = Wire(Bool())
    val fs_ready_go = Wire(Bool())
    val fs_valid = RegInit(false.B)
    val fs_allowin = Wire(Bool())

    to_fs_valid := RegNext(!reset.asBool) && (!reset.asBool)
    fs_ready_go := RegEnable(io.inst.r.valid, io.inst.r.valid)
    fs_allowin  := (!fs_valid) || (fs_ready_go && io.ds_allowin) && !io.ds_flush
    when (fs_allowin) {
        fs_valid := to_fs_valid
    }

    val pc     = RegInit("h1bfffffc".asUInt(32.W))
    val seq_pc = Wire(UInt(32.W))
    val nxt_pc = Wire(UInt(32.W))
    val csr_taken  = RegNext(io.csr_taken)
    val csr_target = RegNext(io.csr_target)
    val ADEF = Wire(Bool())
    ADEF := (pc(1, 0) =/= 0.U(2.W))

    seq_pc := pc + 4.U
    nxt_pc := MuxCase(seq_pc, Seq(
        csr_taken   -> csr_target,
        io.br.taken -> io.br.target
    ))

    io.inst.ar.id       := 0.U(4.W)
    io.inst.ar.addr     := nxt_pc
    io.inst.ar.len      := 0.U(8.W)
    io.inst.ar.size     := 4.U(3.W)
    io.inst.ar.burst    := 1.U(2.W)
    io.inst.ar.lock     := 0.U(2.W)
    io.inst.ar.cache    := 0.U(4.W)
    io.inst.ar.prot     := 0.U(3.W)
    io.inst.arvalid     := true.B

    val now_inst = RegEnable(io.inst.r.data, io.inst.r.valid)

    io.to_ds.pc    := pc
    io.to_ds.inst  := Mux(io.to_ds.valid, now_inst, 0.U(32.W))
    io.to_ds.valid := fs_valid && fs_ready_go && !io.br.taken && !csr_taken && !io.ds_flush
    io.to_ds.ADEF  := ADEF

    when (to_fs_valid && fs_allowin) {
        pc := nxt_pc
    }
}