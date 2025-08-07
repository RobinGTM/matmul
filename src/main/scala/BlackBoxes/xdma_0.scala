/* xdma_0.scala -- XDMA IP black-box
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

    // AXI-Lite master interface
    val m_axil = Flipped(new SlaveAXILiteInterface(32, 32))
    // AXI-MM master interface
    val m_axi  = Flipped(new SlaveAXIInterface(64, 64))
  })
}
