package mycpu

import chisel3._
import chisel3.util._
import common._
class MYCPU_TOP extends Module {
    val io = IO(new Bundle {
        val inst = new RAM_IO()
        val data = new RAM_IO()
        val debug = new DEBUG()
    })

    val IFU = Module(new IF_Stage())
    val IDU = Module(new ID_Stage())
    val EXE = Module(new EXE_Stage())
    val MEM = Module(new MEM_Stage())
    val WBU = Module(new WB_Stage())
    val regfile = Module(new RegFile())
    val csrfile = Module(new CsrFile())

    IFU.io.inst <> io.inst
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

    EXE.io.ms_allowin <> MEM.io.ms_allowin
    EXE.io.to_ms      <> MEM.io.to_ms
    EXE.io.data       <> io.data
    EXE.io.csr        <> csrfile.io.csr

    MEM.io.ws_allowin <> WBU.io.ws_allowin
    MEM.io.data_rdata <> io.data.rdata
    MEM.io.to_ws      <> WBU.io.to_ws

    WBU.io.debug      <> io.debug
    WBU.io.rf_we      <> regfile.io.we
    WBU.io.rf_waddr   <> regfile.io.waddr
    WBU.io.rf_wdata   <> regfile.io.wdata
}