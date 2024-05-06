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
  val opAdd  = (io.aluOp === ALU_ADD)
  val opSub  = (io.aluOp === ALU_SUB)
  val opSlt  = (io.aluOp === ALU_SLT)
  val opSltu = (io.aluOp === ALU_SLTU)
  val opAnd  = (io.aluOp === ALU_AND)
  val opNor  = (io.aluOp === ALU_NOR)
  val opOr   = (io.aluOp === ALU_OR)
  val opXor  = (io.aluOp === ALU_XOR)
  val opSll  = (io.aluOp === ALU_SLL)
  val opSrl  = (io.aluOp === ALU_SRL)
  val opSra  = (io.aluOp === ALU_SRA)
  val opLui  = (io.aluOp === ALU_LU12I)

  // Adder/Subtractor logic
  val aluSrc2 = Wire(UInt(WORD.W))
  val addSubResult = Wire(UInt(WORD.W))
  val adderResult = Wire(UInt(WORD.W))

  aluSrc2 := Mux(opSub || opSlt || opSltu, (~io.aluSrc2) + 1.U(WORD.W), io.aluSrc2)
  adderResult := io.aluSrc1 + aluSrc2
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
