package mycpu

import chisel3._
import chisel3.util._
import common._
import common.const._

class ALU extends Module {
  val io = IO(new Bundle {
    val aluOp   = Input(UInt(OP_LEN.W))
    val aluSrc1 = Input(UInt(WORD.W))
    val aluSrc2 = Input(UInt(WORD.W))
    val aluResult = Output(UInt(WORD.W))
  })

  // Control code decomposition
  val opAdd  = (io.aluOp === 0.U(OP_LEN.W))
  val opSub  = (io.aluOp === 1.U(OP_LEN.W))
  val opSlt  = (io.aluOp === 2.U(OP_LEN.W))
  val opSltu = (io.aluOp === 3.U(OP_LEN.W))
  val opAnd  = (io.aluOp === 4.U(OP_LEN.W))
  val opNor  = (io.aluOp === 5.U(OP_LEN.W))
  val opOr   = (io.aluOp === 6.U(OP_LEN.W))
  val opXor  = (io.aluOp === 7.U(OP_LEN.W))
  val opSll  = (io.aluOp === 8.U(OP_LEN.W))
  val opSrl  = (io.aluOp === 9.U(OP_LEN.W))
  val opSra  = (io.aluOp === 10.U(OP_LEN.W))
  val opLui  = (io.aluOp === 11.U(OP_LEN.W))

  // Adder/Subtractor logic
  val addSubResult = Wire(UInt(WORD.W))
  val adderResult = Mux(opSub || opSlt || opSltu, io.aluSrc1 - io.aluSrc2, io.aluSrc1 + io.aluSrc2)
  addSubResult := adderResult

  // Set less than (signed)
  val sltResult = Wire(UInt(WORD.W))
  sltResult := 0.U
  when(io.aluSrc1.asSInt < io.aluSrc2.asSInt) {
    sltResult := 1.U
  }

  // Set less than (unsigned)
  val sltuResult = Wire(UInt(WORD.W))
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
