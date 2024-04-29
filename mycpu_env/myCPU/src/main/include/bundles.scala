package common

import chisel3._
import chisel3.util._
import common._
import common.const._

class RAM_IO extends Bundle {
    val en    = Output(Bool())
    val we    = Output(UInt(4.W))
    val addr  = Output(UInt(WORD.W))
    val wdata = Output(UInt(WORD.W))
    val rdata = Input(UInt(WORD.W))
}

class DEBUG extends Bundle {
    val wb_pc       = Output(UInt(WORD.W))
    val wb_rf_we    = Output(UInt(4.W))
    val wb_rf_wnum  = Output(UInt(REG.W))
    val wb_rf_wdata = Output(UInt(WORD.W))
}

class BR_OUT extends Bundle {
    val taken  = Output(Bool())
    val target = Output(UInt(WORD.W))
}

class DS_BUS extends Bundle {
    val valid = Output(Bool())
    val pc    = Output(UInt(WORD.W))
    val inst  = Output(UInt(WORD.W))
}

class EX_BUS extends Bundle {
    val valid       = Output(Bool())
    val alu_op      = Output(UInt(12.W))
    val src1_data   = Output(UInt(WORD.W))
    val src2_data   = Output(UInt(WORD.W))
    val wb_sel      = Output(UInt(WB_SEL_LEN.W))
    val rf_we       = Output(Bool())
    val mem_we      = Output(Bool())
    val dest        = Output(UInt(REG.W))
    val rd_value    = Output(UInt(WORD.W))
    val pc          = Output(UInt(WORD.W))
}

class ME_BUS extends Bundle {
    val valid = Output(Bool())
    val wb_sel = Output(UInt(WB_SEL_LEN.W))
    val rf_we = Output(Bool())
    val mem_we = Output(Bool())
    val dest = Output(UInt(REG.W))
    val rd_value = Output(UInt(WORD.W))
    val alu_res = Output(UInt(WORD.W))
    val pc = Output(UInt(WORD.W))
}

class WB_BUS extends Bundle {
    val valid = Output(Bool())
    val wb_sel = Output(UInt(WB_SEL_LEN.W))
    val rf_we = Output(Bool())
    val dest = Output(UInt(REG.W))
    val alu_res = Output(UInt(WORD.W))
    val pc = Output(UInt(WORD.W))
}
