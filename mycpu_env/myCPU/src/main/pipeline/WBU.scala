package mycpu

import chisel3._
import chisel3.util._
import common._
import common.const._

class WB_Stage extends Module {
    val io = IO(new Bundle{
        val to_ws = Flipped(new WB_BUS())
        val debug = new DEBUG()
        val ws_allowin = Output(Bool())
        val data_rdata = Input(UInt(WORD.W))
        val rf_we = Output(UInt(RF_SEL_LEN.W))
        val rf_waddr = Output(UInt(WORD.W))
        val rf_wdata = Output(UInt(WORD.W))
        val rd_ws = Output(UInt(REG.W))
    })

    val ws_valid = RegInit(false.B)
    val ws_ready_go = Wire(Bool())

    ws_ready_go := true.B
    io.ws_allowin := (!ws_valid) | ws_ready_go
    when (io.ws_allowin) {
        ws_valid := io.to_ws.valid
    }

    val dest        = RegInit(0.U(WORD.W))
    val rf_we       = RegInit(0.U(RF_SEL_LEN.W))
    val wb_src      = RegInit(0.U(WB_SEL_LEN.W))
    val rd_value    = RegInit(0.U(WORD.W))
    val alu_res     = RegInit(0.U(WORD.W))
    val pc          = RegInit(0.U(WORD.W))

    when (io.ws_allowin & io.to_ws.valid) {
        dest := io.to_ws.dest
        rf_we := io.to_ws.rf_we
        wb_src := io.to_ws.wb_src
        alu_res := io.to_ws.alu_res
        pc := io.to_ws.pc
    }
    val wb_data = MuxCase(0.U, Seq(
        (wb_src === WB_ALU) -> alu_res,
        (wb_src === WB_MEM) -> io.data_rdata, 
        (wb_src === WB_PC)  -> (pc + 4.U(WORD.W))
    ))
    val wb_addr = dest

    io.rf_we := rf_we
    io.rf_waddr := wb_addr
    io.rf_wdata := wb_data

    io.debug.wb_pc := pc
    io.debug.wb_rf_we := Mux(ws_valid, rf_we, 0.U(4.W))
    io.debug.wb_rf_wnum := wb_addr
    io.debug.wb_rf_wdata := wb_data

    io.rd_ws := Mux(ws_valid, dest, 0.U(REG.W))
}