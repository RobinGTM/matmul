/* AXIS2FIFO.scala -- AXI-Stream to FIFO write port adapter
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
package adapters

// Chisel
import chisel3._
import chisel3.util._

import math.pow

// Local
import axi.interfaces._
import asyncfifo.interfaces._

class AXIS2FIFO(
  AXIS_W : Int
) extends Module {
  // AXI-Stream slave
  val s_axis  = IO(new SlaveAXIStreamInterfaceNoTid(AXIS_W))
  // FIFO write interface
  val fifo_wr = IO(Flipped(new AsyncFIFOWriteInterface(UInt(AXIS_W.W))))

  // AXI-S tkeep is ignored
  fifo_wr.i_we   := s_axis.tvalid & s_axis.tready
  fifo_wr.i_data := s_axis.tdata
  s_axis.tready  := fifo_wr.o_ready

  // tlast and tkeep are ignored
}
