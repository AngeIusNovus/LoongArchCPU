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
        val data = new RAM_IO()
        val rd_es = new TMP_REG()
    })

    val es_valid = RegInit(false.B)
    val es_ready_go = Wire(Bool())

    io.es_allowin := (!es_valid) | (io.ms_allowin & es_ready_go)
    when (io.es_allowin) {
        es_valid := io.to_es.valid
    }
    io.to_ms.valid := es_valid & es_ready_go

    val dest        = RegInit(0.U(WORD.W))
    val alu_op      = RegInit(0.U(OP_LEN.W))
    val src1_data   = RegInit(0.U(WORD.W))
    val src2_data   = RegInit(0.U(WORD.W))
    val mem_re      = RegInit(0.U(BYTE_LEN.W))
    val mem_we      = RegInit(0.U(BYTE_LEN.W))
    val rf_we       = RegInit(0.U(RF_SEL_LEN.W))
    val wb_src      = RegInit(0.U(WB_SEL_LEN.W))
    val pc          = RegInit(0.U(WORD.W))
    val rd_value    = RegInit(0.U(WORD.W))

    when (io.es_allowin & io.to_es.valid) {
        dest        := io.to_es.dest
        alu_op      := io.to_es.alu_op
        src1_data   := io.to_es.src1_data
        src2_data   := io.to_es.src2_data
        mem_re      := io.to_es.mem_re
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
    es_ready_go      := u_alu.io.ready
    
    io.to_ms.wb_src   := wb_src
    io.to_ms.rf_we    := rf_we
    io.to_ms.dest     := dest
    io.to_ms.rd_value := rd_value
    io.to_ms.dest     := dest
    io.to_ms.alu_res  := alu_result
    io.to_ms.pc       := pc
    io.to_ms.mem_re   := mem_re
    io.to_ms.mem_we   := mem_we
    io.to_ms.addr     := alu_result

    val final_we = Wire(UInt(BYTE_LEN.W))
    val off = alu_result(1, 0)

    final_we := MuxCase(mem_we, Seq(
        (off === 1.U(2.W)) -> Cat(mem_we(2, 0), mem_we(3)),
        (off === 2.U(2.W)) -> Cat(mem_we(1, 0), mem_we(3, 2)),
        (off === 3.U(2.W)) -> Cat(mem_we(0), mem_we(3, 1))
    ))

    io.data.en := (mem_re =/= 0.U(BYTE_LEN.W))
    io.data.we := Mux(es_valid, final_we, 0.U(BYTE_LEN.W))
    io.data.addr := alu_result
    io.data.wdata := MuxCase(rd_value, Seq(
        (mem_we === MEM_WB) -> Fill(4, rd_value(7, 0)),
        (mem_we === MEM_WH) -> Fill(2, rd_value(15, 0))
    ))

    io.rd_es.valid := (rf_we =/= 0.U(REG.W)) && es_valid && (dest =/= 0.U(REG.W))
    io.rd_es.ready := wb_src =/= WB_MEM
    io.rd_es.dest  := dest
    io.rd_es.data  := Mux(wb_src === WB_ALU, alu_result, pc + 4.U(WORD.W))
}