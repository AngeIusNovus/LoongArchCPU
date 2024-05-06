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
        val rf_we = Output(UInt(RF_SEL_LEN.W))
        val rf_waddr = Output(UInt(WORD.W))
        val rf_wdata = Output(UInt(WORD.W))
        val rd_ws = new TMP_REG()
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
    val wb_data     = RegInit(0.U(WORD.W))
    val pc          = RegInit(0.U(WORD.W))

    when (io.ws_allowin & io.to_ws.valid) {
        dest := io.to_ws.dest
        rf_we := io.to_ws.rf_we
        wb_src := io.to_ws.wb_src
        wb_data := io.to_ws.wb_data
        pc := io.to_ws.pc
    }
    val wb_addr = dest

    io.rf_we := rf_we
    io.rf_waddr := wb_addr
    io.rf_wdata := wb_data

    io.debug.wb_pc := pc
    io.debug.wb_rf_we := Mux(ws_valid, rf_we, 0.U(BYTE_LEN.W))
    io.debug.wb_rf_wnum := wb_addr
    io.debug.wb_rf_wdata := wb_data

    io.rd_ws.valid := (rf_we =/= 0.U(BYTE_LEN.W)) && ws_valid && (dest =/= 0.U(REG.W))
    io.rd_ws.ready := true.B
    io.rd_ws.dest  := dest
    io.rd_ws.data  := wb_data
}