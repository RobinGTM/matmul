/* AXILiteSlave.scala -- AXI-Lite slave controller
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

import matmul.utils.Parameters
import axi.interfaces._
import matmul.utils.WrAddrData
import mcp.interfaces._

class AXILiteSlave(
  AW                : Int,
  DW                : Int,
  CTL_OFFSET_BYTES  : Int = 0x0,
  N_ADDITIONAL_REGS : Int = 0
) extends Module {
  // AXI-Lite addresses point to each byte, but we want to address
  // C_S_AXI_DATA_WIDTH-bits widde registers, so drop LSBs
  // (https://zipcpu.com/blog/2020/03/08/easyaxil.html // ADDRLSB)
  // We only need to address every C_S_AXI_DATA_WIDTH position (no
  // sub-words)
  private val DROP_LSBS = log2Up(DW) - 3
  // Total number of registers to address (a control register is always
  // assumed)
  private val TOTAL_NREG = N_ADDITIONAL_REGS + 1
  // Base offset must be a multiple of 4
  require(CTL_OFFSET_BYTES % pow(2, DROP_LSBS) == 0);
  /* Constants */
  // Define control offset in word address
  private val CTL_OFFSET     = (CTL_OFFSET_BYTES / pow(2, DROP_LSBS)).toInt
  // Maximum address
  private val ADDR_END       = CTL_OFFSET + N_ADDITIONAL_REGS + 1
  // Define width for internal addresses
  private val INT_ADDR_WIDTH = AW - DROP_LSBS - 1
  // Define offsets as UInts
  private val CTL_OFFSET_U   = (CTL_OFFSET).U(INT_ADDR_WIDTH.W)
  private val ADDR_END_U     = ADDR_END.U(INT_ADDR_WIDTH.W)

  // AXI-Lite interface
  val s_axil = IO(new SlaveAXILiteInterface(AW, DW))

  // Source interface for the write address / data MCP
  val wr_src = IO(Flipped(new MultiCyclePathSrcInterface(WrAddrData(AW, DW))))
  // Source interface for the read address MCP
  val ar_src = IO(Flipped(new MultiCyclePathSrcInterface(UInt(AW.W))))
  // Destination interface for the read data MCP
  val rd_dst = IO(Flipped(new MultiCyclePathDstInterface(UInt(DW.W))))

  /* READ CHANNEL */
  // Read address channel handshake
  val arHandshake = s_axil.arready & s_axil.arvalid
  // Waiting for ar_src ack
  val arWaitForAckReg = RegInit(false.B)
  // Ready when not waiting for ack or when receiving ack
  s_axil.arready := ar_src.ack | (~arWaitForAckReg)

  // Send cross pulse on handshake (lasts only 1 tick since arready
  // goes low right after handshake)
  ar_src.cross_pulse := arHandshake
  // ar_src.data gets read addr (only when handshaking to prevent
  // useless switching)
  ar_src.data := Mux(ar_src.cross_pulse, s_axil.araddr(AW - 1, DROP_LSBS), 0.U)
  // Start waiting for ack after handshake, stop waiting for ack when
  // receiving it
  when(arHandshake) {
    arWaitForAckReg := true.B
  }.elsewhen(ar_src.ack & arWaitForAckReg) {
    arWaitForAckReg := false.B
  }

  // Register to store data incoming from rd_dst
  val rDataReg = RegInit(0.U(DW.W))
  val rValidReg = RegInit(false.B)
  val rHandshake = s_axil.rvalid & s_axil.rready
  // If load_pulse is received on handshake (meaning, load is received
  // when R channel is ready to accept data), no need to latch
  // received data and valid signals since the data is read
  // immediately
  when(rd_dst.load_pulse & ~(rHandshake)) {
    rDataReg  := rd_dst.data
    rValidReg := true.B
  } .elsewhen(rHandshake) {
    rValidReg := false.B
  }
  // Always respond 0 (don't support errors)
  s_axil.rresp  := 0.U
  // Read data is valid when received load pulse and until handshake
  s_axil.rvalid := rValidReg | rd_dst.load_pulse
  // rdata directly gets the MCP output when pulse is 1, and gets the
  // latched same value after that.
  s_axil.rdata  := Mux(rd_dst.load_pulse, rd_dst.data, rDataReg)

  /* WRITE CHANNEL */
  val awHandshake = s_axil.awvalid & s_axil.awready
  val wHandshake = s_axil.wvalid & s_axil.wready

  // Waiting for wr_src.ack pulse register
  val wWaitForAckReg = RegInit(false.B)
  // Ready after wvalid is set and not waiting for ack
  val awReadyReg = RegInit(false.B)
  when(~awReadyReg & (s_axil.awvalid & s_axil.wvalid) & ~wWaitForAckReg) {
    awReadyReg := true.B
  } .elsewhen(wHandshake) {
    awReadyReg := false.B
  }
  // Synchronize W and AW channels
  s_axil.awready := awReadyReg
  s_axil.wready  := awReadyReg

  // Set wWaitForAckReg on handshake, reset on ack
  when(wHandshake) {
    wWaitForAckReg := true.B
  } .elsewhen(wr_src.ack & wWaitForAckReg) {
    wWaitForAckReg := false.B
  }

  // Source MCP cross pulse is driven by both handshakes (only 1
  // should be needed since they're synchronized but let's make sure)
  wr_src.cross_pulse := awHandshake & wHandshake
  // Source MCP gets address / data on cross pulse (no need to latch,
  // MCP already latches incoming data)
  wr_src.data.addr := Mux(wr_src.cross_pulse, s_axil.awaddr(AW - 1, DROP_LSBS), 0.U)
  wr_src.data.data := Mux(wr_src.cross_pulse, s_axil.wdata, 0.U)
  // NB: This slave ignores write strobes

  // Write response
  // Always OK
  s_axil.bresp := 0.U
  // Stall bus until receiving the ack pulse from the MCP
  val bHandshake = s_axil.bready & s_axil.bvalid
  val bValidReg = RegInit(false.B)
  when(wr_src.ack & ~bHandshake) {
    bValidReg := true.B
  } .elsewhen(bHandshake) {
    bValidReg := false.B
  }
  s_axil.bvalid := bValidReg | wr_src.ack
}
