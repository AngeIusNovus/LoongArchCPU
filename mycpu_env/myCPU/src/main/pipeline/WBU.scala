package mycpu

import chisel3._
import chisel3.util._
import common._
import common.const._

class WB_Stage extends Module {
    val io = IO(new Bundle{
        val to_wb = Flipped(new WB_BUS())
        val debug = new DEBUG()
        val ws_allowin = Output(Bool())
        val data_rdata = Input(UInt(WORD.W))
        val rf_we = Output(Bool())
        val rf_waddr = Output(UInt(WORD.W))
        val rf_wdata = Output(UInt(WORD.W))
    })

    val ws_valid = RegInit(false.B)
    val ws_ready_go = Wire(Bool())

    ws_ready_go := true.B
    io.ws_allowin := !ws_valid | ws_ready_go
    when (io.ws_allowin) {
        ws_valid := io.to_wb.valid
    }

    val dest = RegInit(0.U(WORD.W))
    val rf_we = RegInit(false.B)
    val wb_sel = RegInit(0.U(WB_SEL_LEN.W))
    val rd_value = RegInit(0.U(WORD.W))
    val alu_res = RegInit(0.U(WORD.W))
    val pc = RegInit(0.U(WORD.W))

    when (io.ws_allowin & io.to_wb.valid) {
        dest := io.to_wb.dest
        rf_we := io.to_wb.rf_we
        wb_sel := io.to_wb.wb_sel
        alu_res := io.to_wb.alu_res
        pc := io.to_wb.pc
    }
    val wb_data = MuxCase(alu_res, Seq(
        (wb_sel === WB_MEM) -> io.data_rdata, 
        (wb_sel === WB_PC)  -> (pc + 4.U(WORD.W))
    ))
    val wb_addr = dest

    io.rf_we := rf_we
    io.rf_waddr := wb_addr
    io.rf_wdata := wb_data

    io.debug.wb_pc := pc
    io.debug.wb_rf_we := Fill(4, rf_we)
    io.debug.wb_rf_wnum := wb_addr
    io.debug.wb_rf_wdata := wb_data
}