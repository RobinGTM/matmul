package matmul.blackboxes

// Chisel
import chisel3._
import chisel3.util._
import chisel3.experimental._

// Local
import axi.interfaces._

class xdma_0 extends BlackBox {
  val io = IO(new Bundle {
    // System clk / rstn
    val sys_clk        = Input(Clock())
    val sys_clk_gt     = Input(Clock())
    val sys_rst_n      = Input(Bool())

    // PCIe interface
    val pci_exp_rxn    = Input(Bool())
    val pci_exp_rxp    = Input(Bool())
    val pci_exp_txn    = Output(Bool())
    val pci_exp_txp    = Output(Bool())

    // AXI clock
    val axi_aclk       = Output(Clock())
    val axi_aresetn    = Output(Bool())

    // AXI-MM master interface
    val m_axi  = Flipped(new SlaveAXIInterface(64, 64))
  })
}
