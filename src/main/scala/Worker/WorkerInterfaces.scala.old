package matmul.worker

import chisel3._
import chisel3.util._

import matmul.utils.Parameters

package object interfaces {
  class WorkerInterface(
    PARAM : Parameters
  ) extends Bundle {
    val data  = UInt(PARAM.SAF_WIDTH.W)
    val work  = Bool()
  }

  class WorkerHardFloatInterface(
    PARAM : Parameters
  ) extends Bundle {
    val data = UInt((24 + 8 + 1).W)
    val work = Bool()
  }
}
