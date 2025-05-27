package mcp

import chisel3._
import chisel3.util._

// Local
import mcp.interfaces._

// http://www.sunburst-design.com/papers/CummingsSNUG2008Boston_CDC.pdf
// Destination domain MCP
class MultiCyclePathDst[T <: Data](dType : T = UInt(32.W)) extends Module {
  val io_dst   = IO(new MultiCyclePathDstInterface(dType))
  val io_cross = IO(Flipped(new MCPCrossSrc2DstInterface(dType)))

  // FF1's hold / setup will be broken but that's ok, that's why FF2
  // is here
  val loadSyncFF1    = RegNext(io_cross.load)
  val loadSyncFF2    = RegNext(loadSyncFF1)
  val loadPulseReg   = RegInit(false.B)
  loadPulseReg      := loadSyncFF2
  // Send load toggle state back to domain A to generate ack
  io_cross.ack      := loadSyncFF2
  // Output load pulse
  io_dst.load_pulse := loadPulseReg ^ loadSyncFF2
  // Directly output data bridge. Will be metastable but that's okay
  // as long as data is only loaded when o_load_pulse = 1
  io_dst.data       := io_cross.data
}
