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
        val csr = new CSR_IO()
        val es_flush = Output(Bool())
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
    val mem_re      = RegInit(0.U(MEM_RED_LEN.W))
    val mem_we      = RegInit(0.U(BYTE_LEN.W))
    val rf_we       = RegInit(0.U(RF_SEL_LEN.W))
    val wb_src      = RegInit(0.U(WB_SEL_LEN.W))
    val pc          = RegInit(0.U(WORD.W))
    val rd_value    = RegInit(0.U(WORD.W))
    // val csr = RegEnable(io.csr, io.es_allowin && io.to_es.valid)
    val csr_Excp    = RegInit(0.U(EXCP_LEN.W))
    val csr_Ecode   = RegInit(0.U(ECODE_LEN.W))
    val csr_Esubcode= RegInit(0.U(ESUBCODE_LEN.W))
    val csr_pc      = RegInit(0.U(WORD.W))
    val csr_en_mask = RegInit(false.B)
    val csr_we      = RegInit(false.B)
    val csr_waddr   = RegInit(0.U(CSR_ADDR.W))
    val csr_wdata   = RegInit(0.U(WORD.W))
    val csr_mask    = RegInit(0.U(WORD.W))
    val csr_raddr   = RegInit(0.U(CSR_ADDR.W))
    val csr_rdata   = RegInit(0.U(WORD.W))

    when (io.es_allowin && io.to_es.valid) {
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
        csr_Excp    := io.to_es.csr.Excp
        csr_Ecode   := io.to_es.csr.Ecode
        csr_Esubcode:= io.to_es.csr.Esubcode
        csr_pc      := io.to_es.csr.pc
        csr_en_mask := io.to_es.csr.en_mask
        csr_we      := io.to_es.csr.we
        csr_waddr   := io.to_es.csr.waddr
        csr_wdata   := io.to_es.csr.wdata
        csr_mask    := io.to_es.csr.mask
        csr_raddr   := io.to_es.csr.raddr
    }

    val alu_result = Wire(UInt(WORD.W))

    val u_alu = Module(new ALU())
    u_alu.io.aluOp   := Mux(csr_Excp === 0.U(EXCP_LEN.W), alu_op, 0.U(OP_LEN.W))
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
    io.to_ms.csr_data := io.csr.rdata

    io.es_flush     := es_valid && (csr_Excp =/= 0.U(EXCP_LEN.W))
    io.csr.Excp     := csr_Excp & Fill(2, es_valid.asUInt)
    io.csr.Ecode    := csr_Ecode
    io.csr.Esubcode := csr_Esubcode
    io.csr.pc       := csr_pc
    io.csr.en_mask  := csr_en_mask
    io.csr.we       := csr_we
    io.csr.waddr    := csr_waddr
    io.csr.wdata    := csr_wdata
    io.csr.mask     := csr_mask
    io.csr.raddr    := csr_raddr

    val final_we = Wire(UInt(BYTE_LEN.W))
    val off = alu_result(1, 0)

    final_we := MuxCase(mem_we, Seq(
        (off === 1.U(2.W)) -> Cat(mem_we(2, 0), mem_we(3)),
        (off === 2.U(2.W)) -> Cat(mem_we(1, 0), mem_we(3, 2)),
        (off === 3.U(2.W)) -> Cat(mem_we(0), mem_we(3, 1))
    ))

    io.data.en := (mem_re =/= 0.U(BYTE_LEN.W))
    io.data.we := Mux(!es_valid || (csr_Excp =/= 0.U(EXCP_LEN.W)), 0.U(BYTE_LEN.W), final_we)
    io.data.addr := alu_result
    io.data.wdata := MuxCase(rd_value, Seq(
        (mem_we === MEM_WB) -> Fill(4, rd_value(7, 0)),
        (mem_we === MEM_WH) -> Fill(2, rd_value(15, 0))
    ))

    io.rd_es.valid := (rf_we =/= 0.U(REG.W)) && es_valid && (dest =/= 0.U(REG.W))
    io.rd_es.ready := wb_src =/= WB_MEM
    io.rd_es.dest  := dest
    io.rd_es.data  := MuxCase(io.csr.rdata, Seq(
        (wb_src === WB_ALU) -> alu_result,
        (wb_src === WB_PC)  -> (pc + 4.U(WORD.W)),
        (wb_src === WB_X)   -> (0.U(WORD.W))
    ))
}