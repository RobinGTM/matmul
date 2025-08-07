/* AsyncFIFOTest.scala -- Async FIFO test modules
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

import math.pow

import mcp._
import mcp.interfaces._
import asyncfifo.interfaces._

// Testing class for verilog async FIFO test-bench
class AsyncFIFOTest[T <: Data](
  CNT_W : Int,
  dType : T = UInt(32.W)
) extends RawModule {
  // Read port
  val rd_clk  = IO(Input(Clock()))
  val rd_rst  = IO(Input(Bool()))
  val fifo_rd = IO(new AsyncFIFOReadInterface(dType))

  // Write port
  val wr_clk  = IO(Input(Clock()))
  val wr_rst  = IO(Input(Bool()))
  val fifo_wr = IO(new AsyncFIFOWriteInterface(dType))

  // Memory
  val mem = SyncReadMem(pow(2, CNT_W).toInt, dType)

  // Read port
  val fifoRdPort = withClockAndReset(rd_clk, rd_rst) {
    Module(new AsyncFIFOReadPort(CNT_W, dType))
  }
  fifoRdPort.fifo_rd <> fifo_rd

  // Write port
  val fifoWrPort = withClockAndReset(wr_clk, wr_rst) {
    Module(new AsyncFIFOWritePort(CNT_W, dType))
  }
  fifoWrPort.fifo_wr <> fifo_wr

  // Cross-domain connections
  fifoRdPort.wcnt_cross <> fifoWrPort.wcnt_cross
  fifoRdPort.rcnt_cross <> fifoWrPort.rcnt_cross

  // Memory
  // Write port
  when(fifoWrPort.mem.i_we) {
    mem.write(fifoWrPort.mem.i_addr, fifoWrPort.mem.i_data, wr_clk)
  }

  // Read port
  fifoRdPort.mem.o_data := mem.read(fifoRdPort.mem.i_addr, fifoRdPort.mem.i_en, rd_clk)
}

class AsyncFIFOTestRdFaster[T <: Data](
  CNT_W : Int,
  dType : T = UInt(32.W)
) extends Module {
  // Read port
  // Using implicit clk/reset
  val fifo_rd = IO(new AsyncFIFOReadInterface(dType))

  // Write port
  val wr_clk  = IO(Input(Bool()))
  val wr_rst  = IO(Input(Bool()))
  val fifo_wr = IO(new AsyncFIFOWriteInterface(dType))

  val fifo = Module(new AsyncFIFOTest(CNT_W, dType))
  fifo.fifo_rd <> fifo_rd
  fifo.rd_clk  := clock
  fifo.rd_rst  := reset
  fifo.fifo_wr <> fifo_wr
  fifo.wr_clk  := wr_clk.asClock
  fifo.wr_rst  := wr_rst
}

class AsyncFIFOTestWrFaster[T <: Data](
  CNT_W : Int,
  dType : T = UInt(32.W)
) extends Module {
  // Read port
  val rd_clk  = IO(Input(Bool()))
  val rd_rst  = IO(Input(Bool()))
  val fifo_rd = IO(new AsyncFIFOReadInterface(dType))

  // Write port
  // Using implicit clk/reset
  val fifo_wr = IO(new AsyncFIFOWriteInterface(dType))

  val fifo = Module(new AsyncFIFOTest(CNT_W, dType))
  fifo.fifo_rd <> fifo_rd
  fifo.rd_clk  := rd_clk.asClock
  fifo.rd_rst  := rd_rst
  fifo.fifo_wr <> fifo_wr
  fifo.wr_clk  := clock
  fifo.wr_rst  := reset
}
