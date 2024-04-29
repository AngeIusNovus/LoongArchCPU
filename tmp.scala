package common

import chisel3._
import chisel3.util._
import common.Consts._

class REG_IO extends Bundle {
    val r1 = Input(UInt(LENx.W))
    val r2 = Input(UInt(LENx.W))
    val r3 = Input(UInt(LENx.W))
    val rdata1 = Output(UInt(LENX.W))
    val rdata2 = Output(UInt(LENX.W))
    val rdata3 = Output(UInt(LENX.W))
    val wen = Input(UInt(1.W))
    val wr  = Input(UInt(LENx.W))
    val wdata = Input(UInt(LENX.W))
}
class RAM_IO extends Bundle {
    val en    = Output(UInt(1.W))
    val wen   = Output(UInt(4.W))
    val addr  = Output(UInt(LENX.W))
    val wdata = Output(UInt(LENX.W))
    val rdata = Input(UInt(LENX.W))
}
class DEBUG extends Bundle {
    val wb_pc     = Output(UInt(LENX.W))
    val wb_rf_wen = Output(UInt(4.W))
    val wb_rf_wnum = Output(UInt(5.W))
    val wb_rf_wdata = Output(UInt(LENX.W))
}
class ID_INFO extends Bundle {
    val valid = Output(UInt(1.W))
    val inst  = Output(UInt(LENX.W))
    val pc    = Output(UInt(LENX.W))
}
class BR_INFO extends Bundle {
    val flg   = Output(Bool())
    val target= Output(UInt(LENX.W))
}
class EX_INFO extends Bundle {
    val valid = Output(UInt(1.W))
    val exe_fun = Output(UInt(EXE_FUN_LEN.W))
    val op1_data = Output(UInt(LENX.W))
    val op2_data = Output(UInt(LENX.W))
    val mem_wen = Output(UInt(MEN_LEN.W))
    val rf_wen  = Output(UInt(REN_LEN.W))
    val wb_sel = Output(UInt(WB_SEL_LEN.W))
    val pc = Output(UInt(LENX.W))
    val rs3_rd = Output(UInt(LENX.W))
    val dest  = Output(UInt(LENx.W))
}
class MEM_INFO extends Bundle {
    val valid = Output(UInt(1.W))
    val exe_fun = Output(UInt(EXE_FUN_LEN.W))
    val rf_wen  = Output(UInt(REN_LEN.W))
    val wb_sel = Output(UInt(WB_SEL_LEN.W))
    val mem_wen = Output(UInt(MEN_LEN.W))
    val rs3_rd = Output(UInt(LENX.W))
    val pc = Output(UInt(LENX.W))
    val alu_out = Output(UInt(LENX.W))
    val dest  = Output(UInt(LENx.W))
}
class WB_INFO extends Bundle {
    val valid = Output(UInt(1.W))
    val exe_fun = Output(UInt(EXE_FUN_LEN.W))
    val rf_wen  = Output(UInt(REN_LEN.W))
    val wb_sel = Output(UInt(WB_SEL_LEN.W))
    val pc = Output(UInt(LENX.W))
    val alu_out = Output(UInt(LENX.W))
    val dest  = Output(UInt(LENx.W))
}
class WRF_INFO extends Bundle {
    val valid = Output(UInt(1.W))
    val ready = Output(UInt(1.W))
    val dest  = Output(UInt(LENx.W))
    val wdata = Output(UInt(LENX.W))
}

package mycpu

import chisel3._
import chisel3.util._
import common._
import common.Consts._
import common.Instructions._

class IF_Stage extends Module {
    val io = IO(new Bundle {
        val inst    = new RAM_IO()
        val to_ds   = new ID_INFO()
        val br      = Flipped(new BR_INFO())
        val ds_allowin = Input(UInt(1.W))
    })
    val to_fs_valid    = ~reset.asUInt
    val fs_ready_go    = 1.U(1.W)
    val fs_valid       = RegInit(0.U(1.W))
    val fs_allowin     = (~fs_valid) | (io.ds_allowin & fs_ready_go)
    when (fs_allowin === 1.U) {fs_valid := to_fs_valid}
    val pc          = RegInit("h1bfffffc".asUInt(32.W))
    val pc_plus_4   = pc + 4.U(32.W)
    val next_pc     = MuxCase(pc_plus_4, Seq(
        io.br.flg  ->  io.br.target
    ))
    io.inst.wen     := 0.U(4.W)
    io.inst.wdata   := 233.U
    io.inst.en      := (to_fs_valid & fs_allowin)
    io.inst.addr    := next_pc

    io.to_ds.valid  := fs_valid & fs_ready_go
    io.to_ds.pc     := pc
    io.to_ds.inst   := io.inst.rdata

    when ((to_fs_valid & fs_allowin) === 1.U) {pc := next_pc}
} 

package mycpu

import chisel3._
import chisel3.util._
import common._
import common.Consts._
import common.Instructions._

class ID_Stage extends Module {
    val io = IO(new Bundle {
        val fs = Flipped(new ID_INFO())
        val ds_allowin = Output(UInt(1.W))
        val es_allowin = Input(UInt(1.W))
        val br      = new BR_INFO()
        val to_exe = new EX_INFO()
        val reg_r1 = Output(UInt(LENx.W))
        val reg_r2 = Output(UInt(LENx.W))
        val reg_r3 = Output(UInt(LENx.W))
        val reg_rdata1 = Input(UInt(LENX.W))
        val reg_rdata2 = Input(UInt(LENX.W))
        val reg_rdata3 = Input(UInt(LENX.W))
    })
    val ds_valid       = RegInit(0.U(1.W))
    val ds_ready_go    = 1.U(1.W)
    val ds_allowin     = (~ds_valid) | (io.es_allowin & ds_ready_go)
    when (ds_allowin === 1.U) {ds_valid := io.fs.valid}
    io.to_exe.valid   := ds_valid & ds_ready_go
    io.ds_allowin     := ds_allowin
    val inst = RegInit(0.U(LENX.W))
    val pc   = RegInit(0.U(LENX.W))
    when ((ds_allowin & io.fs.valid) === 1.U) {
        inst := io.fs.inst
        pc   := io.fs.pc
    }
    val rd      = inst(4, 0)
    val rj      = inst(9, 5)
    val rk      = inst(14, 10)
    io.reg_r1   := rj
    io.reg_r2   := rk
    io.reg_r3   := rd
    val rs1_rd  = io.reg_rdata1
    val rs2_rd  = io.reg_rdata2
    val rs3_rd  = io.reg_rdata3
    val ui5     = inst(14, 10)
    val i12     = inst(21, 10)
    val i16     = inst(25, 10)
    val i20     = inst(24, 5)
    val i26     = Cat(inst(9, 0), inst(25, 10))
    val i12_sex = Cat(Fill(20, i12(11)), i12)
    val of16_sex = Cat(Fill(14, inst(25)), inst(25, 10), 0.U(2.W))
    val of26_sex = Cat(Fill(4, inst(9)), Cat(inst(9, 0), inst(25, 10)), 0.U(2.W))
    val i20_sex  = Cat(inst(24, 5), 0.U(12.W))
    val ID_signals = ListLookup(inst, 
        List(ALU_X, OP1_RS1, OP2_RS2, MEN_X, REN_X, WB_X),
        Array (
            add_w   -> List(ALU_ADD, OP1_RS1, OP2_RS2, MEN_X, REN_S, WB_ALU),
            sub_w   -> List(ALU_SUB, OP1_RS1, OP2_RS2, MEN_X, REN_S, WB_ALU),
            slt     -> List(ALU_SLT, OP1_RS1, OP2_RS2, MEN_X, REN_S, WB_ALU),
            sltu    -> List(ALU_SLTU, OP1_RS1, OP2_RS2, MEN_X, REN_S, WB_ALU),
            nor     -> List(ALU_NOR, OP1_RS1, OP2_RS2, MEN_X, REN_S, WB_ALU),
            and     -> List(ALU_AND, OP1_RS1, OP2_RS2, MEN_X, REN_S, WB_ALU),
            or      -> List(ALU_OR, OP1_RS1, OP2_RS2, MEN_X, REN_S, WB_ALU),
            xor     -> List(ALU_XOR, OP1_RS1, OP2_RS2, MEN_X, REN_S, WB_ALU),
            slli_w  -> List(ALU_SLL, OP1_RS1, OP2_UI5, MEN_X, REN_S, WB_ALU),
            srli_w  -> List(ALU_SRL, OP1_RS1, OP2_UI5, MEN_X, REN_S, WB_ALU),
            srai_w  -> List(ALU_SRA, OP1_RS1, OP2_UI5, MEN_X, REN_S, WB_ALU),
            addi_w  -> List(ALU_ADD, OP1_RS1, OP2_SI12, MEN_X, REN_S, WB_ALU),
            ld_w    -> List(ALU_SL, OP1_RS1, OP2_SI12, MEN_X, REN_S, WB_MEM),
            st_w    -> List(ALU_SL, OP1_RS1, OP2_SI12, MEN_S, REN_X, WB_X),
            jirl    -> List(BR_JIRL, OP1_RS1, OP2_OF16_SEX, MEN_X, REN_S, WB_PC),
            inst_b  -> List(BR_B, OP1_PC, OP2_OF26_SEX, MEN_X, REN_X, WB_X),
            inst_bl -> List(BR_BL, OP1_PC, OP2_OF26_SEX, MEN_X, REN_S, WB_PC),
            beq     -> List(BR_BEQ, OP1_PC, OP2_OF16_SEX, MEN_X, REN_X, WB_X),
            bne     -> List(BR_BNE, OP1_PC, OP2_OF16_SEX, MEN_X, REN_X, WB_X),
            lu12i_w -> List(ALU_LU12I, OP1_X, OP2_SI20_SEX, MEN_X, REN_S, WB_ALU)
        )
    )
    val exe_fun :: op1_sel :: op2_sel :: mem_wen :: rf_wen :: wb_sel :: Nil = ID_signals

    val op1_data = MuxCase(0.U(32.W), Seq(
        (op1_sel === OP1_RS1) -> rs1_rd,
        (op1_sel === OP1_PC)  -> pc
    ))
    val op2_data = MuxCase(0.U(32.W), Seq(
        (op2_sel === OP2_RS2)  -> rs2_rd,
        (op2_sel === OP2_UI5)  -> ui5,
        (op2_sel === OP2_SI12) -> i12_sex,
        (op2_sel === OP2_SI20_SEX) -> i20_sex,
        (op2_sel === OP2_RD)   -> rs3_rd,
        (op2_sel === OP2_OF26) -> i26,
        (op2_sel === OP2_OF16) -> i16,
        (op2_sel === OP2_OF26_SEX) -> of26_sex,
        (op2_sel === OP2_OF16_SEX) -> of16_sex
    ))
    //branch
    io.br.flg := MuxCase(false.B, Seq(
        (exe_fun === BR_BL) -> true.B,
        (exe_fun === BR_B)  -> true.B,
        (exe_fun === BR_JIRL)  -> true.B,
        (exe_fun === BR_BNE)   -> (rs1_rd =/= rs3_rd),
        (exe_fun === BR_BEQ)   -> (rs1_rd === rs3_rd)
    ))
    io.br.target := op1_data + op2_data

    io.to_exe.exe_fun := exe_fun
    io.to_exe.op1_data := op1_data
    io.to_exe.op2_data := op2_data
    io.to_exe.wb_sel := wb_sel
    io.to_exe.mem_wen := mem_wen
    io.to_exe.rf_wen  := rf_wen
    io.to_exe.pc := pc
    io.to_exe.rs3_rd := rs3_rd
    io.to_exe.dest  := rd
}

package mycpu

import chisel3._
import chisel3.util._
import common._
import common.Consts._

class MEM_Stage extends Module {
    val io = IO(new Bundle {
        val es = Flipped(new MEM_INFO())
        val ms_allowin = Output(UInt(1.W))
        val ws_allowin = Input(UInt(1.W))
        val to_wb = new WB_INFO()
        val data = new RAM_IO()
    })

    val ms_ready_go    = 1.U(1.W)
    val ms_valid       = RegInit(0.U(1.W))
    val ms_allowin     = (~ms_valid) | (io.ws_allowin & ms_ready_go)
    when (ms_allowin === 1.U) {ms_valid := io.es.valid}
    io.to_wb.valid       := ms_valid & ms_ready_go
    io.ms_allowin     := ms_allowin

    val dest    = RegInit(0.U(LENx.W))
    val exe_fun = RegInit(0.U(EXE_FUN_LEN.W))
    val rf_wen  = RegInit(0.U(REN_LEN.W))
    val wb_sel  = RegInit(0.U(WB_SEL_LEN.W))
    val mem_wen = RegInit(0.U(MEN_LEN.W))
    val rs3_rd  = RegInit(0.U(LENX.W))
    val pc      = RegInit(0.U(LENX.W))
    val alu_out = RegInit(0.U(LENX.W))
    when ((ms_allowin & io.es.valid).asUInt === 1.U) {
        dest        := io.es.dest
        exe_fun     := io.es.exe_fun
        mem_wen     := io.es.mem_wen
        rf_wen      := io.es.rf_wen
        wb_sel      := io.es.wb_sel
        pc          := io.es.pc
        rs3_rd      := io.es.rs3_rd
        alu_out     := io.es.alu_out
    }
    io.data.wen := mem_wen
    io.data.wdata := rs3_rd
    io.data.addr := alu_out
    io.data.en   := 1.U(1.W)

    io.to_wb.exe_fun   := exe_fun
    io.to_wb.rf_wen    := rf_wen
    io.to_wb.wb_sel    := wb_sel
    io.to_wb.pc        := pc
    io.to_wb.alu_out   := alu_out
    io.to_wb.dest      := dest
}

package mycpu

import chisel3._
import chisel3.util._
import common._
import common.Consts._

class WB_Stage extends Module {
    val io = IO(new Bundle {
        val ms = Flipped(new WB_INFO())
        val ws_allowin = Output(UInt(1.W))
        val debug   = new DEBUG()
        val data_rdata = Input(UInt(LENX.W))
        val reg_wen = Output(UInt(1.W))
        val reg_wr  = Output(UInt(LENx.W))
        val reg_wdata = Output(UInt(LENX.W))
    })
    val ws_ready_go    = 1.U(1.W)
    val ws_valid       = RegInit(0.U(1.W))
    val ws_allowin     = (~ws_valid) | (ws_ready_go)
    when (ws_allowin === 1.U) {ws_valid := io.ms.valid}
    io.ws_allowin     := ws_allowin

    val dest    = RegInit(0.U(LENx.W))
    val exe_fun = RegInit(0.U(EXE_FUN_LEN.W))
    val rf_wen  = RegInit(0.U(REN_LEN.W))
    val wb_sel  = RegInit(0.U(WB_SEL_LEN.W))
    val pc      = RegInit(0.U(LENX.W))
    val alu_out = RegInit(0.U(LENX.W))
    when ((ws_allowin & io.ms.valid).asUInt === 1.U) {
        dest        := io.ms.dest
        exe_fun     := io.ms.exe_fun
        rf_wen      := io.ms.rf_wen
        wb_sel      := io.ms.wb_sel
        pc          := io.ms.pc
        alu_out     := io.ms.alu_out
    }
    val wb_data = MuxCase(alu_out, Seq(
        (wb_sel === WB_MEM) -> io.data_rdata,
        (wb_sel === WB_PC)  -> (pc + 4.U(LENX.W))
    ))
    val wb_addr = Mux(exe_fun === BR_BL, 1.U(LENx.W), dest)
    
    io.reg_wen := rf_wen
    io.reg_wr := wb_addr
    io.reg_wdata := wb_data

    io.debug.wb_pc      := pc
    io.debug.wb_rf_wen  := Fill(4, rf_wen(0))
    io.debug.wb_rf_wnum := wb_addr
    io.debug.wb_rf_wdata:= wb_data
}