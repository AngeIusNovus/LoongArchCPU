package mycpu

import chisel3._
import chisel3.util._
import common._
import common.const._

class EXE_Stage extends Module {
    val io = IO(new Bundle{
        val to_es = Flipped(new EX_BUS())
        val to_ms = new ME_BUS()
        val es_allowin = Output(Bool())
        val ms_allowin = Input(Bool())
        val rd_es = Output(UInt(REG.W))
    })

    val es_valid = RegInit(false.B)
    val es_ready_go = Wire(Bool())

    es_ready_go := true.B
    io.es_allowin := (!es_valid) | (io.ms_allowin & es_ready_go)
    when (io.es_allowin) {
        es_valid := io.to_es.valid
    }
    io.to_ms.valid := es_valid & es_ready_go

    val dest        = RegInit(0.U(WORD.W))
    val alu_op      = RegInit(0.U(OP_LEN.W))
    val src1_data   = RegInit(0.U(WORD.W))
    val src2_data   = RegInit(0.U(WORD.W))
    val mem_en      = RegInit(false.B)
    val mem_we      = RegInit(0.U(MEM_SEL_LEN.W))
    val rf_we       = RegInit(0.U(RF_SEL_LEN.W))
    val wb_src      = RegInit(0.U(WB_SEL_LEN.W))
    val pc          = RegInit(0.U(WORD.W))
    val rd_value    = RegInit(0.U(WORD.W))

    when (io.es_allowin & io.to_es.valid) {
        dest        := io.to_es.dest
        alu_op      := io.to_es.alu_op
        src1_data   := io.to_es.src1_data
        src2_data   := io.to_es.src2_data
        mem_en      := io.to_es.mem_en
        mem_we      := io.to_es.mem_we
        rf_we       := io.to_es.rf_we
        wb_src      := io.to_es.wb_src
        pc          := io.to_es.pc
        rd_value    := io.to_es.rd_value
    }

    val alu_result = Wire(UInt(WORD.W))

    val u_alu = Module(new ALU())
    u_alu.io.aluOp   := alu_op
    u_alu.io.aluSrc1 := src1_data
    u_alu.io.aluSrc2 := src2_data
    alu_result       := u_alu.io.aluResult

    io.to_ms.wb_src   := wb_src
    io.to_ms.rf_we    := rf_we
    io.to_ms.dest     := dest
    io.to_ms.rd_value := rd_value
    io.to_ms.dest     := dest
    io.to_ms.alu_res  := alu_result
    io.to_ms.pc       := pc
    io.to_ms.mem_en   := mem_en
    io.to_ms.mem_we   := mem_we

    io.rd_es := Mux(es_valid, dest, 0.U(REG.W))
}