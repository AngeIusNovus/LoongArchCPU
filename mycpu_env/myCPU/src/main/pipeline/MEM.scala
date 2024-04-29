package mycpu

import chisel3._
import chisel3.util._
import common._
import common.const._

class MEM_Stage extends Module {
    val io = IO(new Bundle{
        val to_me = Flipped(new ME_BUS())
        val to_wb = new WB_BUS()
        val ms_allowin = Output(Bool())
        val ws_allowin = Input(Bool())
        val data = new RAM_IO()
    })

    val ms_valid = RegInit(false.B)
    val ms_ready_go = Wire(Bool())

    ms_ready_go := true.B
    io.ms_allowin := !ms_valid | (io.ws_allowin & ms_ready_go)
    when (io.ms_allowin) {
        ms_valid := io.to_me.valid
    }
    io.to_wb.valid := ms_valid & ms_ready_go

    val alu_res     = RegInit(0.U(WORD.W))
    val wb_sel      = RegInit(0.U(WB_SEL_LEN.W))
    val mem_we      = RegInit(false.B)
    val rf_we       = RegInit(false.B)
    val dest        = RegInit(0.U(REG.W))
    val rd_value    = RegInit(0.U(WORD.W))
    val pc          = RegInit(0.U(WORD.W))

    when (io.ms_allowin & io.to_me.valid) {
        alu_res := io.to_me.alu_res
        wb_sel := io.to_me.wb_sel
        rf_we := io.to_me.rf_we
        mem_we := io.to_me.mem_we
        dest := io.to_me.dest
        rd_value := io.to_me.rd_value     
        pc := io.to_me.pc 
    }

    io.data.en := true.B
    io.data.we := mem_we
    io.data.addr := alu_res
    io.data.wdata := rd_value

    io.to_wb.wb_sel := wb_sel
    io.to_wb.rf_we := rf_we
    io.to_wb.dest := dest
    io.to_wb.alu_res := alu_res
    io.to_wb.pc := pc
}