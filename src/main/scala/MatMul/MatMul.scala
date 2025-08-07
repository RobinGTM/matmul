/* MatMul.scala -- Core module, wrapping the PEs and the controller
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
package matmul

// Chisel
import chisel3._
import chisel3.util._

// Local
import mcp.interfaces
import asyncfifo._
import asyncfifo.interfaces._
import axi.interfaces._
import matmul.utils._

class MatMul(
  PARAM : Parameters
) extends Module {
  val ctl_reg = IO(new BasicRegInterface(PARAM.CTL_AW, PARAM.CTL_W))
  // AXI-Stream from FIFO
  // MatMul controller receives IEEE754 float32 data
  val s_axis = IO(new SlaveAXIStreamInterfaceNoTid(32))
  // From MatMul output to FIFO
  val m_axis = IO(new MasterAXIStreamInterfaceNoTid(32))

  val matmulCore = Module(new MatMulCore(PARAM))
  val matmulCtl  = Module(new MatMulController(PARAM))

  // Data and control paths go to controller
  matmulCtl.ctl_reg <> ctl_reg
  matmulCtl.s_axis  <> s_axis
  matmulCtl.m_axis  <> m_axis

  // Internal datapaths go to core
  matmulCtl.to_matmul <> matmulCore.i
  matmulCore.o        <> matmulCtl.from_matmul
}
