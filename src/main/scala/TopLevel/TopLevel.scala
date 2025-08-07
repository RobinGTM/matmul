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

import matmul._
import axi._
import matmul.blackboxes._
import matmul.utils.Parameters

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
  val xdma = Module(new xdma_0)

  // AXI wrapper
  val axiW = Module(new AXIWrapper(
    PARAM.IFIFO_CNT_W,
    PARAM.OFIFO_CNT_W,
    PARAM.CTL_AW,
    PARAM.CTL_W,
    PARAM.AXI_AW,
    PARAM.AXI_W
  ))

  // MatMul core
  val core = Module(new CoreWrapper(PARAM))

  // FIFO memories
  // IFIFO is deeper because coefficients (WIDTH * HEIGHT values)
  // arrive there
  val iFifoMem = SyncReadMem(PARAM.IFIFO_DEPTH, UInt(32.W))
  val oFifoMem = SyncReadMem(PARAM.OFIFO_DEPTH, UInt(32.W))

  /* WIRING */
  // Coreclk wiring
  core.i_coreclk := coreclk_bufed
  core.i_arstn   := sys_rst_n_c

  // Control register clock-domain crossing
  core.ctl_wr_xdst <> axiW.ctl_wr_xsrc
  core.ctl_ar_xdst <> axiW.ctl_ar_xsrc
  core.ctl_rd_xsrc <> axiW.ctl_rd_xdst
  // FIFO clock-domain crossing counters
  core.ififo_xwcnt <> axiW.ififo_xwcnt
  core.ififo_xrcnt <> axiW.ififo_xrcnt
  core.ofifo_xrcnt <> axiW.ofifo_xrcnt
  core.ofifo_xwcnt <> axiW.ofifo_xwcnt

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

  /* FIFO MEMORY LOGIC */
  // Output FIFO
  // Write
  when(core.ofifo_wmem.i_we) {
    oFifoMem.write(core.ofifo_wmem.i_addr, core.ofifo_wmem.i_data, coreclk_bufed)
  }
  // Read
  axiW.ofifo_rmem.o_data := oFifoMem.read(
    axiW.ofifo_rmem.i_addr,
    axiW.ofifo_rmem.i_en,
    axi_aclk
  )
  // Input FIFO
  // Write
  when(axiW.ififo_wmem.i_we) {
    iFifoMem.write(axiW.ififo_wmem.i_addr, axiW.ififo_wmem.i_data, axi_aclk)
  }
  // Read
  core.ififo_rmem.o_data := iFifoMem.read(
    core.ififo_rmem.i_addr, core.ififo_rmem.i_en, coreclk_bufed
  )
}
