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
    val badv        = Output(Bool())
    val badvaddr    = Output(UInt(WORD.W))
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
    val badv        = Output(Bool())
    val badvaddr     = Output(UInt(WORD.W))
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
    val ADEF  = Output(Bool())
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

class AXI_AR_IO extends Bundle {
    val id = Output(UInt(AXI_ID.W))
    val addr = Output(UInt(WORD.W))
    val len = Output(UInt(AXI_LEN.W))
    val size = Output(UInt(AXI_SIZE.W))
    val burst = Output(UInt(AXI_BURST.W))
    val lock = Output(UInt(AXI_LOCK.W))
    val cache = Output(UInt(AXI_CACHE.W))
    val prot = Output(UInt(AXI_PROT.W))
}

class AXI_R_IO extends Bundle {
    val id = Output(UInt(AXI_ID.W))
    val data = Output(UInt(WORD.W))
    val valid = Output(Bool())
}

class CPU_AXI_RD_IO extends Bundle {
    val ar = new AXI_AR_IO()
    val r  = Flipped(new AXI_R_IO())
    val arvalid = Output(Bool())
}

class AXI_RD_IO extends CPU_AXI_RD_IO {
    val arready = Input(Bool())
    val rready = Output(Bool())
}

class AXI_AW_IO extends Bundle {
    val id = Output(UInt(AXI_ID.W))
    val addr = Output(UInt(WORD.W))
    val len = Output(UInt(AXI_LEN.W))
    val size = Output(UInt(AXI_SIZE.W))
    val burst = Output(UInt(AXI_BURST.W))
    val lock = Output(UInt(AXI_LOCK.W))
    val cache = Output(UInt(AXI_CACHE.W))
    val prot = Output(UInt(AXI_PROT.W)) 
}

class AXI_W_IO extends Bundle {
    val id = Output(UInt(AXI_ID.W))
    val data = Output(UInt(WORD.W))
    val strb = Output(UInt(AXI_STRB.W))
    val last = Output(Bool())
}

class AXI_B_IO extends Bundle {
    val valid = Output(Bool())
    val ready = Input(Bool())
}

class AXI_WR_IO extends Bundle {
    val aw = new AXI_AW_IO()
    val awvalid = Output(Bool())
    val awready = Input(Bool())
    val w = new AXI_W_IO()
    val wvalid  = Output(Bool())
    val wready  = Input(Bool())
    val b = Flipped(new AXI_B_IO())
}

class AXI_IO extends Bundle {
    val rd = new AXI_RD_IO()
    val wr = new AXI_WR_IO()
}
