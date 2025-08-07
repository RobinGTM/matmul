/* AXILiteSlaveMCPWrapper.scala -- Wraps an AXI-Lite slave and three
 *                                 MCP (2 sources, 1 destination) for
 *                                 passing AR/R/W+AW data through a
 *                                 clock domain boundary
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

import chisel3._
import chisel3.util.log2Up
// import chisel3.util.Enum

import math.pow

import axi.interfaces._
import mcp._
import mcp.interfaces._
import matmul.utils.WrAddrData

/**
  Simple wrapper for AXILiteSlave and MCPs. *_cross interfaces are at the
  boundary of the clock domain.
 */
class AXILiteSlaveMCPWrapper(
  AW                : Int,
  DW                : Int,
  CTL_OFFSET_BYTES  : Int = 0x0,
  N_ADDITIONAL_REGS : Int = 0
) extends Module {
  /* I/O */
  val s_axil = IO(new SlaveAXILiteInterface(AW, DW))
  val wr_src_cross = IO(new MCPCrossSrc2DstInterface(WrAddrData(AW, DW)))
  val ar_src_cross = IO(new MCPCrossSrc2DstInterface(UInt(AW.W)))
  val rd_dst_cross = IO(Flipped(new MCPCrossSrc2DstInterface(UInt(DW.W))))

  /* MODULES */
  val wrMcpSrc = Module(new MultiCyclePathSrc(WrAddrData(AW, DW)))
  val arMcpSrc = Module(new MultiCyclePathSrc(UInt(AW.W)))
  val rdMcpDst = Module(new MultiCyclePathDst(UInt(DW.W)))
  val sAxiL    = Module(new AXILiteSlave(AW, DW, CTL_OFFSET_BYTES, N_ADDITIONAL_REGS))

  /* WIRING */
  // AXI-Lite interface
  sAxiL.s_axil <> s_axil

  // Clock-domain crossing signals
  wrMcpSrc.io_cross <> wr_src_cross
  arMcpSrc.io_cross <> ar_src_cross
  rdMcpDst.io_cross <> rd_dst_cross

  // Communication with AXI-Lite slave
  sAxiL.wr_src <> wrMcpSrc.io_src
  sAxiL.ar_src <> arMcpSrc.io_src
  sAxiL.rd_dst <> rdMcpDst.io_dst
}
