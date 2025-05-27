package mcp

import chisel3._
import chisel3.util._

// Local
import mcp.interfaces.MultiCyclePathInterface

// Only for testing
class MultiCyclePath[T <: Data](dType: T = UInt(32.W)) extends RawModule {
  // MCP I/O
  val i_clka = IO(Input(Clock()))
  val i_rsta = IO(Input(Bool()))
  val i_clkb = IO(Input(Clock()))
  val i_rstb = IO(Input(Bool()))
  val io = IO(new MultiCyclePathInterface(dType))

  // Source domain
  val mcpSrc = withClockAndReset(i_clka, i_rsta) { Module(new MultiCyclePathSrc) }
  val mcpDst = withClockAndReset(i_clkb, i_rstb) { Module(new MultiCyclePathDst) }

  mcpSrc.io_cross <> mcpDst.io_cross
  mcpSrc.io_src   <> io.src
  io.dst          <> mcpDst.io_dst
}
