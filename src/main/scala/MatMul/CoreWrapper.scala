/* CoreWrapper.scala -- High-level module that wraps all core modules
 *                      and exposes only clock-domain crossing and
 *                      FIFO memory interfaces
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

import adapters._
import asyncfifo._
import asyncfifo.interfaces._
import mcp._
import mcp.interfaces._
import saf._
import axi._
import axi.interfaces._
import matmul._
import matmul.utils._

class CoreWrapper(
  PARAM : Parameters
) extends RawModule {
  /* I/O */
  // Clk / rst
  val i_coreclk = IO(Input(Clock()))
  // Async reset (synchronized in the core)
  val i_arstn   = IO(Input(Bool()))

  // Control clock-domain crossing interfaces
  // Write address + data
  val ctl_wr_xdst = IO(Flipped(new MCPCrossSrc2DstInterface(WrAddrData(
    PARAM.CTL_AW, PARAM.CTL_W
  ))))
  // Read address
  val ctl_ar_xdst = IO(Flipped(new MCPCrossSrc2DstInterface(UInt(PARAM.CTL_AW.W))))
  // Read data
  val ctl_rd_xsrc = IO(new MCPCrossSrc2DstInterface(UInt(PARAM.CTL_W.W)))

  // FIFO interfaces
  val ififo_rd = IO(Flipped(new AsyncFIFOReadInterface(UInt(32.W))))
  val ofifo_wr = IO(Flipped(new AsyncFIFOWriteInterface(UInt(32.W))))

  /* RESET SYNCHRONIZER */
  // Reset is synchronized since it comes from a different clock domain
  val sync_rstn = withClock(i_coreclk) { RegNext(RegNext(i_arstn)) }

  // Reset is active low
  withClockAndReset(i_coreclk, ~sync_rstn) {
    /* MODULES */
    // MCP adapter for control register
    val mcpAdapter  = Module(new MCPCross2RegAdapter(PARAM.CTL_AW, PARAM.CTL_W))
    // Input FIFO to AXI-Stream adapter
    val iFifo2AxiS  = Module(new FIFO2AXIS(32))
    // Matrix multiplier core and controller
    val core        = Module(new MatMul(PARAM))
    // Output AXI-Stream to FIFO adapter
    val oAxiS2Fifo  = Module(new AXIS2FIFO(32))

    /* WIRING */
    // Clock-domain crossing
    mcpAdapter.wr_dst_cross <> ctl_wr_xdst
    mcpAdapter.ar_dst_cross <> ctl_ar_xdst
    mcpAdapter.rd_src_cross <> ctl_rd_xsrc
    // Simple register interface
    core.ctl_reg            <> mcpAdapter.io_reg

    // Input AXI-Stream goes to core input
    core.s_axis             <> iFifo2AxiS.m_axis
    // Core output AXI-Stream goes to AXI-Stream output
    core.m_axis             <> oAxiS2Fifo.s_axis

    // FIFO to / from adapters
    ififo_rd                <> iFifo2AxiS.fifo_rd
    oAxiS2Fifo.fifo_wr      <> ofifo_wr
  }
}
