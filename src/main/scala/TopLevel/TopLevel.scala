/* TopLevel.scala -- Top-level module
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

// Scala
import math.log

import matmul._
import axi._
import matmul.blackboxes._
import matmul.utils.Parameters
import asyncfifo._

// XDMA example-inspired top-level module
class TopLevel(
  PARAM : Parameters
) extends RawModule {
  /* I/O */
  // PCIe
  val pci_exp_txp = IO(Output(Bool()))
  val pci_exp_txn = IO(Output(Bool()))
  val pci_exp_rxp = IO(Input(Bool()))
  val pci_exp_rxn = IO(Input(Bool()))

  // Clock and reset
  val sys_clk_p = IO(Input(Clock()))
  val sys_clk_n = IO(Input(Clock()))
  val sys_rst_n = IO(Input(Bool()))

  // Core clock
  val coreclk_ref_p = IO(Input(Clock()))
  val coreclk_ref_n = IO(Input(Clock()))

  /* BUFFERS */
  val sysClkIBuf = Module(new IBUFDS_GTE4)
  val sysRstIBuf = Module(new IBUF)

  /* WIRING */
  // Input clk / rstn buffers (Xilinx)
  val sys_clk_gt     = Wire(Clock())
  val sys_clk        = Wire(Clock())
  sys_clk_gt        := sysClkIBuf.io.O
  sys_clk           := sysClkIBuf.io.ODIV2
  sysClkIBuf.io.CEB := 0.U
  sysClkIBuf.io.I   := sys_clk_p
  sysClkIBuf.io.IB  := sys_clk_n

  // System reset IBUF (Xilinx)
  val sys_rst_n_c  = Wire(Bool())
  sysRstIBuf.io.I := sys_rst_n
  sys_rst_n_c     := sysRstIBuf.io.O

  /* CORE CLOCK GENERATION */
  val coreClkIBufDS = Module(new IBUFDS)
  val coreClkPll    = Module(new PLLE4_BASE(PARAM.PLL_MULT, PARAM.PLL_DIV))
  val coreClkBufG   = Module(new BUFG)

  /* CORE CLOCK WIRING */
  // Reference clock (156.25MHz differential)
  val coreclk_ref = Wire(Clock())
  coreClkIBufDS.io.I   := coreclk_ref_p
  coreClkIBufDS.io.IB  := coreclk_ref_n
  coreclk_ref          := coreClkIBufDS.io.O
  // PLL
  val pll_locked         = Wire(Bool())
  val coreclk_locked     = Wire(Clock())
  coreClkPll.io.CLKIN   := coreclk_ref
  coreClkPll.io.CLKFBIN := coreClkPll.io.CLKFBOUT
  coreClkPll.io.RST     := sys_rst_n_c
  coreclk_locked        := coreClkPll.io.CLKOUT0
  pll_locked            := coreClkPll.io.LOCKED
  // BUFG
  val coreclk_bufed = Wire(Clock())
  coreClkBufG.io.I := coreclk_locked
  coreclk_bufed    := coreClkBufG.io.O

  /* MAIN MODULES */
  // AXI clock and reset
  val axi_aclk    = Wire(Clock())
  val axi_aresetn = Wire(Bool())

  // XDMA instance (black-box)
  val xdma = Module(new xdma)

  // AXI wrapper
  val axiW = Module(new AXIWrapper(
    PARAM.CTL_AW,
    PARAM.CTL_W,
    PARAM.AXI_AW,
    PARAM.AXI_W
  ))

  // MatMul core
  val core = Module(new CoreWrapper(PARAM))

  /* WIRING */
  // Coreclk wiring
  core.i_coreclk := coreclk_bufed
  core.i_arstn   := sys_rst_n_c

  // Control register clock-domain crossing
  core.ctl_wr_xdst <> axiW.ctl_wr_xsrc
  core.ctl_ar_xdst <> axiW.ctl_ar_xsrc
  core.ctl_rd_xsrc <> axiW.ctl_rd_xdst

  // XDMA wiring
  // SYS
  xdma.io.sys_clk_gt  := sys_clk_gt
  xdma.io.sys_clk     := sys_clk
  xdma.io.sys_rst_n   := sys_rst_n_c
  // AXI clock / reset
  axi_aclk            := xdma.io.axi_aclk
  axi_aresetn         := xdma.io.axi_aresetn
  // AXI wrapper
  axiW.axi_aclk       := axi_aclk
  axiW.axi_arstn      := axi_aresetn
  // Data buses
  xdma.io.m_axil      <> axiW.s_axil
  xdma.io.m_axi       <> axiW.s_axi
  // PCIe
  xdma.io.pci_exp_rxn := pci_exp_rxn
  xdma.io.pci_exp_rxp := pci_exp_rxp
  pci_exp_txn         := xdma.io.pci_exp_txn
  pci_exp_txp         := xdma.io.pci_exp_txp

  /* FIFOS */
  PARAM.FIFO_TYPE match {
    case "xpm" =>
      // XPM FIFOs
      val iFifo = Module(new xpm_fifo_async(
        FIFO_WRITE_DEPTH    = PARAM.IFIFO_DEPTH,
        FIFO_READ_LATENCY   = 1,
        RD_DATA_COUNT_WIDTH = PARAM.IFIFO_CNT_W + 1,
        READ_DATA_WIDTH     = 32, // binary32 float
        WR_DATA_COUNT_WIDTH = PARAM.IFIFO_CNT_W + 1,
        WRITE_DATA_WIDTH    = 32 // binary32 float
      ))
      val oFifo = Module(new xpm_fifo_async(
        FIFO_WRITE_DEPTH    = PARAM.OFIFO_DEPTH,
        FIFO_READ_LATENCY   = 1,
        RD_DATA_COUNT_WIDTH = PARAM.OFIFO_CNT_W + 1,
        READ_DATA_WIDTH     = 32, // binary32 float
        WR_DATA_COUNT_WIDTH = PARAM.OFIFO_CNT_W + 1,
        WRITE_DATA_WIDTH    = 32 // binary32 float
      ))

      // XPM FIFO wiring
      // Input FIFO is from AXIWrapper to CoreWrapper
      iFifo.io.rst           := ~sys_rst_n_c
      iFifo.io.sleep         := false.B
      iFifo.io.injectdbiterr := false.B
      iFifo.io.injectsbiterr := false.B
      oFifo.io.rst           := ~sys_rst_n_c
      oFifo.io.sleep         := false.B
      oFifo.io.injectdbiterr := false.B
      oFifo.io.injectsbiterr := false.B
      // Write port
      iFifo.io.wr_clk        := axi_aclk // Not a free-running clock but uuuuh
      iFifo.io.wr_en         := axiW.ififo_wr.i_we
      iFifo.io.din           := axiW.ififo_wr.i_data
      axiW.ififo_wr.o_ready  := ~iFifo.io.full
      // Read port
      iFifo.io.rd_clk        := coreclk_bufed
      iFifo.io.rd_en         := core.ififo_rd.i_en
      core.ififo_rd.o_data   := iFifo.io.dout
      core.ififo_rd.o_nempty := ~iFifo.io.empty
      // Ouptut FIFO is from CoreWrapper to AXIWrapper
      // Write port
      oFifo.io.wr_clk        := coreclk_bufed
      oFifo.io.wr_en         := core.ofifo_wr.i_we
      oFifo.io.din           := core.ofifo_wr.i_data
      core.ofifo_wr.o_ready  := ~oFifo.io.full
      // Read port
      oFifo.io.rd_clk        := axi_aclk
      oFifo.io.rd_en         := axiW.ofifo_rd.i_en
      axiW.ofifo_rd.o_data   := oFifo.io.dout
      axiW.ofifo_rd.o_nempty := ~oFifo.io.empty
    case "default" =>
      // FIFO memories
      val iFifoMem = SyncReadMem(PARAM.IFIFO_DEPTH, UInt(32.W))
      val oFifoMem = SyncReadMem(PARAM.OFIFO_DEPTH, UInt(32.W))

      // Input FIFO ports
      val iFifoWrPort = withClockAndReset(axi_aclk, ~axi_aresetn) {
        Module(new AsyncFIFOWritePort(PARAM.IFIFO_CNT_W, UInt(32.W)))
      }
      val iFifoRdPort = withClockAndReset(coreclk_bufed, ~sys_rst_n_c) {
        Module(new AsyncFIFOReadPort(PARAM.IFIFO_CNT_W, UInt(32.W)))
      }
      // Output FIFO ports
      val oFifoWrPort = withClockAndReset(coreclk_bufed, ~sys_rst_n_c) {
        Module(new AsyncFIFOWritePort(PARAM.OFIFO_CNT_W, UInt(32.W)))
      }
      val oFifoRdPort = withClockAndReset(axi_aclk, ~axi_aresetn) {
        Module(new AsyncFIFOReadPort(PARAM.OFIFO_CNT_W, UInt(32.W)))
      }

      // Cross-clock domain FIFO counter synchronization
      iFifoWrPort.wcnt_cross <> iFifoRdPort.wcnt_cross
      iFifoWrPort.rcnt_cross <> iFifoRdPort.rcnt_cross
      oFifoWrPort.wcnt_cross <> oFifoRdPort.wcnt_cross
      oFifoWrPort.rcnt_cross <> oFifoRdPort.rcnt_cross

      // AXIWrapper wiring
      axiW.ififo_wr <> iFifoWrPort.fifo_wr
      axiW.ofifo_rd <> oFifoRdPort.fifo_rd
      // CoreWrapper wiring
      core.ififo_rd <> iFifoRdPort.fifo_rd
      core.ofifo_wr <> oFifoWrPort.fifo_wr

      // Output FIFO
      // Write
      when(oFifoWrPort.mem.i_we) {
        oFifoMem.write(oFifoWrPort.mem.i_addr, oFifoWrPort.mem.i_data, coreclk_bufed)
      }
      // Read
      oFifoRdPort.mem.o_data := oFifoMem.read(
        oFifoRdPort.mem.i_addr,
        oFifoRdPort.mem.i_en,
        axi_aclk
      )
      // Input FIFO
      // Write
      when(iFifoWrPort.mem.i_we) {
        iFifoMem.write(iFifoWrPort.mem.i_addr, iFifoWrPort.mem.i_data, axi_aclk)
      }
      // Read
      iFifoRdPort.mem.o_data := iFifoMem.read(
        iFifoRdPort.mem.i_addr, iFifoRdPort.mem.i_en, coreclk_bufed
      )
    case _ =>
      require(PARAM.FIFO_TYPE == "xpm" || PARAM.FIFO_TYPE == "default",
        "[TopLevel] PARAM.FIFO_TYPE must be either 'xpm' or 'default'"
      )
  }
}
