package mycpu

import chisel3._
import chisel3.util._

import common._
import common.const._

class AXI_WR extends Module {
    val io = IO(new Bundle{
        val req = Flipped(new AXI_WR_IO())
        val axi = new AXI_WR_IO()
    })

    io.axi.aw := io.req.aw
    io.axi.w  := io.req.w
    io.req.b.valid  := io.axi.b.valid

    val idle :: wait_ready :: wait_wready :: wait_awready :: wait_bvalid :: Nil = Enum(5)
    val state = RegInit(idle)

    io.axi.awvalid := MuxCase(false.B, Seq(
        (state === idle) -> io.req.awvalid,
        (state === wait_ready || state === wait_awready) -> true.B
    ))
    io.axi.wvalid := MuxCase(false.B, Seq(
        (state === idle) -> io.req.wvalid,
        (state === wait_ready || state === wait_wready) -> true.B
    ))

    state := MuxLookup(state, idle)(List(
        idle -> Mux(io.axi.awvalid, wait_ready, idle),
        wait_ready -> MuxCase(wait_ready, Seq(
            (io.axi.awready && io.axi.wready) -> wait_bvalid,
            io.axi.awready -> wait_wready,
            io.axi.wready  -> wait_awready
        )),
        wait_wready  -> Mux(io.axi.wready, wait_bvalid, wait_wready),
        wait_awready -> Mux(io.axi.awready, wait_bvalid, wait_awready),
        wait_bvalid  -> Mux(io.axi.b.valid, idle, wait_bvalid)
    ))
    io.axi.b.ready := (state === wait_bvalid)
}