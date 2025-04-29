package matmul.worker

import chisel3._
import chisel3.util._

import matmul.utils.Parameters

package object interfaces {
  class WorkerInterface(
    PARAM : Parameters
  ) extends Bundle {
    val data  = UInt(PARAM.SAF_WIDTH.W)
    val prog  = Bool()
    val work  = Bool()
  }
}
