/* AXIWrapper.scala -- High-level module wrapping all AXI buses needed
 *                     to interact with XDMA and exposing only
 *                     clock-domain-crossing and FIFO memory
 *                     interfaces
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
package axi

// Chisel
import chisel3._
import chisel3.util._

// Local
import mcp.interfaces._
import asyncfifo._
import asyncfifo.interfaces._
import adapters._
import axi.interfaces._
import matmul.utils._

class AXIWrapper(
  IFIFO_CNT_W : Int,
  OFIFO_CNT_W : Int,
  CTL_AW      : Int = 32,
  CTL_W       : Int = 32,
  AXI_AW      : Int = 64,
  AXI_W       : Int = 64,
  FIFO_TYPE   : String = "default"
) extends RawModule {
  /* I/O */
  val axi_aclk  = IO(Input(Clock()))
  val axi_arstn = IO(Input(Bool()))

  // AXI-Lite interface
  val s_axil = IO(new SlaveAXILiteInterface(CTL_AW, CTL_W))
  // AXI interface
  val s_axi  = IO(new SlaveAXIInterface(AXI_AW, AXI_W))

  // MCP clock-domain crossing interfaces
  val ctl_wr_xsrc = IO(new MCPCrossSrc2DstInterface(WrAddrData(CTL_AW, CTL_W)))
  val ctl_ar_xsrc = IO(new MCPCrossSrc2DstInterface(UInt(CTL_AW.W)))
  val ctl_rd_xdst = IO(Flipped(new MCPCrossSrc2DstInterface(UInt(CTL_W.W))))

  // FIFO interfaces
  val ififo_wr = IO(Flipped(new AsyncFIFOWriteInterface(UInt(32.W))))
  val ofifo_rd = IO(Flipped(new AsyncFIFOReadInterface(UInt(32.W))))

  withClockAndReset(axi_aclk, ~axi_arstn) {
    /* MODULES */
    val axiIf = Module(new SAXIRW2Full(AXI_AW, AXI_W))

    // AXI-Lite slave
    val axiLiteSlave = Module(new AXILiteSlaveMCPWrapper(
      CTL_AW,
      CTL_W,
      0x0,
      2 // 2 regs for M_HEIGHT and M_WIDTH
    ))

    // Input FIFO write port
    val iAxiMm2Fifo = Module(new AXIMemory2FIFO(AXI_AW, AXI_W))
    // val iFifoWrPort = Module(new AsyncFIFOWritePort(IFIFO_CNT_W, UInt(32.W)))

    // Output FIFO read port
    // val oFifoRdPort = Module(new AsyncFIFOReadPort(OFIFO_CNT_W, UInt(32.W)))
    val oFifo2AxiMm = Module(new FIFO2AXIMemory(AXI_AW, AXI_W))

    /* WIRING */
    // External data
    axiIf.s_axi <> s_axi

    // Control
    axiLiteSlave.s_axil       <> s_axil
    axiLiteSlave.wr_src_cross <> ctl_wr_xsrc
    axiLiteSlave.ar_src_cross <> ctl_ar_xsrc
    axiLiteSlave.rd_dst_cross <> ctl_rd_xdst

    // AXI-MM
    iAxiMm2Fifo.s_axi_wr <> axiIf.s_axi_wr
    oFifo2AxiMm.s_axi_rd <> axiIf.s_axi_rd

    // FIFO wiring
    iAxiMm2Fifo.fifo_wr <> ififo_wr
    ofifo_rd            <> oFifo2AxiMm.fifo_rd
  }
}
