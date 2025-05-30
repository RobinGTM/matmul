package mcp

import chisel3._
import chisel3.util._

// Local
import mcp.interfaces._

// http://www.sunburst-design.com/papers/CummingsSNUG2008Boston_CDC.pdf
// Source domain MCP
// io.src.cross_pulse should be a 1-clock pulse form the source domain
// that indicates data must be transferred
class MultiCyclePathSrc[T <: Data](dType : T = UInt(32.W)) extends Module {
  val io_src   = IO(new MultiCyclePathSrcInterface(dType))
  val io_cross = IO(new MCPCrossSrc2DstInterface(dType))

  // Input register
  val dataInReg = RegInit(0.U.asTypeOf(io_src.data))
  // Load on i_cross_pulse enable
  when(io_src.cross_pulse) {
    dataInReg := io_src.data
  }
  // Toggle load crossing wire: when i_cross_pulse pulses for 1
  // clock in domain A, this register toggles, which will cause a
  // load pulse in domain B.
  val loadToggleReg = RegInit(false.B)
  loadToggleReg    := io_src.cross_pulse ^ loadToggleReg
  // Clock-domain crossing data bridge (multi-cycle path)
  io_cross.data    := dataInReg
  // Clock-domain crossing load toggle wire
  io_cross.load    := loadToggleReg

  // Ack circuitry: receives domain B feedback and generates a pulse
  // Synchronizing flip-flops to prevent metastability
  // FF1 will have its hold / setup constraints broken
  val ackSyncFF1  = RegNext(io_cross.ack)
  val ackSyncFF2  = RegNext(ackSyncFF1)
  val ackPulseReg = RegInit(false.B)
  // Generate ack pulse
  ackPulseReg    := ackSyncFF2
  // Output ack pulse
  io_src.ack     := ackPulseReg ^ ackSyncFF2
}
