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
        val data = new RAM_IO()
        val rd_ms = Output(UInt(REG.W))
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
    val mem_en      = RegInit(false.B)
    val mem_we      = RegInit(0.U(MEM_SEL_LEN.W))
    val rf_we       = RegInit(0.U(RF_SEL_LEN.W))
    val wb_src      = RegInit(0.U(WB_SEL_LEN.W))
    val dest        = RegInit(0.U(REG.W))
    val rd_value    = RegInit(0.U(WORD.W))
    val pc          = RegInit(0.U(WORD.W))

    when (io.ms_allowin & io.to_ms.valid) {
        alu_res := io.to_ms.alu_res
        wb_src := io.to_ms.wb_src
        rf_we := io.to_ms.rf_we
        mem_en := io.to_ms.mem_en
        mem_we := io.to_ms.mem_we
        dest := io.to_ms.dest
        rd_value := io.to_ms.rd_value
        pc := io.to_ms.pc 
    }

    io.data.en := mem_en
    io.data.we := mem_we
    io.data.addr := alu_res
    io.data.wdata := rd_value

    io.to_ws.wb_src := wb_src
    io.to_ws.rf_we := rf_we
    io.to_ws.dest := dest
    io.to_ws.alu_res := alu_res
    io.to_ws.pc := pc

    io.rd_ms := Mux(ms_valid, dest, 0.U(REG.W))
}