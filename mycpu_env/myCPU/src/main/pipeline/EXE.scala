package mycpu

import chisel3._
import chisel3.util._
import common._
import common.const._

class EXE_Stage extends Module {
    val io = IO(new Bundle{
        val to_es = Flipped(new EX_BUS())
        val to_ms = new ME_BUS()
        val es_allowin = Output(Bool())
        val ms_allowin = Input(Bool())
        val rd_es = new TMP_REG()
        val csr = new CSR_IO()
        val es_flush = Output(Bool())
        val data = new AXI_WR_IO()
    })

    val es_valid = RegInit(false.B)
    val es_ready_go = Wire(Bool())

    io.es_allowin := (!es_valid) | (io.ms_allowin & es_ready_go)
    when (io.es_allowin) {
        es_valid := io.to_es.valid
    }
    io.to_ms.valid := es_valid && es_ready_go && !io.es_flush 

    val es = RegEnable(io.to_es, io.es_allowin && io.to_es.valid)
    val csr = RegEnable(io.to_es.csr, io.es_allowin && io.to_es.valid)

    val alu_result = Wire(UInt(WORD.W))

    val u_alu = Module(new ALU())
    u_alu.io.aluOp   := Mux(csr.Excp === 0.U(EXCP_LEN.W), es.alu_op, ALU_X)
    u_alu.io.aluSrc1 := es.src1_data
    u_alu.io.aluSrc2 := es.src2_data
    alu_result       := u_alu.io.aluResult
    es_ready_go      := u_alu.io.ready
    
    io.to_ms.wb_src   := es.wb_src
    io.to_ms.rf_we    := es.rf_we
    io.to_ms.rd_value := es.rd_value
    io.to_ms.dest     := es.dest
    io.to_ms.alu_res  := alu_result
    io.to_ms.pc       := es.pc
    io.to_ms.mem_re   := es.mem_re
    io.to_ms.mem_we   := es.mem_we
    io.to_ms.addr     := alu_result
    io.to_ms.csr_data := io.csr.rdata

    val ALE = ((es.mem_re === MEM_RS) && (alu_result(1, 0) =/= 0.U(2.W))) ||
              (((es.mem_re === MEM_RH) || (es.mem_re === MEM_RHU)) && (alu_result(0) =/= 0.U(1.W))) ||
              ((es.mem_we === MEM_WS) && (alu_result(1, 0) =/= 0.U(2.W))) ||
              ((es.mem_we === MEM_WH) && (alu_result(0) =/= 0.U(1.W)))

    io.es_flush     := es_valid && (io.csr.Excp =/= 0.U(EXCP_LEN.W))
    io.csr.Excp     := Mux(csr.Excp === 0.U(EXCP_LEN.W), Cat(0.U(1.W), ALE), 
                                                         csr.Excp) & Fill(2, es_valid.asUInt)
    io.csr.Ecode    := Mux(ALE && (csr.Ecode === Ecode.NONE), Ecode.ALE, csr.Ecode)
    io.csr.Esubcode := csr.Esubcode
    io.csr.badv     := csr.badv || ALE
    io.csr.badvaddr := Mux((io.csr.Ecode === Ecode.ALE), alu_result, csr.badvaddr)
    io.csr.pc       := csr.pc
    io.csr.en_mask  := csr.en_mask
    io.csr.we       := csr.we
    io.csr.waddr    := csr.waddr
    io.csr.wdata    := csr.wdata
    io.csr.mask     := csr.mask
    io.csr.raddr    := csr.raddr

    val final_we = Wire(UInt(BYTE_LEN.W))
    val off = alu_result(1, 0)

    final_we := MuxCase(es.mem_we, Seq(
        (off === 1.U(2.W)) -> Cat(es.mem_we(2, 0), es.mem_we(3)),
        (off === 2.U(2.W)) -> Cat(es.mem_we(1, 0), es.mem_we(3, 2)),
        (off === 3.U(2.W)) -> Cat(es.mem_we(0), es.mem_we(3, 1))
    ))

    io.data.aw.id := 1.U(AXI_ID.W)
    io.data.aw.addr := alu_result
    io.data.aw.len := 0.U(AXI_LEN.W)
    io.data.aw.size := MuxCase(4.U(AXI_SIZE.W), Seq(
        (es.mem_we === MEM_WB) -> 1.U(AXI_SIZE.W),
        (es.mem_we === MEM_WH) -> 2.U(AXI_SIZE.W)
    ))
    io.data.aw.burst := 1.U(AXI_BURST.W)
    io.data.aw.lock := 0.U(AXI_LOCK.W)
    io.data.aw.cache := 0.U(AXI_CACHE.W)
    io.data.aw.prot := 0.U(AXI_PROT.W)
    io.data.awvalid := !(!es_valid || (csr.Excp =/= 0.U(EXCP_LEN.W))) && (es.mem_we =/= MEM_WX)
    io.data.w.id := 1.U(AXI_ID.W)
    io.data.w.data := MuxCase(es.rd_value, Seq(
        (es.mem_we === MEM_WB) -> Fill(4, es.rd_value(7, 0)),
        (es.mem_we === MEM_WH) -> Fill(2, es.rd_value(15, 0))
    ))
    io.data.w.strb := final_we
    io.data.w.last := true.B
    io.data.wvalid := !(!es_valid || (csr.Excp =/= 0.U(EXCP_LEN.W))) && (es.mem_we =/= MEM_WX)

    io.rd_es.valid := (es.rf_we =/= 0.U(REG.W)) && es_valid && (es.dest =/= 0.U(REG.W))
    io.rd_es.ready := es.wb_src =/= WB_MEM
    io.rd_es.dest  := es.dest
    io.rd_es.data  := MuxCase(io.csr.rdata, Seq(
        (es.wb_src === WB_ALU) -> alu_result,
        (es.wb_src === WB_PC)  -> (es.pc + 4.U(WORD.W)),
        (es.wb_src === WB_X)   -> (0.U(WORD.W))
    ))
}