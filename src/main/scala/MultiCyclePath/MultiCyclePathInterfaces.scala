package mcp

// Chisel
import chisel3._
import chisel3.util._

import matmul.utils.Parameters

package object interfaces {
  // Source interface: where the data comes from
  class MultiCyclePathSrcInterface[T <: Data](dType : T = UInt(32.W)) extends Bundle {
    val cross_pulse = Input(Bool())
    val data        = Input(dType)
    val ack         = Output(Bool())
  }

  // Destination interface: where the data goes
  class MultiCyclePathDstInterface[T <: Data](dType : T = UInt(32.W)) extends Bundle {
    val data       = Output(dType)
    val load_pulse = Output(Bool())
  }

  // Clock domain crossing interface
  class MCPCrossSrc2DstInterface[T <: Data](dType : T = UInt(32.W)) extends Bundle {
    val data = Output(dType)
    val load = Output(Bool())
    val ack  = Input(Bool())
  }

  // Only for testing
  class MultiCyclePathInterface[T <: Data](dType : T = UInt(32.W)) extends Bundle {
    // Source domain
    val src = new MultiCyclePathSrcInterface(dType)
    // Destination domain
    val dst = new MultiCyclePathDstInterface(dType)
  }
}
