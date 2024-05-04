package mycpu

import chisel3._
import chisel3.util._
import common._
import common.const._

class RegFile extends Module {
    val io = IO(new Bundle {
        val raddr1 = Input(UInt(REG.W))
        val rdata1 = Output(UInt(WORD.W))
        val raddr2 = Input(UInt(REG.W))
        val rdata2 = Output(UInt(WORD.W))
        val raddr3 = Input(UInt(REG.W))
        val rdata3 = Output(UInt(WORD.W))
        val we     = Input(UInt(RF_SEL_LEN.W))
        val waddr  = Input(UInt(REG.W))
        val wdata  = Input(UInt(WORD.W))
    })

  // 定义一个32x32位的寄存器文件
  val rf = Mem(32, UInt(WORD.W))
  val lst = RegInit(rf.read(io.waddr))
  val mask = Cat(Fill(8, io.we(3)), Fill(8, io.we(2)), Fill(8, io.we(1)), Fill(8, io.we(0)))
  // 写操作
  when(io.we =/= 0.U(RF_SEL_LEN.W)) {
    rf.write(io.waddr, (io.wdata & mask) | (lst & (~mask)))
  }

  // 读端口1
  // 如果读地址是0，则直接输出0，否则从寄存器文件读取数据
  io.rdata1 := Mux(io.raddr1 === 0.U, 0.U(WORD.W), rf.read(io.raddr1))

  // 读端口2
  // 如果读地址是0，则直接输出0，否则从寄存器文件读取数据
  io.rdata2 := Mux(io.raddr2 === 0.U, 0.U(WORD.W), rf.read(io.raddr2))
  io.rdata3 := Mux(io.raddr3 === 0.U, 0.U(WORD.W), rf.read(io.raddr3))
}
