package common

import chisel3._
import chisel3.util._

object const {
    val WORD = 32
    val REG  = 5
    val BYTE_LEN = 4

    val OP_LEN = 4
    val ALU_X = 15.U(OP_LEN.W)
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

    val BR_LEN = 3
    val BR_X    = 0.U(BR_LEN.W)
    val BR_S    = 1.U(BR_LEN.W)
    val BR_BNE  = 2.U(BR_LEN.W)
    val BR_BEQ  = 3.U(BR_LEN.W)
    val BR_BL   = 4.U(BR_LEN.W)

    val SRC1_LEN = 2
    val SRC1_X   = 0.U(SRC1_LEN.W)
    val SRC1_REG = 1.U(SRC1_LEN.W)
    val SRC1_PC  = 2.U(SRC1_LEN.W)

    val SRC2_LEN = 3
    val SRC2_X    = 0.U(SRC2_LEN.W)
    val SRC2_REG  = 1.U(SRC2_LEN.W)
    val SRC2_ui5  = 2.U(SRC2_LEN.W)
    val SRC2_si12 = 3.U(SRC2_LEN.W)
    val SRC2_si16 = 4.U(SRC2_LEN.W)
    val SRC2_si20 = 5.U(SRC2_LEN.W)
    val SRC2_si26 = 6.U(SRC2_LEN.W)

    val MEM_SEL_LEN = 4
    val MEM_WX = 0.U(MEM_SEL_LEN.W)
    val MEM_WS = 15.U(MEM_SEL_LEN.W)

    val MEM_RX = false.B
    val MEM_RS = true.B

    val RF_SEL_LEN = 4
    val RF_X = 0.U(RF_SEL_LEN.W)
    val RF_S = 15.U(RF_SEL_LEN.W)

    val WB_SEL_LEN = 2
    val WB_X    = 0.U(WB_SEL_LEN.W)
    val WB_ALU  = 1.U(WB_SEL_LEN.W)
    val WB_PC   = 2.U(WB_SEL_LEN.W)
    val WB_MEM  = 3.U(WB_SEL_LEN.W)

    val RS_LEN = 2
    val RS_X = 0.U(RS_LEN.W)
    val RS_J = 1.U(RS_LEN.W)
    val RS_K = 2.U(RS_LEN.W)
    val RS_D = 3.U(RS_LEN.W)
}