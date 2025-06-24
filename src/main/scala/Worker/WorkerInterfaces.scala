package matmul.worker

import chisel3._
import chisel3.util._

package object interfaces {
  class WorkerInterface(
    DW : Int
  ) extends Bundle {
    // Data
    val data  = UInt(DW.W)
    // Data valid flag
    val valid = Bool()
    // Prog flag (data must go in worker memory)
    val prog  = Bool()
    // Write flag (worker is sending its own accumulator on the bus)
    val write = Bool()
  }

  // class WorkerOutput(
  //   DW : Int
  // ) extends Bundle {
  //   val data  = Output(UInt(DW.W))
  //   val valid = Output(Bool())
  //   val prog  = Output(Bool())
  //   val write = Output(Bool())
  // }
}
