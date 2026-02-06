/* MultiCyclePathSrc.scala -- MCP source port
 *
 * (C) Copyright 2025 Robin Gay <robin.gay@polymtl.ca>
 *
 * This file is part of matmul.
 *
 * matmul is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * matmul is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with matmul. If not, see <https://www.gnu.org/licenses/>.
 */
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
  val ackSyncFF1  = RegInit(false.B)
  val ackSyncFF2  = RegInit(false.B)
  ackSyncFF1     := io_cross.ack
  ackSyncFF2     := ackSyncFF1
  val ackPulseReg = RegInit(false.B)
  // Generate ack pulse
  ackPulseReg    := ackSyncFF2
  // Output ack pulse
  io_src.ack     := ackPulseReg ^ ackSyncFF2
}
