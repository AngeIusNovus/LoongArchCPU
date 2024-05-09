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
  val opMul  = (io.aluOp === ALU_MUL)
  val opMulu = (io.aluOp === ALU_MULU)
  val opMulh = (io.aluOp === ALU_MULH)
  val opMulhu= (io.aluOp === ALU_MULHU)
  val opDiv  = (io.aluOp === ALU_DIV)
  val opDivu = (io.aluOp === ALU_DIVU)
  val opMod  = (io.aluOp === ALU_MOD)
  val opModu = (io.aluOp === ALU_MODU)

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

  val div = Module(new Div()).io
  val mul = Module(new Mul()).io

  val divReady = Wire(Bool())
  val divisorResult = Wire(UInt(LONG.W))
  val divResult = Wire(UInt(WORD.W))
  val modResult = Wire(UInt(WORD.W))
  div.start := opDiv || opDivu || opMod || opModu
  div.signed := opDiv || opMod
  div.src1 := io.aluSrc1
  div.src2 := io.aluSrc2
  div.allow_to_go := false.B
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
  mul.src1 := io.aluSrc1
  mul.src2 := io.aluSrc2
  mul.allow_to_go := false.B
  mulReady  := mul.ready
  mulResult := mul.result

  highResult := mulResult(LONG - 1, WORD)
  lowResult  := mulResult(WORD - 1, 0)

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
