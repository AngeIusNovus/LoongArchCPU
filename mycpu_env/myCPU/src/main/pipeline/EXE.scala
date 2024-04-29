package mycpu

import chisel3._
import chisel3.util._
import common._
import common.const._

class EXE_Stage extends Module {
    val io = IO(new Bundle{
        val to_ex = Flipped(new EX_BUS())
        val to_me = new ME_BUS()
        val es_allowin = Output(Bool())
        val ms_allowin = Input(Bool())
    })

    val es_valid = RegInit(false.B)
    val es_ready_go = Wire(Bool())

    es_ready_go := true.B
    io.es_allowin := !es_valid | (io.ms_allowin & es_ready_go)
    when (io.es_allowin) {
        es_valid := io.to_ex.valid
    }
    io.to_me.valid := es_valid & es_ready_go

    val dest        = RegInit(0.U(WORD.W))
    val alu_op      = RegInit(0.U(OP_LEN.W))
    val src1_data   = RegInit(0.U(WORD.W))
    val src2_data   = RegInit(0.U(WORD.W))
    val mem_we      = RegInit(0.U(4.W))
    val rf_we       = RegInit(0.U(4.W))
    val wb_sel      = RegInit(0.U(WB_SEL_LEN.W))
    val pc          = RegInit(0.U(WORD.W))
    val rd_value    = RegInit(0.U(WORD.W))

    when (io.es_allowin & io.to_ex.valid) {
        dest        := io.to_ex.dest
        alu_op      := io.to_ex.alu_op
        src1_data   := io.to_ex.src1_data
        src2_data   := io.to_ex.src2_data
        mem_we      := io.to_ex.mem_we
        rf_we       := io.to_ex.rf_we
        wb_sel      := io.to_ex.wb_sel
        pc          := io.to_ex.pc
        rd_value    := io.to_ex.rd_value
    }

    val alu_result = Wire(UInt(WORD.W))

    val u_alu = Module(new ALU())
    u_alu.io.aluOp   := alu_op
    u_alu.io.aluSrc1 := src1_data
    u_alu.io.aluSrc2 := src2_data
    alu_result       := u_alu.io.aluResult

    io.to_me.wb_sel   := wb_sel
    io.to_me.rf_we    := rf_we
    io.to_me.dest     := dest
    io.to_me.rd_value := rd_value
    io.to_me.dest     := dest
    io.to_me.alu_res  := alu_result
    io.to_me.pc       := pc
    io.to_me.mem_we   := mem_we
}