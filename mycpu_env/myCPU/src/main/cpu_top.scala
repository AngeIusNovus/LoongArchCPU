package mycpu

import chisel3._
import chisel3.util._
import common._
class MYCPU_TOP extends Module {
    val io = IO(new Bundle {
        val axi = new AXI_IO()
        val debug = new DEBUG()
    })

    val IFU = Module(new IF_Stage())
    val IDU = Module(new ID_Stage())
    val EXE = Module(new EXE_Stage())
    val MEM = Module(new MEM_Stage())
    val WBU = Module(new WB_Stage())
    val regfile = Module(new RegFile())
    val csrfile = Module(new CsrFile())
    val axi_rd = Module(new AXI_RD())
    val axi_wr = Module(new AXI_WR())

    io.axi.rd <> axi_rd.io.axi
    io.axi.wr <> axi_wr.io.axi

    IFU.io.inst <> axi_rd.inst
    IFU.io.to_ds <> IDU.io.to_ds
    IFU.io.br <> IDU.io.br
    IFU.io.ds_allowin <> IDU.io.ds_allowin
    IFU.io.csr_taken  <> csrfile.io.csr_taken
    IFU.io.csr_target <> csrfile.io.csr_target
    IFU.io.ds_flush   <> IDU.io.ds_flush

    IDU.io.es_allowin <> EXE.io.es_allowin
    IDU.io.to_es <> EXE.io.to_es
    IDU.io.rj <> regfile.io.raddr1
    IDU.io.rk <> regfile.io.raddr2
    IDU.io.rd <> regfile.io.raddr3
    IDU.io.reg_rdata1 <> regfile.io.rdata1
    IDU.io.reg_rdata2 <> regfile.io.rdata2
    IDU.io.reg_rdata3 <> regfile.io.rdata3
    IDU.io.rd_es <> EXE.io.rd_es
    IDU.io.rd_ms <> MEM.io.rd_ms
    IDU.io.rd_ws <> WBU.io.rd_ws
    IDU.io.es_flush <> EXE.io.es_flush
    IDU.io.en_INT <> csrfile.io.en_INT
    IDU.io.EcounterL  <> csrfile.io.EcounterL
    IDU.io.EcounterH  <> csrfile.io.EcounterH
    IDU.io.EcounterID <> csrfile.io.EcounterID

    EXE.io.ms_allowin <> MEM.io.ms_allowin
    EXE.io.to_ms      <> MEM.io.to_ms
    EXE.io.data       <> axi_wr.io.req
    EXE.io.csr        <> csrfile.io.csr

    MEM.io.ws_allowin <> WBU.io.ws_allowin
    MEM.io.data       <> axi_rd.data
    MEM.io.to_ws      <> WBU.io.to_ws

    WBU.io.debug      <> io.debug
    WBU.io.rf_we      <> regfile.io.we
    WBU.io.rf_waddr   <> regfile.io.waddr
    WBU.io.rf_wdata   <> regfile.io.wdata
}