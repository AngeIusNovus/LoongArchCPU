package mycpu

import chisel3._
import chisel3.util._
import common._
import common.const._

// 参数化的解码器模块
class Decoder[S <: Data, T <: Data](inWidth: Int, outWidth: Int) extends Module {
    val io = IO(new Bundle {
        val in  = Input(UInt(inWidth.W))
        val out = Output(Vec(outWidth, Bool()))
    })

  // 生成解码逻辑
  io.out := VecInit(Seq.tabulate(outWidth)(i => io.in === i.U))
}

// 实例化特定的解码器
class Decoder_2_4  extends Decoder(2, 4)
class Decoder_4_16 extends Decoder(4, 16)
class Decoder_5_32 extends Decoder(5, 32)
class Decoder_6_64 extends Decoder(6, 64)