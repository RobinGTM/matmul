/* SAXIRW2Full.scala -- Converts a slave AXI-Full interface into 2
 *                      separate read and write interfaces
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
import axi.interfaces._

class SAXIRW2Full(
  AW : Int,
  DW : Int
) extends RawModule {
  val s_axi    = IO(new SlaveAXIInterface(AW, DW))
  val s_axi_rd = IO(Flipped(new SlaveAXIReadInterface(AW, DW)))
  val s_axi_wr = IO(Flipped(new SlaveAXIWriteInterface(AW, DW)))

  s_axi_rd.araddr  := s_axi.araddr
  s_axi_rd.arlen   := s_axi.arlen
  s_axi.arready    := s_axi_rd.arready
  s_axi_rd.arvalid := s_axi.arvalid

  s_axi.rdata      := s_axi_rd.rdata
  s_axi.rlast      := s_axi_rd.rlast
  s_axi_rd.rready  := s_axi.rready
  s_axi.rvalid     := s_axi_rd.rvalid

  s_axi_wr.awaddr  := s_axi.awaddr
  s_axi_wr.awlen   := s_axi.awlen
  s_axi.awready    := s_axi_wr.awready
  s_axi_wr.awvalid := s_axi.awvalid

  s_axi_wr.wdata   := s_axi.wdata
  s_axi_wr.wstrb   := s_axi.wstrb
  s_axi_wr.wlast   := s_axi.wlast
  s_axi.wready     := s_axi_wr.wready
  s_axi_wr.wvalid  := s_axi.wvalid

  s_axi_wr.bready  := s_axi.bready
  s_axi.bvalid     := s_axi_wr.bvalid
}
