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

    val ws = RegEnable(io.to_ws, io.ws_allowin && io.to_ws.valid)

    val wb_addr = ws.dest

    io.rf_we := ws.rf_we
    io.rf_waddr := wb_addr
    io.rf_wdata := ws.wb_data

    io.debug.wb_pc := ws.pc
    io.debug.wb_rf_we := Mux(ws_valid, ws.rf_we, 0.U(BYTE_LEN.W))
    io.debug.wb_rf_wnum := wb_addr
    io.debug.wb_rf_wdata := ws.wb_data

    io.rd_ws.valid := (ws.rf_we =/= 0.U(BYTE_LEN.W)) && ws_valid && (ws.dest =/= 0.U(REG.W))
    io.rd_ws.ready := true.B
    io.rd_ws.dest  := ws.dest
    io.rd_ws.data  := ws.wb_data
}