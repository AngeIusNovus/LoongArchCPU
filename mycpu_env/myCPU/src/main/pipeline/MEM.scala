package mycpu

import chisel3._
import chisel3.util._
import common._
import common.const._

class MEM_Stage extends Module {
    val io = IO(new Bundle{
        val to_ms = Flipped(new ME_BUS())
        val to_ws = new WB_BUS()
        val ms_allowin = Output(Bool())
        val ws_allowin = Input(Bool())
        val data_rdata = Input(UInt(WORD.W))
        val rd_ms = new TMP_REG()
    })

    val ms_valid = RegInit(false.B)
    val ms_ready_go = Wire(Bool())

    ms_ready_go := true.B
    io.ms_allowin := (!ms_valid) | (io.ws_allowin & ms_ready_go)
    when (io.ms_allowin) {
        ms_valid := io.to_ms.valid
    }
    io.to_ws.valid := ms_valid & ms_ready_go

    val alu_res     = RegInit(0.U(WORD.W))
    val mem_re      = RegInit(0.U(MEM_RED_LEN.W))
    val mem_we      = RegInit(0.U(BYTE_LEN.W))
    val rf_we       = RegInit(0.U(RF_SEL_LEN.W))
    val wb_src      = RegInit(0.U(WB_SEL_LEN.W))
    val dest        = RegInit(0.U(REG.W))
    val rd_value    = RegInit(0.U(WORD.W))
    val addr        = RegInit(0.U(WORD.W))
    val pc          = RegInit(0.U(WORD.W))
    val csr_data    = RegInit(0.U(WORD.W))

    when (io.ms_allowin & io.to_ms.valid) {
        alu_res := io.to_ms.alu_res
        wb_src := io.to_ms.wb_src
        rf_we := io.to_ms.rf_we
        mem_re := io.to_ms.mem_re
        mem_we := io.to_ms.mem_we
        dest := io.to_ms.dest
        rd_value := io.to_ms.rd_value
        addr := io.to_ms.addr
        pc := io.to_ms.pc 
        csr_data := io.to_ms.csr_data
    }

    val off = addr(1, 0)
    val final_rdata = Wire(UInt(WORD.W))
    val tmp_rdata = Wire(UInt(WORD.W))

    tmp_rdata := MuxCase(io.data_rdata, Seq(
        (off === 0.U(2.W)) -> io.data_rdata(15, 0),
        (off === 1.U(2.W)) -> io.data_rdata(23, 8),
        (off === 2.U(2.W)) -> io.data_rdata(31, 16),
        (off === 3.U(2.W)) -> io.data_rdata(31, 24)
    ))

    final_rdata := MuxCase(io.data_rdata, Seq(
        (mem_re === MEM_RB) -> Cat(Fill(24, tmp_rdata(7)), tmp_rdata(7, 0)),
        (mem_re === MEM_RH) -> Cat(Fill(16, tmp_rdata(15)), tmp_rdata(15, 0)),
        (mem_re === MEM_RBU) -> Cat(0.U(24.W), tmp_rdata(7, 0)),
        (mem_re === MEM_RHU) -> Cat(0.U(24.W), tmp_rdata(15, 0))
    ))

    val wb_data = Wire(UInt(WORD.W))
    wb_data := MuxCase(0.U, Seq(
        (wb_src === WB_ALU) -> alu_res,
        (wb_src === WB_CSR) -> csr_data,
        (wb_src === WB_BOTH) -> csr_data,
        (wb_src === WB_MEM) -> final_rdata,
        (wb_src === WB_PC) -> (pc + 4.U(WORD.W))
    ))

    io.to_ws.wb_src := wb_src
    io.to_ws.rf_we := rf_we
    io.to_ws.dest := dest
    io.to_ws.wb_data := wb_data
    io.to_ws.pc := pc

    io.rd_ms.valid := (rf_we =/= 0.U(BYTE_LEN.W)) && ms_valid && (dest =/= 0.U(REG.W))
    io.rd_ms.ready := true.B
    io.rd_ms.dest  := dest
    io.rd_ms.data  := wb_data
}