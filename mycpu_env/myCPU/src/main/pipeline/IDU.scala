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
        val ds_flush = Output(Bool())
        val es_flush = Input(Bool())
    })

    val ds_valid = RegInit(false.B)
    val ds_ready_go = Wire(Bool())

    io.ds_allowin := (!ds_valid) || (io.es_allowin && ds_ready_go)
    when (io.ds_allowin || io.es_flush) {
        ds_valid := io.to_ds.valid && !io.es_flush
    }
    io.to_es.valid := ds_valid && ds_ready_go && !io.es_flush

    val inst = RegInit(0.U(WORD.W))
    val pc   = RegInit(0.U(WORD.W))
    when (io.ds_allowin && io.to_ds.valid) {
        inst := io.to_ds.inst
        pc   := io.to_ds.pc
    }

    def get_reg(dest : UInt, init : UInt) : UInt = {
        return MuxCase(init, Seq(
            (io.rd_es.valid && io.rd_es.dest === dest) -> io.rd_es.data,
            (io.rd_ms.valid && io.rd_ms.dest === dest) -> io.rd_ms.data,
            (io.rd_ws.valid && io.rd_ws.dest === dest) -> io.rd_ws.data
        ))
    }
    io.rd := inst(4, 0)
    io.rj := inst(9, 5)
    io.rk := inst(14, 10)
    val rj_value = Wire(UInt(WORD.W))
    val rk_value = Wire(UInt(WORD.W))
    val rd_value = Wire(UInt(WORD.W))
    rj_value := get_reg(io.rj, io.reg_rdata1)
    rk_value := get_reg(io.rk, io.reg_rdata2)
    rd_value := get_reg(io.rd, io.reg_rdata3)

    val ui5  = Wire(UInt(WORD.W))
    val i12  = Wire(UInt(12.W))
    val i16  = Wire(UInt(16.W))
    val i20  = Wire(UInt(20.W))
    val ui12 = Wire(UInt(WORD.W))
    val si12 = Wire(UInt(WORD.W))
    val si16 = Wire(UInt(WORD.W))
    val si20 = Wire(UInt(WORD.W))
    val si26 = Wire(UInt(WORD.W))
    val rc = Wire(UInt(CSR_ADDR.W))
    rc  := inst(23, 10)
    ui5 := inst(14, 10)
    i12 := inst(21, 10)
    i16 := inst(25, 10)
    i20 := inst(24, 5)
    ui12 := Cat(0.U(20.W), i12)
    si12 := Cat(Fill(20, i12(11)), i12)
    si16 := Cat(Fill(14, i16(15)), i16, 0.U(2.W))
    si20 := Cat(i20, 0.U(12.W))
    si26 := Cat(Fill(4, inst(9)), inst(9, 0), inst(25, 10), 0.U(2.W))

    val decode = ListLookup(inst, 
        List(ALU_X, BR_X, CSR_X, SRC1_X, SRC2_X, MEM_RX, MEM_WX, RF_X, WB_X, RS_X, RS_X), 
        // ALU操作类型，跳转类型，异常类型，ALU源操作数1类型，ALU源操作数2类型，内存读使能，内存写使能，寄存器写使能，写回来源，源操作数1寄存器，源操作数2寄存器
        Array (
            add_w     -> List(ALU_ADD,    BR_X,    CSR_X, SRC1_REG, SRC2_REG,  MEM_RX, MEM_WX, RF_S, WB_ALU, RS_J, RS_K),
            sub_w     -> List(ALU_SUB,    BR_X,    CSR_X, SRC1_REG, SRC2_REG,  MEM_RX, MEM_WX, RF_S, WB_ALU, RS_J, RS_K),
            slt       -> List(ALU_SLT,    BR_X,    CSR_X, SRC1_REG, SRC2_REG,  MEM_RX, MEM_WX, RF_S, WB_ALU, RS_J, RS_K),
            slti      -> List(ALU_SLT,    BR_X,    CSR_X, SRC1_REG, SRC2_si12, MEM_RX, MEM_WX, RF_S, WB_ALU, RS_J, RS_X),
            sltu      -> List(ALU_SLTU,   BR_X,    CSR_X, SRC1_REG, SRC2_REG,  MEM_RX, MEM_WX, RF_S, WB_ALU, RS_J, RS_K),
            sltui     -> List(ALU_SLTU,   BR_X,    CSR_X, SRC1_REG, SRC2_si12, MEM_RX, MEM_WX, RF_S, WB_ALU, RS_J, RS_X),
            nor       -> List(ALU_NOR,    BR_X,    CSR_X, SRC1_REG, SRC2_REG,  MEM_RX, MEM_WX, RF_S, WB_ALU, RS_J, RS_K),
            or        -> List(ALU_OR,     BR_X,    CSR_X, SRC1_REG, SRC2_REG,  MEM_RX, MEM_WX, RF_S, WB_ALU, RS_J, RS_K),
            ori       -> List(ALU_OR,     BR_X,    CSR_X, SRC1_REG, SRC2_ui12, MEM_RX, MEM_WX, RF_S, WB_ALU, RS_J, RS_X),
            and       -> List(ALU_AND,    BR_X,    CSR_X, SRC1_REG, SRC2_REG,  MEM_RX, MEM_WX, RF_S, WB_ALU, RS_J, RS_K),
            andi      -> List(ALU_AND,    BR_X,    CSR_X, SRC1_REG, SRC2_ui12, MEM_RX, MEM_WX, RF_S, WB_ALU, RS_J, RS_X),
            xor       -> List(ALU_XOR,    BR_X,    CSR_X, SRC1_REG, SRC2_REG,  MEM_RX, MEM_WX, RF_S, WB_ALU, RS_J, RS_K),
            xori      -> List(ALU_XOR,    BR_X,    CSR_X, SRC1_REG, SRC2_ui12, MEM_RX, MEM_WX, RF_S, WB_ALU, RS_J, RS_X),
            sll_w     -> List(ALU_SLL,    BR_X,    CSR_X, SRC1_REG, SRC2_REG,  MEM_RX, MEM_WX, RF_S, WB_ALU, RS_J, RS_K),
            slli_w    -> List(ALU_SLL,    BR_X,    CSR_X, SRC1_REG, SRC2_ui5,  MEM_RX, MEM_WX, RF_S, WB_ALU, RS_J, RS_X),
            srl_w     -> List(ALU_SRL,    BR_X,    CSR_X, SRC1_REG, SRC2_REG,  MEM_RX, MEM_WX, RF_S, WB_ALU, RS_J, RS_K),
            srli_w    -> List(ALU_SRL,    BR_X,    CSR_X, SRC1_REG, SRC2_ui5,  MEM_RX, MEM_WX, RF_S, WB_ALU, RS_J, RS_X),
            sra_w     -> List(ALU_SRA,    BR_X,    CSR_X, SRC1_REG, SRC2_REG,  MEM_RX, MEM_WX, RF_S, WB_ALU, RS_J, RS_K),
            srai_w    -> List(ALU_SRA,    BR_X,    CSR_X, SRC1_REG, SRC2_ui5,  MEM_RX, MEM_WX, RF_S, WB_ALU, RS_J, RS_X),
            addi_w    -> List(ALU_ADD,    BR_X,    CSR_X, SRC1_REG, SRC2_si12, MEM_RX, MEM_WX, RF_S, WB_ALU, RS_J, RS_X),
            ld_b      -> List(ALU_LD,     BR_X,    CSR_X, SRC1_REG, SRC2_si12, MEM_RB, MEM_WX, RF_S, WB_MEM, RS_J, RS_X),
            ld_h      -> List(ALU_LD,     BR_X,    CSR_X, SRC1_REG, SRC2_si12, MEM_RH, MEM_WX, RF_S, WB_MEM, RS_J, RS_X),
            ld_w      -> List(ALU_LD,     BR_X,    CSR_X, SRC1_REG, SRC2_si12, MEM_RS, MEM_WX, RF_S, WB_MEM, RS_J, RS_X),
            ld_bu     -> List(ALU_LD,     BR_X,    CSR_X, SRC1_REG, SRC2_si12, MEM_RBU,MEM_WX, RF_S, WB_MEM, RS_J, RS_X),
            ld_hu     -> List(ALU_LD,     BR_X,    CSR_X, SRC1_REG, SRC2_si12, MEM_RHU,MEM_WX, RF_S, WB_MEM, RS_J, RS_X),
            st_b      -> List(ALU_ST,     BR_X,    CSR_X, SRC1_REG, SRC2_si12, MEM_RS, MEM_WB, RF_X, WB_X,   RS_J, RS_D),
            st_h      -> List(ALU_ST,     BR_X,    CSR_X, SRC1_REG, SRC2_si12, MEM_RS, MEM_WH, RF_X, WB_X,   RS_J, RS_D),
            st_w      -> List(ALU_ST,     BR_X,    CSR_X, SRC1_REG, SRC2_si12, MEM_RS, MEM_WS, RF_X, WB_X,   RS_J, RS_D),
            jirl      -> List(ALU_X,      BR_S,    CSR_X, SRC1_REG, SRC2_si16, MEM_RX, MEM_WX, RF_S, WB_PC,  RS_J, RS_X),
            inst_b    -> List(ALU_X,      BR_S,    CSR_X, SRC1_PC,  SRC2_si26, MEM_RX, MEM_WX, RF_X, WB_X,   RS_X, RS_X),
            inst_bl   -> List(ALU_X,      BR_BL,   CSR_X, SRC1_PC,  SRC2_si26, MEM_RX, MEM_WX, RF_S, WB_PC,  RS_X, RS_X),
            beq       -> List(ALU_X,      BR_BEQ,  CSR_X, SRC1_PC,  SRC2_si16, MEM_RX, MEM_WX, RF_X, WB_X,   RS_J, RS_D),
            bne       -> List(ALU_X,      BR_BNE,  CSR_X, SRC1_PC,  SRC2_si16, MEM_RX, MEM_WX, RF_X, WB_X,   RS_J, RS_D),
            blt       -> List(ALU_X,      BR_BLT,  CSR_X, SRC1_PC,  SRC2_si16, MEM_RX, MEM_WX, RF_X, WB_X,   RS_J, RS_D),
            bltu      -> List(ALU_X,      BR_BLTU, CSR_X, SRC1_PC,  SRC2_si16, MEM_RX, MEM_WX, RF_X, WB_X,   RS_J, RS_D),
            bge       -> List(ALU_X,      BR_BGE,  CSR_X, SRC1_PC,  SRC2_si16, MEM_RX, MEM_WX, RF_X, WB_X,   RS_J, RS_D),
            bgeu      -> List(ALU_X,      BR_BGEU, CSR_X, SRC1_PC,  SRC2_si16, MEM_RX, MEM_WX, RF_X, WB_X,   RS_J, RS_D),
            lu12i_w   -> List(ALU_LU12I,  BR_X,    CSR_X, SRC1_X,   SRC2_si20, MEM_RX, MEM_WX, RF_S, WB_ALU, RS_X, RS_X),
            pcaddu12i -> List(ALU_ADD,    BR_X,    CSR_X, SRC1_PC,  SRC2_si20, MEM_RX, MEM_WX, RF_S, WB_ALU, RS_X, RS_X),
            mul_w     -> List(ALU_MUL,    BR_X,    CSR_X, SRC1_REG, SRC2_REG,  MEM_RX, MEM_WX, RF_S, WB_ALU, RS_J, RS_K),
            mulh_w    -> List(ALU_MULH,   BR_X,    CSR_X, SRC1_REG, SRC2_REG,  MEM_RX, MEM_WX, RF_S, WB_ALU, RS_J, RS_K),
            mulh_wu   -> List(ALU_MULHU,  BR_X,    CSR_X, SRC1_REG, SRC2_REG,  MEM_RX, MEM_WX, RF_S, WB_ALU, RS_J, RS_K),
            div_w     -> List(ALU_DIV,    BR_X,    CSR_X, SRC1_REG, SRC2_REG,  MEM_RX, MEM_WX, RF_S, WB_ALU, RS_J, RS_K),
            div_wu    -> List(ALU_DIVU,   BR_X,    CSR_X, SRC1_REG, SRC2_REG,  MEM_RX, MEM_WX, RF_S, WB_ALU, RS_J, RS_K),
            mod_w     -> List(ALU_MOD,    BR_X,    CSR_X, SRC1_REG, SRC2_REG,  MEM_RX, MEM_WX, RF_S, WB_ALU, RS_J, RS_K),
            mod_wu    -> List(ALU_MODU,   BR_X,    CSR_X, SRC1_REG, SRC2_REG,  MEM_RX, MEM_WX, RF_S, WB_ALU, RS_J, RS_K),
            csrrd     -> List(ALU_X,      BR_X,    CSR_RD, SRC1_X, SRC2_X, MEM_RX, MEM_WX, RF_S, WB_CSR, RS_X, RS_X),
            csrwr     -> List(ALU_X,      BR_X,    CSR_WR, SRC1_X, SRC2_X, MEM_RX, MEM_WX, RF_S, WB_BOTH, RS_X, RS_X),
            csrxchg   -> List(ALU_X,      BR_X,    CSR_XCHG, SRC1_REG, SRC2_X, MEM_RX, MEM_WX, RF_S, WB_BOTH, RS_J, RS_X),
            syscall   -> List(ALU_X,      BR_X,    CSR_SYSCALL, SRC1_X, SRC2_X, MEM_RX, MEM_WX, RF_X, WB_X, RS_X, RS_X),
            ertn      -> List(ALU_X,      BR_X,    CSR_ERTN, SRC1_X, SRC2_X, MEM_RX, MEM_WX, RF_X, WB_X, RS_X, RS_X)
        ))
    val alu_op :: br_op :: csr_op :: src1_type :: src2_type :: mem_en :: mem_we :: rf_we :: wb_src :: rs1_type :: rs2_type :: Nil = decode

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

    def need_rd(rd : TMP_REG, rs1 : UInt, rs2 : UInt) : Bool = {
        return rd.valid && (rd.dest === rs1 || rd.dest === rs2)
    }
    val need_rd_es = Wire(Bool())
    val need_rd_ms = Wire(Bool())
    val need_rd_ws = Wire(Bool())
    need_rd_es := need_rd(io.rd_es, rs1, rs2)
    need_rd_ms := need_rd(io.rd_ms, rs1, rs2)
    need_rd_ws := need_rd(io.rd_ws, rs1, rs2)
    ds_ready_go := (!need_rd_es || io.rd_es.ready) && (!need_rd_ms || io.rd_ms.ready) && (!need_rd_ws || io.rd_ws.ready)

    val src1_data = Wire(UInt(WORD.W))
    src1_data := MuxCase(0.U(WORD.W), Seq(
        (src1_type === SRC1_REG) -> rj_value,
        (src1_type === SRC1_PC)  -> pc
    ))

    val src2_data = Wire(UInt(WORD.W))
    src2_data := MuxCase(0.U(WORD.W), Seq(
        (src2_type === SRC2_REG)  -> rk_value,
        (src2_type === SRC2_ui5)  -> ui5,
        (src2_type === SRC2_ui12) -> ui12,
        (src2_type === SRC2_si12) -> si12,
        (src2_type === SRC2_si16) -> si16,
        (src2_type === SRC2_si20) -> si20,
        (src2_type === SRC2_si26) -> si26
    ))
    
    io.br.taken := ds_valid && MuxCase(false.B, Seq(
        ((br_op === BR_S) || (br_op === BR_BL)) -> true.B,  
        (br_op === BR_BNE)  -> (rj_value =/= rd_value), 
        (br_op === BR_BEQ)  -> (rj_value === rd_value),
        (br_op === BR_BLT)  -> (rj_value.asSInt < rd_value.asSInt),
        (br_op === BR_BLTU) -> (rj_value.asUInt < rd_value.asUInt),
        (br_op === BR_BGE)  -> (rj_value.asSInt > rd_value.asSInt),
        (br_op === BR_BGEU) -> (rj_value.asUInt > rd_value.asUInt)
    ))
    io.br.target := src1_data + src2_data

    io.to_es.csr.Excp := MuxCase(0.U(EXCP_LEN.W), Seq(
        (csr_op === CSR_SYSCALL) -> 1.U(EXCP_LEN.W),
        (csr_op === CSR_ERTN)    -> 2.U(EXCP_LEN.W)
    ))
    io.to_es.csr.Ecode := MuxCase(0.U(ECODE_LEN.W), Seq(
        (csr_op === CSR_SYSCALL) -> "hb".U(ECODE_LEN.W)
    ))
    io.to_es.csr.Esubcode := MuxCase(0.U(ESUBCODE_LEN.W), Seq(
        (csr_op === CSR_SYSCALL) -> 0.U(ESUBCODE_LEN.W)
    ))
    io.to_es.csr.pc := pc
    io.to_es.csr.en_mask := (csr_op === CSR_XCHG)
    io.to_es.csr.we := (wb_src === WB_BOTH)
    io.to_es.csr.waddr := rc
    io.to_es.csr.wdata := rd_value
    io.to_es.csr.mask := rj_value
    io.to_es.csr.raddr := rc

    io.ds_flush := io.es_flush || (ds_valid && io.to_es.csr.Excp =/= 0.U(2.W))

    io.to_es.alu_op := alu_op
    io.to_es.src1_data := src1_data
    io.to_es.src2_data := src2_data
    io.to_es.wb_src := wb_src
    io.to_es.mem_re := mem_en
    io.to_es.mem_we := mem_we
    io.to_es.rf_we := rf_we
    io.to_es.rd_value := rd_value
    io.to_es.dest := MuxCase(0.U(REG.W), Seq(
        (br_op === BR_BL) -> 1.U(REG.W),
        (rf_we =/= 0.U(RF_SEL_LEN.W)) -> io.rd
    ))
    io.to_es.pc := pc
}