/* MultiCyclePathDst.scala -- MCP destination port
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
// Destination domain MCP
class MultiCyclePathDst[T <: Data](dType : T = UInt(32.W)) extends Module {
  val io_dst   = IO(new MultiCyclePathDstInterface(dType))
  val io_cross = IO(Flipped(new MCPCrossSrc2DstInterface(dType)))

  // FF1's hold / setup will be broken but that's ok, that's why FF2
  // is here
  val loadSyncFF1    = RegInit(false.B)
  val loadSyncFF2    = RegInit(false.B)
  loadSyncFF1       := io_cross.load
  loadSyncFF2       := loadSyncFF1
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
