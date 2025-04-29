package matmul

// Chisel
import chisel3._
import chisel3.util._

import matmul._
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
  val sys_clk_p     = IO(Input(Clock()))
  val sys_clk_n     = IO(Input(Clock()))
  val sys_rst_n     = IO(Input(Bool()))

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

  /* MODULES / WIRES */
  // AXI clock and reset
  val axi_aclk    = Wire(Clock())
  val axi_aresetn = Wire(Bool())

  // XDMA instance
  val xdma = Module(new xdma_0)

  // Kernel is clocked with AXI clock, which is not so good but help there's no
  // time
  val kern = withClockAndReset(axi_aclk, ~axi_aresetn){ Module(new MatMulWrapper(PARAM)) }

  // XDMA wiring
  // SYS
  xdma.io.sys_clk_gt  := sys_clk_gt
  xdma.io.sys_clk     := sys_clk
  xdma.io.sys_rst_n   := sys_rst_n_c
  // AXI clock / reset
  axi_aclk            := xdma.io.axi_aclk
  axi_aresetn         := xdma.io.axi_aresetn
  // PCIe
  xdma.io.pci_exp_rxn := pci_exp_rxn
  xdma.io.pci_exp_rxp := pci_exp_rxp
  pci_exp_txn         := xdma.io.pci_exp_txn
  pci_exp_txp         := xdma.io.pci_exp_txp
  // AXI
  xdma.io.m_axi       <> kern.s_axi
}
