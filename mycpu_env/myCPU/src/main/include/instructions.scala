package common

import chisel3.util._

object Instructions {
    val add_w       = BitPat("b00000000000100000???????????????")
    val sub_w       = BitPat("b00000000000100010???????????????")
    val slt         = BitPat("b00000000000100100???????????????")
    val slti        = BitPat("b0000001000??????????????????????")
    val sltu        = BitPat("b00000000000100101???????????????")
    val sltui       = BitPat("b0000001001??????????????????????")
    val nor         = BitPat("b00000000000101000???????????????")
    val and         = BitPat("b00000000000101001???????????????")
    val or          = BitPat("b00000000000101010???????????????")
    val xor         = BitPat("b00000000000101011???????????????")
    val andi        = BitPat("b0000001101??????????????????????")
    val ori         = BitPat("b0000001110??????????????????????")
    val xori        = BitPat("b0000001111??????????????????????")
    val slli_w      = BitPat("b00000000010000001???????????????")
    val srli_w      = BitPat("b00000000010001001???????????????")
    val srai_w      = BitPat("b00000000010010001???????????????")
    val sll_w       = BitPat("b00000000000101110???????????????")
    val srl_w       = BitPat("b00000000000101111???????????????")
    val sra_w       = BitPat("b00000000000110000???????????????")
    val addi_w      = BitPat("b0000001010??????????????????????")
    val ld_b        = BitPat("b0010100000??????????????????????")
    val ld_h        = BitPat("b0010100001??????????????????????")
    val ld_w        = BitPat("b0010100010??????????????????????")
    val st_b        = BitPat("b0010100100??????????????????????")
    val st_h        = BitPat("b0010100101??????????????????????")
    val st_w        = BitPat("b0010100110??????????????????????")
    val ld_bu       = BitPat("b0010101000??????????????????????")
    val ld_hu       = BitPat("b0010101001??????????????????????")
    val jirl        = BitPat("b010011??????????????????????????")
    val inst_b      = BitPat("b010100??????????????????????????")
    val inst_bl     = BitPat("b010101??????????????????????????")
    val beq         = BitPat("b010110??????????????????????????")
    val bne         = BitPat("b010111??????????????????????????")
    val blt         = BitPat("b011000??????????????????????????")
    val bge         = BitPat("b011001??????????????????????????")
    val bltu        = BitPat("b011010??????????????????????????")
    val bgeu        = BitPat("b011011??????????????????????????")
    val lu12i_w     = BitPat("b0001010?????????????????????????")
    val pcaddu12i   = BitPat("b0001110?????????????????????????")
    val mul_w       = BitPat("b00000000000111000???????????????")
    val mulh_w      = BitPat("b00000000000111001???????????????")
    val mulh_wu     = BitPat("b00000000000111010???????????????")
    val div_w       = BitPat("b00000000001000000???????????????")
    val mod_w       = BitPat("b00000000001000001???????????????")
    val div_wu      = BitPat("b00000000001000010???????????????")
    val mod_wu      = BitPat("b00000000001000011???????????????")
    val csrrd       = BitPat("b00000100??????????????00000?????")
    val csrwr       = BitPat("b00000100??????????????00001?????")
    val csrxchg     = BitPat("b00000100????????????????????????")
    val syscall     = BitPat("b00000000001010110???????????????")
    val ertn        = BitPat("b00000110010010000011100000000000")
    val break       = BitPat("b00000000001010100???????????????")
    val rdcntid     = BitPat("b0000000000000000011000?????00000")
    val rdcntvl    = BitPat("b000000000000000001100000000?????")
    val rdcntvh    = BitPat("b000000000000000001100100000?????")
}