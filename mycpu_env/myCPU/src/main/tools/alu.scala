package mycpu

import chisel3._
import chisel3.util._
import common._
import common.const._
//import config._
import config.CpuConfig._

class ALU extends Module {
  val io = IO(new Bundle {
    val aluOp   = Input(UInt(OP_LEN.W))
    val aluSrc1 = Input(UInt(WORD.W))
    val aluSrc2 = Input(UInt(WORD.W))
    val aluResult = Output(UInt(WORD.W))
    val ready   = Output(Bool())
  })

  val aluOp = io.aluOp
  val aluSrc1 = io.aluSrc1
  val aluSrc2 = io.aluSrc2

  // Control code decomposition
  val opAdd  = (aluOp === ALU_ADD)
  val opSub  = (aluOp === ALU_SUB)
  val opSlt  = (aluOp === ALU_SLT)
  val opSltu = (aluOp === ALU_SLTU)
  val opAnd  = (aluOp === ALU_AND)
  val opNor  = (aluOp === ALU_NOR)
  val opOr   = (aluOp === ALU_OR)
  val opXor  = (aluOp === ALU_XOR)
  val opSll  = (aluOp === ALU_SLL)
  val opSrl  = (aluOp === ALU_SRL)
  val opSra  = (aluOp === ALU_SRA)
  val opLui  = (aluOp === ALU_LU12I)
  val opMul  = (aluOp === ALU_MUL)
  val opMulu = (aluOp === ALU_MULU)
  val opMulh = (aluOp === ALU_MULH)
  val opMulhu= (aluOp === ALU_MULHU)
  val opDiv  = (aluOp === ALU_DIV)
  val opDivu = (aluOp === ALU_DIVU)
  val opMod  = (aluOp === ALU_MOD)
  val opModu = (aluOp === ALU_MODU)

  // Adder/Subtractor logic
  val newAluSrc2 = Wire(UInt(WORD.W))
  val addSubResult = Wire(UInt(WORD.W))
  val adderResult = Wire(UInt(WORD.W))

  newAluSrc2 := Mux(opSub || opSlt || opSltu, (~aluSrc2) + 1.U(WORD.W), aluSrc2)
  adderResult := aluSrc1 + newAluSrc2
  addSubResult := adderResult

  // Set less than (signed)
  val sltResult = Wire(UInt(WORD.W))
  sltResult := 0.U
  when(aluSrc1.asSInt < aluSrc2.asSInt) {
    sltResult := 1.U
  }

  // Set less than (unsigned)
  val sltuResult = Wire(UInt(WORD.W))
  sltuResult := 0.U
  when(aluSrc1.asUInt < aluSrc2.asUInt) {  // Assuming adderResult(31) is the carry out
    sltuResult := 1.U
  }

  // Bitwise operations
  val andResult = aluSrc1 & aluSrc2
  val norResult = ~(aluSrc1 | aluSrc2)
  val orResult = aluSrc1 | aluSrc2
  val xorResult = aluSrc1 ^ aluSrc2

  // Shift operations
  val sllResult = aluSrc1 << aluSrc2(4,0)
  val srlResult = aluSrc1 >> aluSrc2(4,0)
  val sraResult = (aluSrc1.asSInt >> aluSrc2(4,0)).asUInt

  // Load Upper Immediate
  val luiResult = io.aluSrc2

  val div = Module(new Div()).io
  val mul = Module(new Mul()).io

  val divReady = Wire(Bool())
  val divisorResult = Wire(UInt(LONG.W))
  val divResult = Wire(UInt(WORD.W))
  val modResult = Wire(UInt(WORD.W))
  div.start := opDiv || opDivu || opMod || opModu
  div.signed := opDiv || opMod
  div.src1 := aluSrc1
  div.src2 := aluSrc2
  div.allow_to_go := true.B
  divReady := div.ready
  divisorResult := div.result
  modResult := divisorResult(LONG - 1, WORD)
  divResult := divisorResult(WORD - 1, 0)

  val mulReady = Wire(Bool())
  val mulResult = Wire(UInt(LONG.W))
  val highResult = Wire(UInt(WORD.W))
  val lowResult = Wire(UInt(WORD.W))
  mul.start := opMul || opMulh || opMulu || opMulhu
  mul.signed := opMul || opMulh
  mul.src1 := aluSrc1
  mul.src2 := aluSrc2
  mul.allow_to_go := true.B
  mulReady  := mul.ready
  mulResult := mul.result

  highResult := mulResult(LONG - 1, WORD)
  lowResult  := mulResult(WORD - 1, 0)

  io.ready := MuxCase(true.B, Seq(
    (opMul || opMulh || opMulu || opMulhu) -> mulReady,
    (opDiv || opDivu || opMod || opModu)   -> divReady
  ))

  // ALU result selection
  io.aluResult := MuxCase(0.U, Seq(
    opAdd  -> addSubResult,
    opSub  -> addSubResult,
    opSlt  -> sltResult,
    opSltu -> sltuResult,
    opAnd  -> andResult,
    opNor  -> norResult,
    opOr   -> orResult,
    opXor  -> xorResult,
    opSll  -> sllResult,
    opSrl  -> srlResult,
    opSra  -> sraResult,
    opLui  -> luiResult,
    (opMul || opMulu) -> lowResult,
    (opMulh || opMulhu) -> highResult,
    (opDiv || opDivu) -> divResult,
    (opMod || opModu) -> modResult
  ))
}
