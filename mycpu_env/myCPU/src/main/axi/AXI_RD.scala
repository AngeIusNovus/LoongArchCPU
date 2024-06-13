package mycpu

import chisel3._
import chisel3.util._

import common._
import common.const._

class AXI_RD extends Module {
    val io = IO(new Bundle{
        val inst = Flipped(new CPU_AXI_RD_IO())
        val data = Flipped(new CPU_AXI_RD_IO())
        val axi = new AXI_RD_IO()
    })
    val ar_sel_lock = RegInit(false.B)
    val ar_sel_val = RegInit(false.B)  // 0取指令，1取数据
    val ar_id = Mux(ar_sel_lock, ar_sel_val, io.data.arvalid)

    val inst = io.inst
    val data = io.data
    
    when (io.axi.arvalid) {
        when (io.axi.arready) {
            ar_sel_lock := false.B
        }.otherwise {
            ar_sel_lock := true.B
            ar_sel_val := ar_id
        }
    }

    val r_sel = io.axi.r.id(0)
    io.inst.r.valid := !r_sel && io.axi.r.valid
    io.inst.r.data  := io.axi.r.data
    io.inst.r.id := 0.U(4.W)
    io.data.r.valid := r_sel && io.axi.r.valid
    io.data.r.data  := io.axi.r.data
    io.data.r.id := 1.U(4.W)

    val idle :: wait_arready :: wait_rvalid :: Nil = Enum(3)

    val state = RegInit(idle)
    state := MuxLookup(state, idle)(List(
        idle         -> Mux(io.axi.arvalid, wait_arready, idle),
        wait_arready -> Mux(io.axi.arready, wait_rvalid, wait_arready),
        wait_rvalid  -> Mux(io.axi.r.valid, idle, wait_rvalid)
    ))

    io.axi.rready := (state === wait_rvalid)
    io.axi.ar := Mux(ar_id, io.data.ar, io.inst.ar)
    io.axi.arvalid := MuxCase(false.B, Seq(
        (state === idle) -> Mux(ar_id, data.arvalid, inst.arvalid),
        (state === wait_arready) -> true.B
    ))
}