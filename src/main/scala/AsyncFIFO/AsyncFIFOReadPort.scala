/* AsyncFIFOReadPort.scala -- Async FIFO read port
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
package asyncfifo

import chisel3._
import chisel3.util._

import mcp._
import mcp.interfaces._
import asyncfifo.interfaces._

// Based on Cumming's Async FIFO and Clock Domain Crossing whitepapers
class AsyncFIFOReadPort[T <: Data](
  CNT_W : Int,
  dType : T = UInt(32.W)
) extends Module {
  /* I/O */
  // External FIFO read interface
  val fifo_rd    = IO(new AsyncFIFOReadInterface(dType))
  // Internal RAM interface
  val mem        = IO(new BasicMemReadInterface(CNT_W, dType))
  // MCP cross-domain interface for synchronized write counter (destination)
  val wcnt_cross = IO(Flipped(new MCPCrossSrc2DstInterface(UInt((CNT_W + 1).W))))
  // MCP cross-domain interface for synchronized read counter (source)
  val rcnt_cross = IO(new MCPCrossSrc2DstInterface(UInt((CNT_W + 1).W)))

  /* READ POINTER COUNTER */
  // FIFO counters have 1 more bit than necessary to be able to determine if
  // write pointer has wrapped over read pointer (FIFO full) or if FIFO is
  // empty
  val rCntReg = RegInit(0.U((CNT_W + 1).W))
  // Actual FIFO pointer
  val rPtr    = rCntReg(CNT_W - 1, 0)
  // Memory's read port is addressed by the read pointer (read counter without
  // MSB)
  mem.i_addr := rPtr

  /* SAMPLED READ COUNTER MCP */
  // Starting logic
  val samplingStartedReg     = RegInit(false.B)
  // samplingStartedReg ^ samplingStartedPrevReg will generate a pulse
  // that will be the first crossing pulse for sampled read counter
  // MCP after reset
  val samplingStartedPrevReg = RegNext(samplingStartedReg)
  // Increment sampling started state reg right after reset
  samplingStartedReg        := true.B
  // MCP source
  val rCntMcpSrc = Module(new MultiCyclePathSrc(UInt((CNT_W + 1).W)))
  rCntMcpSrc.io_cross <> rcnt_cross
  // Sampling register
  val rCntSampleReg = RegInit(0.U)
  // Sample reg is sent to other clock domain
  rCntMcpSrc.io_src.data := rCntSampleReg
  // Sample read counter on ack
  when(rCntMcpSrc.io_src.ack) {
    rCntSampleReg := rCntReg
  }
  // Also, cross again on ack (and cross on sampling started right
  // after reset)
  rCntMcpSrc.io_src.cross_pulse := (
    rCntMcpSrc.io_src.ack | (samplingStartedReg ^ samplingStartedPrevReg)
  )

  /* SAMPLED WRITE COUNTER MCP */
  val wCntMcpDst  = Module(new MultiCyclePathDst(UInt((CNT_W + 1).W)))
  wCntMcpDst.io_cross <> wcnt_cross
  // Synchronized write pointer
  val wCntSyncReg = RegInit(0.U((CNT_W + 1).W))
  when(wCntMcpDst.io_dst.load_pulse) {
    wCntSyncReg := wCntMcpDst.io_dst.data
  }

  /* EMPTY LOGIC */
  // FIFO is empty when sampled write counter and read counter are equal
  // (pessimistic empty estimation, empty might go high before the FIFO is
  // actually empty)
  // Also, write counter is compared with the next value of the read
  // counter
  val fifoEmpty    = wCntSyncReg === rCntReg
  // Data is valid when FIFO is not empty
  // NB: Sync read => when valid, data output on NEXT CLOCK EDGE is
  // valid
  fifo_rd.o_nempty := ~fifoEmpty

  /* OUTPUT */
  // Actually read memory only when FIFO is not empty
  mem.i_en       := ~fifoEmpty
  fifo_rd.o_data := mem.o_data

  /* READ COUNTER LOGIC */
  when(fifo_rd.i_en & ~fifoEmpty) {
    rCntReg := rCntReg + 1.U
  }
}
