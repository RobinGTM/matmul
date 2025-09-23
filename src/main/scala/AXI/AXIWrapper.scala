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

  // // Clock domain crossing interfaces
  // val ififo_xwcnt = IO(new MCPCrossSrc2DstInterface(UInt((IFIFO_CNT_W + 1).W)))
  // val ififo_xrcnt = IO(Flipped(new MCPCrossSrc2DstInterface(UInt((IFIFO_CNT_W + 1).W))))
  // // Write memory interface
  // val ififo_wmem  = IO(new BasicMemWriteInterface(IFIFO_CNT_W, UInt(32.W)))

  // // Output FIFO crossing / mem signals
  // val ofifo_xwcnt = IO(Flipped(new MCPCrossSrc2DstInterface(UInt((OFIFO_CNT_W + 1).W))))
  // val ofifo_xrcnt = IO(new MCPCrossSrc2DstInterface(UInt((OFIFO_CNT_W + 1).W)))
  // // Read memory interface
  // val ofifo_rmem  = IO(new BasicMemReadInterface(OFIFO_CNT_W, UInt(32.W)))

  // XPM FIFO ports
  val xpm_ififo_wr = IO(Flipped(new AsyncFIFOWriteInterface(UInt(32.W))))
  val xpm_ofifo_rd = IO(Flipped(new AsyncFIFOReadInterface(UInt(32.W))))

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
    // Input XPM FIFO
    iAxiMm2Fifo.fifo_wr <> xpm_ififo_wr
    // Output XPM FIFO
    oFifo2AxiMm.fifo_rd <> xpm_ofifo_rd

    // External data
    axiIf.s_axi <> s_axi

    // Control
    axiLiteSlave.s_axil       <> s_axil
    axiLiteSlave.wr_src_cross <> ctl_wr_xsrc
    axiLiteSlave.ar_src_cross <> ctl_ar_xsrc
    axiLiteSlave.rd_dst_cross <> ctl_rd_xdst

    // AXI-MM
    // Input
    iAxiMm2Fifo.s_axi_wr   <> axiIf.s_axi_wr
    // iAxiMm2Fifo.fifo_wr    <> iFifoWrPort.fifo_wr
    // iFifoWrPort.wcnt_cross <> ififo_xwcnt
    // iFifoWrPort.rcnt_cross <> ififo_xrcnt
    // iFifoWrPort.mem        <> ififo_wmem
    // Output
    oFifo2AxiMm.s_axi_rd   <> axiIf.s_axi_rd
    // oFifo2AxiMm.fifo_rd    <> oFifoRdPort.fifo_rd
    // oFifoRdPort.wcnt_cross <> ofifo_xwcnt
    // oFifoRdPort.rcnt_cross <> ofifo_xrcnt
    // oFifoRdPort.mem        <> ofifo_rmem
  }
}
