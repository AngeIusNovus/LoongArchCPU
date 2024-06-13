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
        val data = new CPU_AXI_RD_IO()
        val rd_ms = new TMP_REG()
    })

    val ms_valid = RegInit(false.B)
    val ms_ready_go = Wire(Bool())

    val ms = RegEnable(io.to_ms, io.ms_allowin && io.to_ms.valid)
    
    ms_ready_go := (ms.wb_src =/= WB_MEM) || io.data.r.valid
    io.ms_allowin := (!ms_valid) | (io.ws_allowin & ms_ready_go)
    when (io.ms_allowin) {
        ms_valid := io.to_ms.valid
    }
    io.to_ws.valid := ms_valid & ms_ready_go


    io.data.ar.id       := 1.U(4.W)
    io.data.ar.addr     := ms.alu_res
    io.data.ar.len      := 0.U(8.W)
    io.data.ar.size     := 4.U(3.W)
    io.data.ar.burst    := 1.U(2.W)
    io.data.ar.lock     := 0.U(2.W)
    io.data.ar.cache    := 0.U(4.W)
    io.data.ar.prot     := 0.U(3.W)
    io.data.arvalid     := (ms.wb_src === WB_MEM)

    val data_rdata = RegEnable(io.data.r.data, io.data.r.valid)

    val off = ms.addr(1, 0)
    val final_rdata = Wire(UInt(WORD.W))
    val tmp_rdata = Wire(UInt(WORD.W))

    tmp_rdata := MuxCase(data_rdata, Seq(
        (off === 0.U(2.W)) -> data_rdata(15, 0),
        (off === 1.U(2.W)) -> data_rdata(23, 8),
        (off === 2.U(2.W)) -> data_rdata(31, 16),
        (off === 3.U(2.W)) -> data_rdata(31, 24)
    ))

    final_rdata := MuxCase(data_rdata, Seq(
        (ms.mem_re === MEM_RB) -> Cat(Fill(24, tmp_rdata(7)), tmp_rdata(7, 0)),
        (ms.mem_re === MEM_RH) -> Cat(Fill(16, tmp_rdata(15)), tmp_rdata(15, 0)),
        (ms.mem_re === MEM_RBU) -> Cat(0.U(24.W), tmp_rdata(7, 0)),
        (ms.mem_re === MEM_RHU) -> Cat(0.U(24.W), tmp_rdata(15, 0))
    ))

    val wb_data = Wire(UInt(WORD.W))
    wb_data := MuxCase(0.U, Seq(
        (ms.wb_src === WB_ALU) -> ms.alu_res,
        (ms.wb_src === WB_CSR) -> ms.csr_data,
        (ms.wb_src === WB_BOTH) -> ms.csr_data,
        (ms.wb_src === WB_MEM) -> final_rdata,
        (ms.wb_src === WB_PC) -> (ms.pc + 4.U(WORD.W))
    ))

    io.to_ws.wb_src := ms.wb_src
    io.to_ws.rf_we := ms.rf_we
    io.to_ws.dest := ms.dest
    io.to_ws.wb_data := wb_data
    io.to_ws.pc := ms.pc

    io.rd_ms.valid := (ms.rf_we =/= 0.U(BYTE_LEN.W)) && ms_valid && (ms.dest =/= 0.U(REG.W))
    io.rd_ms.ready := true.B
    io.rd_ms.dest  := ms.dest
    io.rd_ms.data  := wb_data
}