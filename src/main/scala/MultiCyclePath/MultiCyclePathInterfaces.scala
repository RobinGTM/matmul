/* MultiCyclePathInterfaces.scala -- MCP interface definitions
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
