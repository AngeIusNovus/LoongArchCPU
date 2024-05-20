package mycpu

import chisel3._
import chisel3.util._
import common._
import common.const._

class CsrFile extends Module {
    val io = IO(new Bundle {
        val csr = Flipped(new CSR_IO())
        val csr_taken = Output(Bool())
        val csr_target = Output(UInt(WORD.W))
    })

    val rf = Mem(9, UInt(WORD.W))
    when (reset.asBool) {
        rf.write(0.U, 8.U(WORD.W))
        rf.write(1.U, 0.U(WORD.W))
        rf.write(2.U, 0.U(WORD.W))
        rf.write(3.U, 0.U(WORD.W))
        rf.write(4.U, 0.U(WORD.W))
        rf.write(5.U, 0.U(WORD.W))
        rf.write(6.U, 0.U(WORD.W))
        rf.write(7.U, 0.U(WORD.W))
        rf.write(8.U, 0.U(WORD.W))
    }
    def get_vaddr(addr : UInt) : UInt = {
        MuxCase(CSR_BADADDR, Seq(
            (addr === CRMD_X) -> CRMD,
            (addr === PRMD_X) -> PRMD,
            (addr === ESTAT_X) -> ESTAT,
            (addr === ERA_X) -> ERA,
            (addr === EENTRY_X) -> EENTRY,
            (addr === SAVE0_X) -> SAVE0,
            (addr === SAVE1_X) -> SAVE1,
            (addr === SAVE2_X) -> SAVE2,
            (addr === SAVE3_X) -> SAVE3,
        ))
    }
    val rvaddr = get_vaddr(io.csr.raddr)
    val wvaddr = get_vaddr(io.csr.waddr)
    io.csr.rdata := Mux(rvaddr === CSR_BADADDR, 0.U(WORD.W), rf.read(rvaddr))
    io.csr_taken := io.csr.Excp =/= 0.U(2.W)
    io.csr_target := Mux(io.csr.Excp === 1.U(2.W), rf.read(EENTRY), rf.read(ERA))

    when (io.csr.Excp === 1.U(EXCP_LEN.W)) {
        rf.write(CRMD, Cat(rf.read(CRMD)(31, 3), 0.U(3.W)))
        rf.write(PRMD, Cat(0.U(29.W), rf.read(CRMD)(2, 0)))
        rf.write(ERA, io.csr.pc)
        rf.write(ESTAT, Cat(0.U(1.W), io.csr.Esubcode, io.csr.Ecode, rf.read(ESTAT)(15, 0)))
    }.elsewhen (io.csr.Excp === 2.U(EXCP_LEN.W)) {
        when (rf.read(ESTAT)(21, 16) === "h3F".U(6.W)) {
            rf.write(CRMD, Cat(rf.read(CRMD)(31, 6), "b101".U(3.W), rf.read(PRMD)(2, 0)))
        }.otherwise {
            rf.write(CRMD, Cat(rf.read(CRMD)(31, 3), rf.read(PRMD)(2, 0)))
        }
    }.otherwise {
        val masked_data = (~io.csr.mask & rf.read(wvaddr)) | (io.csr.mask & io.csr.wdata)
        val final_data = Mux(io.csr.en_mask, masked_data, io.csr.wdata)
        val CRMD_BADADDR = Cat(0.U(23.W), "b0101".U(4.W), io.csr.wdata(4, 0))
        when (io.csr.we) {
            when (wvaddr === CSR_BADADDR) {
                when (io.csr.wdata(4) === 1.U(1.W)) {
                    rf.write(CRMD, CRMD_BADADDR)
                }.otherwise {
                    rf.write(wvaddr, 0.U(32.W))
                }
            }.otherwise {
                rf.write(wvaddr, final_data)
            }
        }
    }
}