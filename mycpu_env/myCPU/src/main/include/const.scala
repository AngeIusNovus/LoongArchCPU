package common

import chisel3._
import chisel3.util._

object const {
    val WORD = 32
    val REG  = 5

    val OP_LEN = 4

    val ALU_ADD = 0.U(OP_LEN.W)
    val ALU_SUB = 1.U(OP_LEN.W)
    val ALU_LD = 0.U(OP_LEN.W)
    val ALU_ST = 0.U(OP_LEN.W)
    val ALU_JIRL = 0.U(OP_LEN.W)
    val ALU_B = 0.U(OP_LEN.W)
    val ALU_BL = 0.U(OP_LEN.W)
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
    val ALU_BEQ = 0.U(OP_LEN.W)
    val ALU_BNE = 0.U(OP_LEN.W)

    val MEM_SEL_LEN = 4
    val MEM_X = 0.U(MEM_SEL_LEN.W)
    val MEM_S = 15.U(MEM_SEL_LEN.W)

    val RF_SEL_LEN = 1
    val RF_X = 0.U(RF_SEL_LEN.W)
    val RF_S = 1.U(RF_SEL_LEN.W)

    val WB_SEL_LEN = 2
    val WB_X = 0.U(WB_SEL_LEN.W)
    val WB_ALU = 1.U(WB_SEL_LEN.W)
    val WB_PC = 2.U(WB_SEL_LEN.W)
    val WB_MEM = 3.U(WB_SEL_LEN.W)
}