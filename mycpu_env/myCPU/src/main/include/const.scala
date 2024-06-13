package common

import chisel3._
import chisel3.util._

object const {
    val WORD = 32
    val LONG = 64
    val REG  = 5
    val CSR_ADDR = 14
    val BYTE_LEN = 4
    val COUNT_N = 25

    val OP_LEN = 5
    val ALU_X = 31.U(OP_LEN.W)
    val ALU_ADD = 0.U(OP_LEN.W)
    val ALU_LD = 0.U(OP_LEN.W)
    val ALU_ST = 0.U(OP_LEN.W)
    val ALU_SUB = 1.U(OP_LEN.W)
    val ALU_SLT = 2.U(OP_LEN.W)
    val ALU_SLTU = 3.U(OP_LEN.W)
    val ALU_AND = 4.U(OP_LEN.W)
    val ALU_NOR = 5.U(OP_LEN.W)
    val ALU_OR = 6.U(OP_LEN.W)
    val ALU_XOR = 7.U(OP_LEN.W)
    val ALU_SLL = 8.U(OP_LEN.W)
    val ALU_SRL = 9.U(OP_LEN.W)
    val ALU_SRA = 10.U(OP_LEN.W)
    val ALU_LU12I = 11.U(OP_LEN.W)
    val ALU_MUL = 12.U(OP_LEN.W)
    val ALU_MULU = 13.U(OP_LEN.W)
    val ALU_MULH = 14.U(OP_LEN.W)
    val ALU_MULHU = 15.U(OP_LEN.W)
    val ALU_DIV = 16.U(OP_LEN.W)
    val ALU_DIVU = 17.U(OP_LEN.W)
    val ALU_MOD = 18.U(OP_LEN.W)
    val ALU_MODU = 19.U(OP_LEN.W)

    val BR_LEN = 4
    val BR_X    = 0.U(BR_LEN.W)
    val BR_S    = 1.U(BR_LEN.W)
    val BR_BNE  = 2.U(BR_LEN.W)
    val BR_BEQ  = 3.U(BR_LEN.W)
    val BR_BL   = 4.U(BR_LEN.W)
    val BR_BLT  = 5.U(BR_LEN.W)
    val BR_BLTU = 6.U(BR_LEN.W)
    val BR_BGE  = 7.U(BR_LEN.W)
    val BR_BGEU = 8.U(BR_LEN.W)

    val CSR_LEN = 3
    val CSR_X       = 0.U(CSR_LEN.W)
    val CSR_RD      = 1.U(CSR_LEN.W)
    val CSR_WR      = 2.U(CSR_LEN.W)
    val CSR_XCHG    = 3.U(CSR_LEN.W)
    val CSR_ERTN    = 4.U(CSR_LEN.W)
    val CSR_SYSCALL = 5.U(CSR_LEN.W)
    val CSR_BREAK   = 6.U(CSR_LEN.W)
    val CSR_INE     = 7.U(CSR_LEN.W)

    val SRC1_LEN = 2
    val SRC1_X   = 0.U(SRC1_LEN.W)
    val SRC1_REG = 1.U(SRC1_LEN.W)
    val SRC1_PC  = 2.U(SRC1_LEN.W)

    val SRC2_LEN = 4
    val SRC2_X    = 0.U(SRC2_LEN.W)
    val SRC2_REG  = 1.U(SRC2_LEN.W)
    val SRC2_ui5  = 2.U(SRC2_LEN.W)
    val SRC2_si12 = 3.U(SRC2_LEN.W)
    val SRC2_si16 = 4.U(SRC2_LEN.W)
    val SRC2_si20 = 5.U(SRC2_LEN.W)
    val SRC2_si26 = 6.U(SRC2_LEN.W)
    val SRC2_ui12 = 7.U(SRC2_LEN.W)
    val SRC2_CNTL = 8.U(SRC2_LEN.W)
    val SRC2_CNTH = 9.U(SRC2_LEN.W)
    val SRC2_CNTID = 10.U(SRC2_LEN.W)

    val MEM_WX = 0.U(BYTE_LEN.W)
    val MEM_WS = 15.U(BYTE_LEN.W)
    val MEM_WB = 1.U(BYTE_LEN.W)
    val MEM_WH = 3.U(BYTE_LEN.W)

    val MEM_RED_LEN = 3
    val MEM_RX  = 0.U(BYTE_LEN.W)
    val MEM_RS  = 1.U(BYTE_LEN.W)
    val MEM_RB  = 2.U(BYTE_LEN.W)
    val MEM_RH  = 3.U(BYTE_LEN.W)
    val MEM_RBU = 4.U(BYTE_LEN.W)
    val MEM_RHU = 5.U(BYTE_LEN.W)

    val RF_SEL_LEN = 4
    val RF_X = 0.U(RF_SEL_LEN.W)
    val RF_S = 15.U(RF_SEL_LEN.W)

    val WB_SEL_LEN = 3
    val WB_X    = 0.U(WB_SEL_LEN.W)
    val WB_ALU  = 1.U(WB_SEL_LEN.W)
    val WB_PC   = 2.U(WB_SEL_LEN.W)
    val WB_MEM  = 3.U(WB_SEL_LEN.W)
    val WB_CSR  = 4.U(WB_SEL_LEN.W)
    val WB_BOTH = 5.U(WB_SEL_LEN.W)

    val RS_LEN = 2
    val RS_X = 0.U(RS_LEN.W)
    val RS_J = 1.U(RS_LEN.W)
    val RS_K = 2.U(RS_LEN.W)
    val RS_D = 3.U(RS_LEN.W)

    val EXCP_LEN = 2
    val ECODE_LEN = 6
    val ESUBCODE_LEN = 9

    val CSR_WIDTH = 4
    val CSR_BADADDR = 15.U(CSR_WIDTH.W)
    val CRMD_X = "h0".U
    val PRMD_X = "h1".U
    val EUEN_X = "h2".U
    val ECFG_X = "h4".U
    val ESTAT_X = "h5".U
    val ERA_X = "h6".U
    val BADV_X = "h7".U
    val EENTRY_X = "hc".U
    val SAVE0_X = "h30".U
    val SAVE1_X = "h31".U
    val SAVE2_X = "h32".U
    val SAVE3_X = "h33".U
    val TID_X = "h40".U
    val TCFG_X = "h41".U 
    val TVAL_X = "h42".U 
    val TICLR_X = "h44".U

    val AXI_ID = 4
    val AXI_LEN = 8
    val AXI_SIZE = 3
    val AXI_BURST = 2
    val AXI_LOCK = 2
    val AXI_CACHE = 4
    val AXI_PROT = 3
    val AXI_STRB = 8
}

object Ecode {
    val INT     = 0x00.U(6.W) 
    val PIL     = 0x01.U(6.W)
    val PIS     = 0x02.U(6.W)
    val PIF     = 0x03.U(6.W)
    val PME     = 0x04.U(6.W)
    val PPI     = 0x07.U(6.W)
    val ADEF    = 0x08.U(6.W)
    val ADEM    = 0x08.U(6.W)
    val ALE     = 0x09.U(6.W)
    val SYS     = 0x0b.U(6.W)
    val BRK     = 0x0c.U(6.W)
    val INE     = 0x0d.U(6.W)
    val IPE     = 0x0e.U(6.W)
    val FPD     = 0x0f.U(6.W)
    val FPE     = 0x12.U(6.W)
    val TLBR    = 0x3F.U(6.W)
    val NONE    = 0x1f.U(6.W)
}