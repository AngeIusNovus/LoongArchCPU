package csrInclude

import chisel3._ 
import chisel3.util._ 
import common._ 
import common.const._ 

trait MyCSR {
    val id : UInt
    val value : Data
    val rw : UInt
    def write(wdata : UInt) = {
        value := ((~rw & value.asUInt) | (rw & wdata)).asTypeOf(value)
    }
}

class CRMD_INFO extends Bundle {
    val ZERO = UInt(23.W)
    val DATM = UInt(2.W)
    val DATF = UInt(2.W)
    val PG = Bool()
    val DA = Bool()
    val IE = Bool()
    val PLV = UInt(2.W)
}
class CRMD extends MyCSR {
    override val id = CRMD_X
    override val value = RegInit({
        val init = WireDefault(0.U.asTypeOf(new CRMD_INFO))
        init.DA := true.B
        init
    })
    override val rw = "b0000_0000_0000_0000_0000_0001_1111_1111".U
}

class PRMD_INFO extends Bundle {
    val ZERO = UInt(29.W)
    val PIE = Bool()
    val PPLV = UInt(2.W)
}
class PRMD extends MyCSR {
    override val id = PRMD_X
    override val value = RegInit(0.U.asTypeOf(new PRMD_INFO))
    override val rw = "b0000_0000_0000_0000_0000_0000_0000_0111".U
}

class EUEN_INFO extends Bundle {
    val ZERO = UInt(31.W)
    val FPE = Bool()
}
class EUEN extends MyCSR {
    override val id = EUEN_X
    override val value = RegInit(0.U.asTypeOf(new EUEN_INFO))
    override val rw = "b0000_0000_0000_0000_0000_0000_0000_0001".U
}

class ECFG_INFO extends Bundle {
    val ZERO1 = UInt(19.W)
    val LIE1 = UInt(2.W)
    val ZERO2 = Bool()
    val LIE2 = UInt(10.W)
}
class ECFG extends MyCSR {
    override val id = ECFG_X
    override val value = RegInit(0.U.asTypeOf(new ECFG_INFO))
    override val rw = "b0000_0000_0000_0000_0001_1011_1111_1111".U
}

class ESTAT_INFO extends Bundle {
    val ZERO1 = Bool()
    val Esubcode = UInt(9.W)
    val Ecode = UInt(6.W)
    val ZERO2 = UInt(3.W)
    val IS_12 = Bool()
    val IS_11 = Bool()
    val ZERO3 = Bool()
    val IS_9_2 = UInt(8.W)
    val IS_1_0 = UInt(2.W)
}
class ESTAT extends MyCSR {
    override val id = ESTAT_X
    override val value = RegInit(0.U.asTypeOf(new ESTAT_INFO))
    override val rw = "b0000_0000_0000_0000_0000_0000_0000_0011".U
}

class ERA_INFO extends Bundle {
    val pc = UInt(WORD.W)
}
class ERA extends MyCSR {
    override val id = ERA_X
    override val value = RegInit(0.U.asTypeOf(new ERA_INFO))
    override val rw = "b1111_1111_1111_1111_1111_1111_1111_1111".U
}

class BADV_INFO extends Bundle {
    val vaddr = UInt(WORD.W)
}
class BADV extends MyCSR {
    override val id = BADV_X
    override val value = RegInit(0.U.asTypeOf(new BADV_INFO))
    override val rw = "b1111_1111_1111_1111_1111_1111_1111_1111".U
}

class EENTRY_INFO extends Bundle {
    val VA = UInt(26.W)
    val ZERO = UInt(6.W)
}
class EENTRY extends MyCSR {
    override val id = EENTRY_X
    override val value = RegInit(0.U.asTypeOf(new EENTRY_INFO))
    override val rw = "b1111_1111_1111_1111_1111_1111_1100_0000".U
}

class SAVE_INFO extends Bundle {
    val data = UInt(WORD.W)
}
class SAVE0 extends MyCSR {
    override val id = SAVE0_X
    override val value = RegInit(0.U.asTypeOf(new SAVE_INFO))
    override val rw = "b1111_1111_1111_1111_1111_1111_1111_1111".U
}
class SAVE1 extends MyCSR {
    override val id = SAVE1_X
    override val value = RegInit(0.U.asTypeOf(new SAVE_INFO))
    override val rw = "b1111_1111_1111_1111_1111_1111_1111_1111".U
}
class SAVE2 extends MyCSR {
    override val id = SAVE2_X
    override val value = RegInit(0.U.asTypeOf(new SAVE_INFO))
    override val rw = "b1111_1111_1111_1111_1111_1111_1111_1111".U
}
class SAVE3 extends MyCSR {
    override val id = SAVE3_X
    override val value = RegInit(0.U.asTypeOf(new SAVE_INFO))
    override val rw = "b1111_1111_1111_1111_1111_1111_1111_1111".U
}

class TID_INFO extends Bundle {
    val TID = UInt(32.W)
}
class TID extends MyCSR {
    override val id = TID_X
    override val value = RegInit(0.U.asTypeOf(new TID_INFO))
    override val rw = "b1111_1111_1111_1111_1111_1111_1111_1111".U
}

class TCFG_INFO extends Bundle {
    val ZERO = UInt((WORD - COUNT_N).W)
    val InitVal = UInt((COUNT_N - 2).W)
    val Periodic = Bool()
    val En = Bool()
}
class TCFG extends MyCSR {
    override val id = TCFG_X
    override val value = RegInit(0.U.asTypeOf(new TCFG_INFO))
    override val rw = Cat(0.U((WORD - COUNT_N).W), Fill(COUNT_N - 2, 1.U(1.W)), 3.U(2.W))
}

class TVAL_INFO extends Bundle {
    val ZERO = UInt((WORD - COUNT_N).W)
    val TimeVal = UInt(COUNT_N.W)
}
class TVAL extends MyCSR {
    override val id = TVAL_X
    override val value = RegInit(0.U.asTypeOf(new TVAL_INFO))
    override val rw = Cat(0.U((WORD - COUNT_N).W), Fill(COUNT_N, 1.U(1.W)))
}

class TICLR_INFO extends Bundle {
    val ZERO = UInt(31.W)
    val CLR = Bool()
}
class TICLR extends MyCSR {
    override val id = TICLR_X
    override val value = RegInit(0.U.asTypeOf(new TICLR_INFO))
    override val rw = "b0000_0000_0000_0000_0000_0000_0000_0001".U
}