package mycpu

import chisel3._
import chisel3.util._
import common._
import common.const._
import common.Instructions._

class ID_Stage extends Module {
    val io = IO(new Bundle {
        val to_ds = Flipped(new DS_BUS())
        val to_ex = new EX_BUS()
        val ds_allowin = Output(Bool())
        val es_allowin = Input(Bool())
        val br = new BR_OUT()
        val rj = Output(UInt(WORD.W))
        val rk = Output(UInt(WORD.W))
        val rd = Output(UInt(WORD.W))
        val reg_rdata1 = Input(UInt(WORD.W))
        val reg_rdata2 = Input(UInt(WORD.W))
        val reg_rdata3 = Input(UInt(WORD.W))
    })

    val ds_valid = RegInit(false.B)
    val ds_ready_go = Wire(Bool())

    ds_ready_go := true.B
    io.ds_allowin := !ds_valid | (io.es_allowin & ds_ready_go)
    when (io.ds_allowin) {
        ds_valid := io.to_ds.valid
    }
    io.to_ex.valid := ds_valid & ds_ready_go

    val inst = RegInit(0.U(WORD.W))
    val pc   = RegInit(0.U(WORD.W))
    when (io.ds_allowin && io.to_ds.valid) {
        inst := io.to_ds.inst
        pc   := io.to_ds.pc
    }

    io.rd := inst(4, 0)
    io.rj := inst(9, 5)
    io.rk := inst(14, 10)
    val rj_value = Wire(UInt(WORD.W))
    val rk_value = Wire(UInt(WORD.W))
    val rd_value = Wire(UInt(WORD.W))
    rj_value := io.reg_rdata1
    rk_value := io.reg_rdata2
    rd_value := io.reg_rdata3

    val ui5  = Wire(UInt(WORD.W))
    val i12  = Wire(UInt(12.W))
    val i16  = Wire(UInt(16.W))
    val i20  = Wire(UInt(20.W))
    val si12 = Wire(UInt(WORD.W))
    val si16 = Wire(UInt(WORD.W))
    val si20 = Wire(UInt(WORD.W))
    val si26 = Wire(UInt(WORD.W))
    ui5 := inst(14, 10)
    i12 := inst(21, 10)
    i16 := inst(25, 10)
    i20 := inst(24, 5)
    si12 := Cat(Fill(20, i12(11)), i12)
    si16 := Cat(Fill(14, i16(15)), i16, 0.U(2.W))
    si20 := Cat(i20, 0.U(12.W))
    si26 := Cat(inst(9, 0), inst(25, 10))

    val decode = ListLookup(inst, 
        List(ALU_ADD, 0.U(WORD.W), 0.U(WORD.W), MEM_X, RF_X, WB_X),
        Array (
            add_w -> List(ALU_ADD, rj_value, rk_value, MEM_X, RF_S, WB_ALU),
            sub_w -> List(ALU_SUB, rj_value, rk_value, MEM_X, RF_S, WB_ALU),
            slt   -> List(ALU_SLT, rj_value, rk_value, MEM_X, RF_S, WB_ALU),
            sltu  -> List(ALU_SLTU, rj_value, rk_value, MEM_X, RF_S, WB_ALU),
            nor   -> List(ALU_NOR, rj_value, rk_value, MEM_X, RF_S, WB_ALU), 
            or    -> List(ALU_OR, rj_value, rk_value, MEM_X, RF_S, WB_ALU), 
            and   -> List(ALU_AND, rj_value, rk_value, MEM_X, RF_S, WB_ALU), 
            xor   -> List(ALU_XOR, rj_value, rk_value, MEM_X, RF_S, WB_ALU), 
            slli_w -> List(ALU_SLL, rj_value, ui5, MEM_X, RF_S, WB_ALU), 
            srli_w -> List(ALU_SLL, rj_value, ui5, MEM_X, RF_S, WB_ALU), 
            srai_w -> List(ALU_SRA, rj_value, ui5, MEM_X, RF_S, WB_ALU), 
            addi_w -> List(ALU_ADD, rj_value, si12, MEM_X, RF_S, WB_ALU),
            ld_w   -> List(ALU_LD, rj_value, si12, MEM_X, RF_S, WB_ALU),
            st_w   -> List(ALU_ST, rj_value, si12, MEM_S, RF_X, WB_X),
            jirl   -> List(ALU_JIRL, pc, si16, MEM_X, RF_S, WB_PC),
            inst_b -> List(ALU_B, pc, si26, MEM_X, RF_X, WB_X),
            inst_bl -> List(ALU_BL, pc, si26, MEM_X, RF_S, WB_PC),
            beq     -> List(ALU_BEQ, pc, si16, MEM_X, RF_X, WB_X),
            bne     -> List(ALU_BNE, pc, si16, MEM_X, RF_X, WB_X),
            lu12i_w -> List(ALU_LU12I, 0.U, si20, MEM_X, RF_S, WB_ALU)
        ))
    val alu_op :: src1_data :: src2_data :: mem_we :: rf_we :: wb_sel :: Nil = decode

    io.br.taken := MuxCase(false.B, Seq(
        ((alu_op === ALU_B) || (alu_op === ALU_BL) || (alu_op === ALU_JIRL)) -> true.B, 
        (alu_op === ALU_BNE) -> (rj_value =/= rk_value), 
        (alu_op === ALU_BEQ) -> (rj_value === rk_value)
    ))
    io.br.target := src1_data + src2_data

    io.to_ex.alu_op := alu_op
    io.to_ex.src1_data := src1_data
    io.to_ex.src2_data := src2_data
    io.to_ex.wb_sel := wb_sel
    io.to_ex.mem_we := mem_we
    io.to_ex.rf_we := rf_we.asBool
    io.to_ex.rd_value := rd_value
    io.to_ex.dest := Mux(alu_op === ALU_BL, 1.U(REG.W), io.rd)
    io.to_ex.pc := pc
}