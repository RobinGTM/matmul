/* AsyncFIFOWritePort.scala -- Async FIFO write port
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

// Based on Cumming's Async FIFO and Clock Domain Crossing
// whitepapers. The logic is really similar to that of the read port,
// which source is way more commented
class AsyncFIFOWritePort[T <: Data](
  CNT_W : Int,
  dType : T = UInt(32.W)
) extends Module {
  /* I/O */
  // External FIFO write interface
  val fifo_wr    = IO(new AsyncFIFOWriteInterface(dType))
  // Internal RAM interface
  val mem        = IO(new BasicMemWriteInterface(CNT_W, dType))
  // MCP cross-domain interface for synchronized read counter (destination)
  val rcnt_cross = IO(Flipped(new MCPCrossSrc2DstInterface(UInt((CNT_W + 1).W))))
  // MCP cross-domain interface for synchronized write counter (source)
  val wcnt_cross = IO(new MCPCrossSrc2DstInterface(UInt((CNT_W + 1).W)))

  /* WRITE POINTER COUNTER */
  // FIFO counters have 1 more bit than necessary to be able to
  // determine if write pointer has wrapped over and catched read
  // pointer (FIFO full) or if FIFO is empty
  val wCntReg = RegInit(0.U((CNT_W + 1).W))
  // Actual FIFO pointer
  val wPtr    = wCntReg(CNT_W - 1, 0)
  // Memory addressing
  mem.i_addr := wPtr

  /* SAMPLED WRITE COUNTER MCP */
  // Starting logic
  val samplingStartedReg     = RegInit(false.B)
  val samplingStartedPrevReg = RegNext(samplingStartedReg)
  samplingStartedReg        := true.B
  // MCP source
  val wCntMcpSrc = Module(new MultiCyclePathSrc(UInt((CNT_W + 1).W)))
  wCntMcpSrc.io_cross <> wcnt_cross
  val wCntSampleReg = RegInit(0.U)
  wCntMcpSrc.io_src.data := wCntSampleReg
  when(wCntMcpSrc.io_src.ack) {
    wCntSampleReg := wCntReg
  }
  wCntMcpSrc.io_src.cross_pulse := (
    wCntMcpSrc.io_src.ack | (samplingStartedReg ^ samplingStartedPrevReg)
  )

  /* SAMPLED READ COUNTER MCP */
  val rCntMcpDst = Module(new MultiCyclePathDst(UInt((CNT_W + 1).W)))
  rCntMcpDst.io_cross <> rcnt_cross
  // Synchronized read counter
  val rCntSyncReg = RegInit(0.U((CNT_W + 1).W))
  when(rCntMcpDst.io_dst.load_pulse) {
    rCntSyncReg := rCntMcpDst.io_dst.data
  }

  /* FULL LOGIC */
  // FIFO is full when read and write counters' MSBs are different but the rest
  // is equal (meaning that write counter has wrapped 1 more time than read
  // counter). This is a pessimistic estimation (full might go high before the
  // FIFO is actually full)
  val fifoFull = (
    (rCntSyncReg(CNT_W) =/= wCntReg(CNT_W)) &
    (rCntSyncReg(CNT_W - 1, 0) === wPtr)
  )
  // FIFO is ready to accept data when not full
  fifo_wr.o_ready := ~fifoFull

  /* INPUT */
  mem.i_data := fifo_wr.i_data
  // Actually write memory only when FIFO is not full
  mem.i_we   := fifo_wr.i_we & ~fifoFull

  /* WRITE COUNTER LOGIC */
  when(fifo_wr.i_we & ~fifoFull) {
    wCntReg := wCntReg + 1.U
  }
}
