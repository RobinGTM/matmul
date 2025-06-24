package matmul

// Chisel
import chisel3._
import chisel3.util._

import matmul._
import axi._
import matmul.blackboxes._
import matmul.utils.Parameters

class Top(
  PARAM : Parameters
) extends RawModule {
  /* I/O */
  // PCIe
  val pci_exp_txp = IO(Output(Bool()))
  val pci_exp_txn = IO(Output(Bool()))
  val pci_exp_rxp = IO(Input(Bool()))
  val pci_exp_rxn = IO(Input(Bool()))

  // Clock and reset
  val sys_clk_p   = IO(Input(Clock()))
  val sys_clk_n   = IO(Input(Clock()))
  val sys_rst_n   = IO(Input(Bool()))

  // Core clock
  val coreclk_p   = IO(Input(Clock()))
  val coreclk_n   = IO(Input(Clock()))

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
  val coreClkPll    = Module(new PLLE4_BASE)
  val coreClkBufG   = Module(new BUFG)

  /* CORE CLOCK WIRING */
  // Reference clock (156.25MHz differential)
  val coreclk_ref = Wire(Clock())
  coreClkIBufDS.io.I   := coreclk_p
  coreClkIBufDS.io.IB  := coreclk_n
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

  // XDMA instance
  val xdma = Module(new xdma_0)

  // AXI wrapper
  val axiW = Module(new AXIWrapper(PARAM))

  // MatMul kernel
  val kern = Module(new MatMulWrapper(PARAM))
  // val kern = withClockAndReset(axi_aclk, ~axi_aresetn) {
  //   Module(new MatMulHardFloatWrapper(PARAM))
  // }

  // FIFO memories
  val iFifoMem = SyncReadMem(PARAM.FIFO_DEPTH, UInt(32.W))
  val oFifoMem = SyncReadMem(PARAM.FIFO_DEPTH, UInt(32.W))

  /* WIRING */
  // Kernel wiring
  kern.i_coreclk := coreclk_bufed
  kern.i_arstn   := sys_rst_n_c

  // Clock domain crossing
  kern.ififo_xwcnt <> axiW.ififo_xwcnt
  kern.ififo_xrcnt <> axiW.ififo_xrcnt
  kern.ofifo_xrcnt <> axiW.ofifo_xrcnt
  kern.ofifo_xwcnt <> axiW.ofifo_xwcnt

  // XDMA wiring
  // SYS
  xdma.io.sys_clk_gt  := sys_clk_gt
  xdma.io.sys_clk     := sys_clk
  xdma.io.sys_rst_n   := sys_rst_n_c
  xdma.io.m_axi       <> axiW.s_axi
  // AXI clock / reset
  axi_aclk            := xdma.io.axi_aclk
  axi_aresetn         := xdma.io.axi_aresetn
  // AXI wrapper
  axiW.axi_aclk       := axi_aclk
  axiW.axi_arstn      := axi_aresetn
  // PCIe
  xdma.io.pci_exp_rxn := pci_exp_rxn
  xdma.io.pci_exp_rxp := pci_exp_rxp
  pci_exp_txn         := xdma.io.pci_exp_txn
  pci_exp_txp         := xdma.io.pci_exp_txp

  /* FIFO MEMORY LOGIC */
  // Output FIFO
  // Write
  when(kern.ofifo_wmem.i_we) {
    oFifoMem.write(kern.ofifo_wmem.i_addr, kern.ofifo_wmem.i_data, coreclk_bufed)
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
  kern.ififo_rmem.o_data := iFifoMem.read(
    kern.ififo_rmem.i_addr, kern.ififo_rmem.i_en, coreclk_bufed
  )
}
