package common

import chisel3._
import chisel3.util._
import common._
import common.const._

class RAM_IO extends Bundle {
    val en    = Output(Bool())
    val we    = Output(UInt(BYTE_LEN.W))
    val addr  = Output(UInt(WORD.W))
    val wdata = Output(UInt(WORD.W))
    val rdata = Input(UInt(WORD.W))
}

class DEBUG extends Bundle {
    val wb_pc       = Output(UInt(WORD.W))
    val wb_rf_we    = Output(UInt(BYTE_LEN.W))
    val wb_rf_wnum  = Output(UInt(REG.W))
    val wb_rf_wdata = Output(UInt(WORD.W))
}

class CSR_IO extends Bundle {
    val Excp        = Output(UInt(EXCP_LEN.W)) // 1:excp, 2:etrn
    val Ecode       = Output(UInt(ECODE_LEN.W))
    val Esubcode    = Output(UInt(ESUBCODE_LEN.W))
    val pc          = Output(UInt(WORD.W))
    val en_mask     = Output(Bool())
    val we          = Output(Bool())
    val waddr       = Output(UInt(CSR_ADDR.W))
    val wdata       = Output(UInt(WORD.W))
    val mask        = Output(UInt(WORD.W))
    val raddr       = Output(UInt(CSR_ADDR.W))
    val rdata       = Input(UInt(WORD.W))
}

class CSR_OUT extends Bundle {
    val Excp        = Output(UInt(EXCP_LEN.W))
    val Ecode       = Output(UInt(ECODE_LEN.W))
    val Esubcode    = Output(UInt(ESUBCODE_LEN.W))
    val pc          = Output(UInt(WORD.W))
    val en_mask     = Output(Bool())
    val we          = Output(Bool())
    val waddr       = Output(UInt(CSR_ADDR.W))
    val wdata       = Output(UInt(WORD.W))
    val mask        = Output(UInt(WORD.W))
    val raddr       = Output(UInt(CSR_ADDR.W))
}

class BR_OUT extends Bundle {
    val taken  = Output(Bool())
    val target = Output(UInt(WORD.W))
}

class TMP_REG extends Bundle {
    val valid = Output(Bool())
    val ready = Output(Bool())
    val dest = Output(UInt(REG.W))
    val data = Output(UInt(WORD.W))
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
    val wb_src      = Output(UInt(WB_SEL_LEN.W))
    val rf_we       = Output(UInt(RF_SEL_LEN.W))
    val mem_re      = Output(UInt(MEM_RED_LEN.W))
    val mem_we      = Output(UInt(BYTE_LEN.W))
    val dest        = Output(UInt(REG.W))
    val rd_value    = Output(UInt(WORD.W))
    val pc          = Output(UInt(WORD.W))
    val csr         = new CSR_OUT()
}

class ME_BUS extends Bundle {
    val valid       = Output(Bool())
    val wb_src      = Output(UInt(WB_SEL_LEN.W))
    val rf_we       = Output(UInt(RF_SEL_LEN.W))
    val mem_re      = Output(UInt(MEM_RED_LEN.W))
    val mem_we      = Output(UInt(BYTE_LEN.W))
    val dest        = Output(UInt(REG.W))
    val rd_value    = Output(UInt(WORD.W))
    val alu_res     = Output(UInt(WORD.W))
    val addr        = Output(UInt(WORD.W))
    val pc          = Output(UInt(WORD.W))
    val csr_data    = Output(UInt(WORD.W))
}

class WB_BUS extends Bundle {
    val valid   = Output(Bool())
    val wb_src  = Output(UInt(WB_SEL_LEN.W))
    val rf_we   = Output(UInt(RF_SEL_LEN.W))
    val dest    = Output(UInt(REG.W))
    val wb_data = Output(UInt(WORD.W))
    val pc      = Output(UInt(WORD.W))
}
