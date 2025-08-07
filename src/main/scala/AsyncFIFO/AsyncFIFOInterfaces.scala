/* AsyncFIFOInterfaces.scala -- Asynchronous FIFO interface
 *                              definitions
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
package asyncfifo

// Chisel
import chisel3._
import chisel3.util._

package object interfaces {
  // Async FIFO read port
  class AsyncFIFOReadInterface[T <: Data](
    dType : T = UInt(32.W)
  ) extends Bundle {
    val i_en     = Input(Bool())
    val o_nempty = Output(Bool())
    val o_data   = Output(dType)
  }

  // Async FIFO write port
  class AsyncFIFOWriteInterface[T <: Data](
    dType : T = UInt(32.W)
  ) extends Bundle {
    val i_we    = Input(Bool())
    val o_ready = Output(Bool())
    val i_data  = Input(dType)
  }

  // NB: Memory interfaces are defined from the FIFO counter logic's
  // point of view. So, flipped with respect to what a memory would
  // use
  // Basic memory read interface
  class BasicMemReadInterface[T <: Data](
    AW    : Int,
    dType : T = UInt(32.W)
  ) extends Bundle {
    val i_en   = Output(Bool())
    val i_addr = Output(UInt(AW.W))
    val o_data = Input(dType)
  }

  // Basic memory write interface
  class BasicMemWriteInterface[T <: Data](
    AW    : Int,
    dType : T = UInt(32.W)
  ) extends Bundle {
    val i_we   = Output(Bool())
    val i_addr = Output(UInt(AW.W))
    val i_data = Output(dType)
  }
}
