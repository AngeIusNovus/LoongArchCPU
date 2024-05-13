package config

import chisel3._
import chisel3.util._

object CpuConfig {
    val cpuBuild : Boolean = false
    val cpuDivClkNum = 8.U
    val cpuMulClkNum = 2.U
}