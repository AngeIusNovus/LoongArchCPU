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
        val rj = Output(UInt(REG.W))
        val rk = Output(UInt(REG.W))
        val rd = Output(UInt(REG.W))
        val reg_rdata1 = Input(UInt(WORD.W))
        val reg_rdata2 = Input(UInt(WORD.W))
        val reg_rdata3 = Input(UInt(WORD.W))
        val rd_es = Flipped(new TMP_REG())
        val rd_ms = Flipped(new TMP_REG())
        val rd_ws = Flipped(new TMP_REG())
    })

    val ds_valid = RegInit(false.B)
    val ds_ready_go = Wire(Bool())

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
    rj_value := MuxCase(io.reg_rdata1, Seq(
        (io.rd_es.valid && io.rd_es.dest === io.rj) -> io.rd_es.data,
        (io.rd_ms.valid && io.rd_ms.dest === io.rj) -> io.rd_ms.data,
        (io.rd_ws.valid && io.rd_ws.dest === io.rj) -> io.rd_ws.data
    ))
    rk_value := MuxCase(io.reg_rdata2, Seq(
        (io.rd_es.valid && io.rd_es.dest === io.rk) -> io.rd_es.data,
        (io.rd_ms.valid && io.rd_ms.dest === io.rk) -> io.rd_ms.data,
        (io.rd_ws.valid && io.rd_ws.dest === io.rk) -> io.rd_ws.data
    ))
    rd_value := MuxCase(io.reg_rdata3, Seq(
        (io.rd_es.valid && io.rd_es.dest === io.rd) -> io.rd_es.data,
        (io.rd_ms.valid && io.rd_ms.dest === io.rd) -> io.rd_ms.data,
        (io.rd_ws.valid && io.rd_ws.dest === io.rd) -> io.rd_ws.data
    ))

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
        List(ALU_X, BR_X, SRC1_X, SRC2_X, MEM_RX, MEM_WX, RF_X, WB_X, RS_X, RS_X), 
        // ALU操作类型，跳转类型，ALU源操作数1类型，ALU源操作数2类型，内存读使能，内存写使能，寄存器写使能，写回来源，源操作数1寄存器，源操作数2寄存器
        Array (
            add_w   -> List(ALU_ADD,    BR_X,   SRC1_REG, SRC2_REG,  MEM_RX, MEM_WX, RF_S, WB_ALU, RS_J, RS_K),
            sub_w   -> List(ALU_SUB,    BR_X,   SRC1_REG, SRC2_REG,  MEM_RX, MEM_WX, RF_S, WB_ALU, RS_J, RS_K),
            slt     -> List(ALU_SLT,    BR_X,   SRC1_REG, SRC2_REG,  MEM_RX, MEM_WX, RF_S, WB_ALU, RS_J, RS_K),
            sltu    -> List(ALU_SLTU,   BR_X,   SRC1_REG, SRC2_REG,  MEM_RX, MEM_WX, RF_S, WB_ALU, RS_J, RS_K),
            nor     -> List(ALU_NOR,    BR_X,   SRC1_REG, SRC2_REG,  MEM_RX, MEM_WX, RF_S, WB_ALU, RS_J, RS_K), 
            or      -> List(ALU_OR,     BR_X,   SRC1_REG, SRC2_REG,  MEM_RX, MEM_WX, RF_S, WB_ALU, RS_J, RS_K), 
            and     -> List(ALU_AND,    BR_X,   SRC1_REG, SRC2_REG,  MEM_RX, MEM_WX, RF_S, WB_ALU, RS_J, RS_K), 
            xor     -> List(ALU_XOR,    BR_X,   SRC1_REG, SRC2_REG,  MEM_RX, MEM_WX, RF_S, WB_ALU, RS_J, RS_K), 
            slli_w  -> List(ALU_SLL,    BR_X,   SRC1_REG, SRC2_ui5,  MEM_RX, MEM_WX, RF_S, WB_ALU, RS_J, RS_X), 
            srli_w  -> List(ALU_SRL,    BR_X,   SRC1_REG, SRC2_ui5,  MEM_RX, MEM_WX, RF_S, WB_ALU, RS_J, RS_X), 
            srai_w  -> List(ALU_SRA,    BR_X,   SRC1_REG, SRC2_ui5,  MEM_RX, MEM_WX, RF_S, WB_ALU, RS_J, RS_X), 
            addi_w  -> List(ALU_ADD,    BR_X,   SRC1_REG, SRC2_si12, MEM_RX, MEM_WX, RF_S, WB_ALU, RS_J, RS_X),
            ld_w    -> List(ALU_LD,     BR_X,   SRC1_REG, SRC2_si12, MEM_RS, MEM_WX, RF_S, WB_MEM, RS_J, RS_X),
            st_w    -> List(ALU_ST,     BR_X,   SRC1_REG, SRC2_si12, MEM_RS, MEM_WS, RF_X, WB_X,   RS_J, RS_D),
            jirl    -> List(ALU_X,      BR_S,   SRC1_REG, SRC2_si16, MEM_RX, MEM_WX, RF_S, WB_PC,  RS_J, RS_X),
            inst_b  -> List(ALU_X,      BR_S,   SRC1_PC,  SRC2_si26, MEM_RX, MEM_WX, RF_X, WB_X,   RS_X, RS_X),
            inst_bl -> List(ALU_X,      BR_BL,  SRC1_PC,  SRC2_si26, MEM_RX, MEM_WX, RF_S, WB_PC,  RS_X, RS_X),
            beq     -> List(ALU_X,      BR_BEQ, SRC1_PC,  SRC2_si16, MEM_RX, MEM_WX, RF_X, WB_X,   RS_J, RS_D),
            bne     -> List(ALU_X,      BR_BNE, SRC1_PC,  SRC2_si16, MEM_RX, MEM_WX, RF_X, WB_X,   RS_J, RS_D),
            lu12i_w -> List(ALU_LU12I,  BR_X,   SRC1_X,   SRC2_si20, MEM_RX, MEM_WX, RF_S, WB_ALU, RS_X, RS_X)
        ))
    val alu_op :: br_op :: src1_type :: src2_type :: mem_en :: mem_we :: rf_we :: wb_src :: rs1_type :: rs2_type :: Nil = decode

    val rs1 = Wire(UInt(REG.W))
    val rs2 = Wire(UInt(REG.W))
    rs1 := MuxCase(0.U(REG.W), Seq(
        (rs1_type === RS_J) -> io.rj,
        (rs1_type === RS_K) -> io.rk,
        (rs1_type === RS_D) -> io.rd
    ))
    rs2 := MuxCase(0.U(REG.W), Seq(
        (rs2_type === RS_J) -> io.rj,
        (rs2_type === RS_K) -> io.rk,
        (rs2_type === RS_D) -> io.rd
    ))

    val need_rd_es = Wire(Bool())
    need_rd_es := io.rd_es.valid && (io.rd_es.dest === rs1 || io.rd_es.dest === rs2)
    ds_ready_go := Mux(need_rd_es, io.rd_es.ready, true.B)

    val src1_data = Wire(UInt(WORD.W))
    src1_data := MuxCase(0.U(WORD.W), Seq(
        (src1_type === SRC1_REG) -> rj_value,
        (src1_type === SRC1_PC)  -> pc
    ))

    val src2_data = Wire(UInt(WORD.W))
    src2_data := MuxCase(0.U(WORD.W), Seq(
        (src2_type === SRC2_REG)  -> rk_value,
        (src2_type === SRC2_ui5)  -> ui5,
        (src2_type === SRC2_si12) -> si12,
        (src2_type === SRC2_si16) -> si16,
        (src2_type === SRC2_si20) -> si20,
        (src2_type === SRC2_si26) -> si26
    ))
    
    io.br.taken := Mux(!ds_valid, false.B, MuxCase(false.B, Seq(
        ((br_op === BR_S) || (br_op === BR_BL)) -> true.B,  
        (br_op === BR_BNE) -> (rj_value =/= rd_value), 
        (br_op === BR_BEQ) -> (rj_value === rd_value)
    )))
    io.br.target := src1_data + src2_data

    io.to_es.alu_op := alu_op
    io.to_es.src1_data := src1_data
    io.to_es.src2_data := src2_data
    io.to_es.wb_src := wb_src
    io.to_es.mem_en := mem_en
    io.to_es.mem_we := mem_we
    io.to_es.rf_we := rf_we
    io.to_es.rd_value := rd_value
    io.to_es.dest := MuxCase(0.U(REG.W), Seq(
        (br_op === BR_BL) -> 1.U(REG.W),
        (rf_we =/= 0.U(RF_SEL_LEN.W)) -> io.rd
    ))
    io.to_es.pc := pc
}