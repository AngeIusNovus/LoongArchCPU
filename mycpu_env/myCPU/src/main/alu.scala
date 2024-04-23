package mycpu_top

import chisel3._
import chisel3.util._

class ALU extends Module {
  val io = IO(new Bundle {
    val aluOp   = Input(UInt(12.W))
    val aluSrc1 = Input(UInt(32.W))
    val aluSrc2 = Input(UInt(32.W))
    val aluResult = Output(UInt(32.W))
  })

  // Control code decomposition
  val opAdd  = io.aluOp(0)
  val opSub  = io.aluOp(1)
  val opSlt  = io.aluOp(2)
  val opSltu = io.aluOp(3)
  val opAnd  = io.aluOp(4)
  val opNor  = io.aluOp(5)
  val opOr   = io.aluOp(6)
  val opXor  = io.aluOp(7)
  val opSll  = io.aluOp(8)
  val opSrl  = io.aluOp(9)
  val opSra  = io.aluOp(10)
  val opLui  = io.aluOp(11)

  // Adder/Subtractor logic
  val addSubResult = Wire(UInt(32.W))
  val adderResult = Mux(opSub || opSlt || opSltu, io.aluSrc1 - io.aluSrc2, io.aluSrc1 + io.aluSrc2)
  addSubResult := adderResult

  // Set less than (signed)
  val sltResult = Wire(UInt(32.W))
  sltResult := 0.U
  when(io.aluSrc1.asSInt < io.aluSrc2.asSInt) {
    sltResult := 1.U
  }

  // Set less than (unsigned)
  val sltuResult = Wire(UInt(32.W))
  sltuResult := 0.U
  when(io.aluSrc1.asUInt < io.aluSrc2.asUInt) {  // Assuming adderResult(31) is the carry out
    sltuResult := 1.U
  }

  // Bitwise operations
  val andResult = io.aluSrc1 & io.aluSrc2
  val norResult = ~(io.aluSrc1 | io.aluSrc2)
  val orResult = io.aluSrc1 | io.aluSrc2
  val xorResult = io.aluSrc1 ^ io.aluSrc2

  // Shift operations
  val sllResult = io.aluSrc1 << io.aluSrc2(4,0)
  val srlResult = io.aluSrc1 >> io.aluSrc2(4,0)
  val sraResult = (io.aluSrc1.asSInt >> io.aluSrc2(4,0)).asUInt

  // Load Upper Immediate
  val luiResult = io.aluSrc2

  // ALU result selection
  io.aluResult := MuxCase(0.U, Seq(
    opAdd.orR  -> addSubResult,
    opSub.orR  -> addSubResult,
    opSlt.orR  -> sltResult,
    opSltu.orR -> sltuResult,
    opAnd.orR  -> andResult,
    opNor.orR  -> norResult,
    opOr.orR   -> orResult,
    opXor.orR  -> xorResult,
    opSll.orR  -> sllResult,
    opSrl.orR  -> srlResult,
    opSra.orR  -> sraResult,
    opLui.orR  -> luiResult
  ))
}
