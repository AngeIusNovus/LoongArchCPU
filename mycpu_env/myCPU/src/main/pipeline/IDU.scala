package mycpu

import chisel3._
import chisel3.util._
import common._
import common.const._
import common.Instructions._

class ID_Stage extends Module {
    val io = IO(new Bundle {
        val to_ds = Flipped(new DS_BUS())
        val to_es = new EX_BUS()
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
    io.ds_allowin := (!ds_valid) | (io.es_allowin & ds_ready_go)
    when (io.ds_allowin) {
        ds_valid := io.to_ds.valid
    }
    io.to_es.valid := ds_valid & ds_ready_go

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
    si26 := Cat(Fill(4, inst(9)), inst(9, 0), inst(25, 10), 0.U(2.W))

    val decode = ListLookup(inst, 
        List(ALU_X, BR_X, 0.U(WORD.W), 0.U(WORD.W), MEM_X, RF_X, WB_X),
        Array (
            add_w -> List(ALU_ADD, BR_X, rj_value, rk_value, MEM_X, RF_S, WB_ALU),
            sub_w -> List(ALU_SUB, BR_X, rj_value, rk_value, MEM_X, RF_S, WB_ALU),
            slt   -> List(ALU_SLT, BR_X, rj_value, rk_value, MEM_X, RF_S, WB_ALU),
            sltu  -> List(ALU_SLTU, BR_X, rj_value, rk_value, MEM_X, RF_S, WB_ALU),
            nor   -> List(ALU_NOR, BR_X, rj_value, rk_value, MEM_X, RF_S, WB_ALU), 
            or    -> List(ALU_OR, BR_X, rj_value, rk_value, MEM_X, RF_S, WB_ALU), 
            and   -> List(ALU_AND, BR_X, rj_value, rk_value, MEM_X, RF_S, WB_ALU), 
            xor   -> List(ALU_XOR, BR_X, rj_value, rk_value, MEM_X, RF_S, WB_ALU), 
            slli_w -> List(ALU_SLL, BR_X, rj_value, ui5, MEM_X, RF_S, WB_ALU), 
            srli_w -> List(ALU_SRL, BR_X, rj_value, ui5, MEM_X, RF_S, WB_ALU), 
            srai_w -> List(ALU_SRA, BR_X, rj_value, ui5, MEM_X, RF_S, WB_ALU), 
            addi_w -> List(ALU_ADD, BR_X, rj_value, si12, MEM_X, RF_S, WB_ALU),
            ld_w   -> List(ALU_LD, BR_X, rj_value, si12, MEM_X, RF_S, WB_MEM),
            st_w   -> List(ALU_ST, BR_X, rj_value, si12, MEM_S, RF_X, WB_X),
            jirl   -> List(ALU_X, BR_S, rj_value, si16, MEM_X, RF_S, WB_PC),
            inst_b -> List(ALU_X, BR_S, pc, si26, MEM_X, RF_X, WB_X),
            inst_bl -> List(ALU_X, BR_BL, pc, si26, MEM_X, RF_S, WB_PC),
            beq     -> List(ALU_X, BR_BEQ, pc, si16, MEM_X, RF_X, WB_X),
            bne     -> List(ALU_X, BR_BNE, pc, si16, MEM_X, RF_X, WB_X),
            lu12i_w -> List(ALU_LU12I, BR_X, 0.U, si20, MEM_X, RF_S, WB_ALU)
        ))
    val alu_op :: br_op :: src1_data :: src2_data :: mem_we :: rf_we :: wb_src :: Nil = decode

    io.br.taken := MuxCase(false.B, Seq(
        ((br_op === BR_S) || (br_op === BR_BL)) -> true.B,  
        (br_op === BR_BNE) -> (rj_value =/= rd_value), 
        (br_op === BR_BEQ) -> (rj_value === rd_value)
    ))
    io.br.target := src1_data + src2_data

    io.to_es.alu_op := alu_op
    io.to_es.src1_data := src1_data
    io.to_es.src2_data := src2_data
    io.to_es.wb_src := wb_src
    io.to_es.mem_we := mem_we
    io.to_es.rf_we := rf_we.asBool
    io.to_es.rd_value := rd_value
    io.to_es.dest := Mux(br_op === BR_BL, 1.U(REG.W), io.rd)
    io.to_es.pc := pc
}