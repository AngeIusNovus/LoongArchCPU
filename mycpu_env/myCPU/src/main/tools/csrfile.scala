package mycpu

import chisel3._
import chisel3.util._
import common._
import common.const._
import csrInclude._

class CsrFile extends Module {
    val io = IO(new Bundle {
        val csr = Flipped(new CSR_IO())
        val csr_taken = Output(Bool())
        val csr_target = Output(UInt(WORD.W))
        val en_INT = Output(Bool())
        val EcounterL = Output(UInt(WORD.W))
        val EcounterH = Output(UInt(WORD.W))
        val EcounterID = Output(UInt(WORD.W))
    })

    val CRMD = new CRMD()
    val PRMD = new PRMD()
    val EUEN = new EUEN()
    val ECFG = new ECFG()
    val ESTAT = new ESTAT()
    val ERA = new ERA()
    val BADV = new BADV()
    val EENTRY = new EENTRY()
    val SAVE0 = new SAVE0()
    val SAVE1 = new SAVE1()
    val SAVE2 = new SAVE2()
    val SAVE3 = new SAVE3()
    val TID = new TID()
    val TCFG = new TCFG()
    val TVAL = new TVAL()
    val TICLR = new TICLR()

    val csr_list = Seq(
        CRMD, PRMD, EUEN, ECFG, ESTAT, ERA, BADV, EENTRY, SAVE0, SAVE1, SAVE2, SAVE3, TID, TCFG, TVAL, TICLR
    )

    val stable_counter = RegInit(0.U(64.W))
    stable_counter := Mux(stable_counter === "hffffffffffffffff".U, 0.U(64.W), stable_counter + 1.U)

    io.EcounterL := stable_counter(31, 0)
    io.EcounterH := stable_counter(63, 32)
    io.EcounterID := Mux(io.csr.we && (io.csr.waddr === TID_X), io.csr.wdata, TID.value.TID)

    io.csr.rdata := 0.U(WORD.W)
    for (csr <- csr_list) {
        when (csr.id === io.csr.raddr) {
            io.csr.rdata := Mux(csr.id === TICLR_X, 0.U(WORD.W), csr.value.asUInt)
        }
    }

    io.csr_taken := io.csr.Excp =/= 0.U(2.W)
    io.csr_target := Mux(io.csr.Excp === 1.U(2.W), EENTRY.value.asUInt, ERA.value.asUInt)
    io.en_INT := (Cat(ESTAT.value.IS_12, ESTAT.value.IS_11, ESTAT.value.IS_9_2, ESTAT.value.IS_1_0) & 
                 (Cat(ECFG.value.LIE2, ECFG.value.LIE1))).orR.asBool && CRMD.value.IE

    when (io.csr.Excp === 1.U(EXCP_LEN.W)) {
        PRMD.value.PPLV := CRMD.value.PLV
        PRMD.value.PIE := CRMD.value.IE
        CRMD.value.PLV := "b00".U
        CRMD.value.IE := false.B
        ERA.write(io.csr.pc)
        ESTAT.value.Esubcode := io.csr.Esubcode
        ESTAT.value.Ecode := io.csr.Ecode
        when (io.csr.badv) {
            BADV.value.vaddr := io.csr.badvaddr
        }
    }.elsewhen (io.csr.Excp === 2.U(EXCP_LEN.W)) {
        CRMD.value.PLV := PRMD.value.PPLV
        CRMD.value.IE := PRMD.value.PIE
        when (ESTAT.value.Ecode === "h3F".U(6.W)) {
            CRMD.value.PG := true.B
            CRMD.value.DA := false.B
        }
    }

    val mask = Mux(io.csr.en_mask, io.csr.mask, "b1111_1111_1111_1111_1111_1111_1111_1111".U)
    def getMaskedData(lst : UInt, now : UInt, mask : UInt) : UInt = {
        return (~mask & lst) | (mask & now)
    }

    when (io.csr.we && io.csr.waddr === TCFG_X) {
        val final_data = getMaskedData(TCFG.value.asUInt, io.csr.wdata, mask)
        TVAL.value.TimeVal := Cat(final_data, 1.U(2.W))
    }.elsewhen (TCFG.value.En) {
        when (TVAL.value.TimeVal === 0.U) {
            TVAL.value.TimeVal := Mux(TCFG.value.Periodic, Cat(TCFG.value.InitVal, 0.U(2.W)), 0.U(COUNT_N.W))
        }.otherwise {
            TVAL.value.TimeVal := TVAL.value.TimeVal - 1.U
        }
    }

    val TvalLst = RegNext(TVAL.value.TimeVal)
    when (TCFG.value.En && TVAL.value.TimeVal === 0.U(COUNT_N.W) && TvalLst === 1.U(COUNT_N.W)) {
        ESTAT.value.IS_11 := true.B
    }

    when (io.csr.we) { 
        for (csr <- csr_list) {
            when (csr.id === io.csr.waddr) {
                val final_data = (~mask & csr.value.asUInt) | (mask & io.csr.wdata)
                csr.write(final_data)
                when (csr.id === CRMD_X && final_data(4) === 1.U(1.W)) {
                    CRMD.value.DATF := "b01".U
                    CRMD.value.DATM := "b01".U
                }
                when (csr.id === TICLR_X && TICLR.value.CLR) {
                    ESTAT.value.IS_11 := false.B
                }
            }
        }
    }
}