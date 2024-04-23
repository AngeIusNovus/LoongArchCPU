package mycpu_top

import chisel3._
import chisel3.util._

class MYCPU_TOP extends Module {
    val io = IO(new Bundle {
        val resetn            = Input(Bool())
        // inst sram interface
        val inst_sram_we      = Output(Bool())
        val inst_sram_addr    = Output(UInt(32.W))
        val inst_sram_wdata   = Output(UInt(32.W))
        val inst_sram_rdata   = Input(UInt(32.W))
        // data sram interface
        val data_sram_we      = Output(Bool())
        val data_sram_addr    = Output(UInt(32.W))
        val data_sram_wdata   = Output(UInt(32.W))
        val data_sram_rdata   = Input(UInt(32.W))
        // trace debug interface
        val debug_wb_pc       = Output(UInt(32.W))
        val debug_wb_rf_we    = Output(UInt(4.W))
        val debug_wb_rf_wnum  = Output(UInt(5.W))
        val debug_wb_rf_wdata = Output(UInt(32.W))
    })

    val my_reset = RegNext(~io.resetn, init = false.B)
    val valid = RegInit(false.B)

    when(my_reset) {
        valid := false.B
    } .otherwise {
        valid := true.B
    }

    val seq_pc    = Wire(UInt(32.W))
    val nextpc    = Wire(UInt(32.W))
    val br_taken  = Wire(Bool())
    val br_target = Wire(UInt(32.W))
    val inst      = Wire(UInt(32.W))
    val pc        = RegInit(0x1bfffffcL.U(32.W))

    val alu_op        = WireInit(0.U(12.W))
//  val load_op       = Wire(Bool())
    val src1_is_pc    = Wire(Bool())
    val src2_is_imm   = Wire(Bool())
    val res_from_mem  = Wire(Bool())
    val dst_is_r1     = Wire(Bool())
    val gr_we         = Wire(Bool())
    val mem_we        = Wire(Bool())
    val src_reg_is_rd = Wire(Bool())
    val dest          = Wire(UInt(5.W))
    val rj_value      = Wire(UInt(32.W))
    val rkd_value     = Wire(UInt(32.W))
    val imm           = Wire(UInt(32.W))
    val br_offs       = Wire(UInt(32.W))
    val jirl_offs     = Wire(UInt(32.W))

    val op_31_26 = Wire(UInt(6.W))
    val op_25_22 = Wire(UInt(4.W))
    val op_21_20 = Wire(UInt(2.W))
    val op_19_15 = Wire(UInt(5.W))
    val rd       = Wire(UInt(5.W))
    val rj       = Wire(UInt(5.W))
    val rk       = Wire(UInt(5.W))
    val i12      = Wire(UInt(12.W))
    val i20      = Wire(UInt(20.W))
    val i16      = Wire(UInt(16.W))
    val i26      = Wire(UInt(26.W))

    val op_31_26_d = Wire(UInt(64.W))
    val op_25_22_d = Wire(UInt(16.W))
    val op_21_20_d = Wire(UInt( 4.W))
    val op_19_15_d = Wire(UInt(32.W))

    val inst_add_w   = Wire(Bool())
    val inst_sub_w   = Wire(Bool())
    val inst_slt     = Wire(Bool())
    val inst_sltu    = Wire(Bool())
    val inst_nor     = Wire(Bool())
    val inst_and     = Wire(Bool())
    val inst_or      = Wire(Bool())
    val inst_xor     = Wire(Bool())
    val inst_slli_w  = Wire(Bool())
    val inst_srli_w  = Wire(Bool())
    val inst_srai_w  = Wire(Bool())
    val inst_addi_w  = Wire(Bool())
    val inst_ld_w    = Wire(Bool())
    val inst_st_w    = Wire(Bool())
    val inst_jirl    = Wire(Bool())
    val inst_b       = Wire(Bool())
    val inst_bl      = Wire(Bool())
    val inst_beq     = Wire(Bool())
    val inst_bne     = Wire(Bool())
    val inst_lu12i_w = Wire(Bool())

    val need_ui5  = Wire(Bool())
    val need_si12 = Wire(Bool())
    val need_si16 = Wire(Bool())
    val need_si20 = Wire(Bool())
    val need_si26 = Wire(Bool())
    val src2_is_4 = Wire(Bool())

    val rf_raddr1 = Wire(UInt(5.W))
    val rf_rdata1 = Wire(UInt(32.W))
    val rf_raddr2 = Wire(UInt(5.W))
    val rf_rdata2 = Wire(UInt(32.W))
    val rf_we     = Wire(Bool())
    val rf_waddr  = Wire(UInt(5.W))
    val rf_wdata  = Wire(UInt(32.W))

    val alu_src1   = Wire(UInt(32.W))
    val alu_src2   = Wire(UInt(32.W))
    val alu_result = Wire(UInt(32.W))

    val mem_result = Wire(UInt(32.W))

    seq_pc := pc + 4.U
    nextpc := Mux(br_taken, br_target, seq_pc)


    when(my_reset) {
        pc := 0x1bfffffcL.U
    } .otherwise {
        pc := nextpc
    }

    io.inst_sram_we := false.B
    io.inst_sram_addr  := pc
    io.inst_sram_wdata := 0.U(32.W)
    inst            := io.inst_sram_rdata

    op_31_26 := inst(31, 26)
    op_25_22 := inst(25, 22)
    op_21_20 := inst(21, 20)
    op_19_15 := inst(19, 15)

    rd := inst(4, 0)
    rj := inst(9, 5)
    rk := inst(14, 10)

    i12 := inst(21, 10)
    i20 := inst(24, 5)
    i16 := inst(25, 10)
    i26 := Cat(inst(9, 0), inst(25, 10))

    val u_dec0 = Module(new Decoder_6_64())
    u_dec0.io.in := op_31_26
    op_31_26_d := u_dec0.io.out.asUInt
    
    val u_dec1 = Module(new Decoder_4_16())
    u_dec1.io.in := op_25_22
    op_25_22_d := u_dec1.io.out.asUInt
    
    val u_dec2 = Module(new Decoder_2_4())
    u_dec2.io.in := op_21_20
    op_21_20_d := u_dec2.io.out.asUInt
    
    val u_dec3 = Module(new Decoder_5_32())
    u_dec3.io.in := op_19_15
    op_19_15_d := u_dec3.io.out.asUInt

    inst_add_w   := op_31_26_d(0) & op_25_22_d(0) & op_21_20_d(1) & op_19_15_d(0)
    inst_sub_w   := op_31_26_d(0) & op_25_22_d(0) & op_21_20_d(1) & op_19_15_d(2)
    inst_slt     := op_31_26_d(0) & op_25_22_d(0) & op_21_20_d(1) & op_19_15_d(4)
    inst_sltu    := op_31_26_d(0) & op_25_22_d(0) & op_21_20_d(1) & op_19_15_d(5)
    inst_nor     := op_31_26_d(0) & op_25_22_d(0) & op_21_20_d(1) & op_19_15_d(8)
    inst_and     := op_31_26_d(0) & op_25_22_d(0) & op_21_20_d(1) & op_19_15_d(9)
    inst_or      := op_31_26_d(0) & op_25_22_d(0) & op_21_20_d(1) & op_19_15_d(10)
    inst_xor     := op_31_26_d(0) & op_25_22_d(0) & op_21_20_d(1) & op_19_15_d(11)
    inst_slli_w  := op_31_26_d(0) & op_25_22_d(1) & op_21_20_d(0) & op_19_15_d(1)
    inst_srli_w  := op_31_26_d(0) & op_25_22_d(1) & op_21_20_d(0) & op_19_15_d(9)
    inst_srai_w  := op_31_26_d(0) & op_25_22_d(1) & op_21_20_d(0) & op_19_15_d(17)
    inst_addi_w  := op_31_26_d(0) & op_25_22_d(10)
    inst_ld_w    := op_31_26_d(10) & op_25_22_d(2)
    inst_st_w    := op_31_26_d(10) & op_25_22_d(6)
    inst_jirl    := op_31_26_d(19)
    inst_b       := op_31_26_d(20)
    inst_bl      := op_31_26_d(21)
    inst_beq     := op_31_26_d(22)
    inst_bne     := op_31_26_d(23)
    inst_lu12i_w := op_31_26_d(5) & ~inst(25)

    alu_op := Cat(inst_lu12i_w,
                  inst_srai_w,
                  inst_srli_w,
                  inst_slli_w,
                  inst_xor,
                  inst_or,
                  inst_nor,
                  inst_and,
                  inst_sltu,
                  inst_slt,
                  inst_sub_w,
                  inst_add_w | inst_addi_w | inst_ld_w | inst_st_w | inst_jirl | inst_bl
                 )

    need_ui5  := inst_slli_w | inst_srli_w | inst_srai_w
    need_si12 := inst_addi_w | inst_ld_w | inst_st_w
    need_si16 := inst_jirl | inst_beq | inst_bne
    need_si20 := inst_lu12i_w
    need_si26 := inst_b | inst_bl
    src2_is_4 := inst_jirl | inst_bl

    imm := Mux(src2_is_4, 4.U(32.W), 
           Mux(need_si20, Cat(i20(19, 0), 0.U(12.W)), 
           Mux(need_si12, Cat(Fill(20, i12(11)), i12(11, 0)), 
                          Cat(0.U(27.W), inst(14, 10)))))

    jirl_offs := Cat(Fill(14, i16(15)), i16(15, 0), 0.U(2.W))

    br_offs := Mux(need_si26, Cat(Fill(4, i26(25)), i26(25, 0), 0.U(2.W)), 
                              jirl_offs)

    src_reg_is_rd := inst_beq | inst_bne | inst_st_w

    src1_is_pc    := inst_jirl | inst_bl

    src2_is_imm   := inst_slli_w | inst_srli_w | inst_srai_w |
                     inst_addi_w | inst_ld_w | inst_st_w |
                     inst_lu12i_w | inst_jirl | inst_bl

    res_from_mem  := inst_ld_w
    dst_is_r1     := inst_bl
    gr_we         := ~inst_st_w & ~inst_beq & ~inst_bne & ~inst_b
    mem_we        := inst_st_w
    dest          := Mux(dst_is_r1, 1.U(5.W), rd)

    rf_raddr1 := rj
    rf_raddr2 := Mux(src_reg_is_rd, rd, rk)
    val u_regfile = Module(new RegFile())
    u_regfile.io.raddr1 := rf_raddr1
    rf_rdata1 := u_regfile.io.rdata1
    u_regfile.io.raddr2 := rf_raddr2
    rf_rdata2 := u_regfile.io.rdata2
    u_regfile.io.we := rf_we
    u_regfile.io.waddr := rf_waddr
    u_regfile.io.wdata := rf_wdata
    
    rj_value  := rf_rdata1
    rkd_value := rf_rdata2

    val rj_eq_rd = Wire(Bool())
    rj_eq_rd  := (rj_value === rkd_value)
    br_taken  := (   inst_beq && rj_eq_rd
                  || inst_bne && !rj_eq_rd
                  || inst_jirl
                  || inst_bl
                  || inst_b
                 ) && valid
    br_target := Mux(inst_beq || inst_bne || inst_bl || inst_b, pc + br_offs
                                                              , rj_value + jirl_offs)
    
    alu_src1 := Mux(src1_is_pc, pc(31, 0), rj_value)
    alu_src2 := Mux(src2_is_imm, imm, rkd_value)

    val u_alu = Module(new ALU())
    u_alu.io.aluOp   := alu_op
    u_alu.io.aluSrc1 := alu_src1
    u_alu.io.aluSrc2 := alu_src2
    alu_result := u_alu.io.aluResult

    io.data_sram_we    := mem_we && valid
    io.data_sram_addr  := alu_result
    io.data_sram_wdata := rkd_value

    val final_result = Wire(UInt(32.W))
    mem_result   := io.data_sram_rdata
    final_result := Mux(res_from_mem, mem_result, alu_result)

    rf_we    := gr_we && valid
    rf_waddr := dest
    rf_wdata := final_result

    io.debug_wb_pc       := pc
    io.debug_wb_rf_we    := Fill(4, rf_we)
    io.debug_wb_rf_wnum  := dest
    io.debug_wb_rf_wdata := final_result
}