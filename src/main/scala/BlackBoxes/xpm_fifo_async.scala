/* XPM_FIFO_ASYNC.scala -- XPM_ASYNC_FIFO macro
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

// Scala
import math.log

class xpm_fifo_async(
  // DECIMAL
  FIFO_WRITE_DEPTH    : Int = 2048,
  // DECIMAL
  FIFO_READ_LATENCY   : Int = 1,
  // DECIMAL
  RD_DATA_COUNT_WIDTH : Int = 1,
  // DECIMAL
  READ_DATA_WIDTH     : Int = 32,
  // DECIMAL
  WR_DATA_COUNT_WIDTH : Int = 1,
  // DECIMAL
  WRITE_DATA_WIDTH    : Int = 32
) extends BlackBox(Map(
  // DECIMAL
  "FIFO_READ_LATENCY"   -> FIFO_READ_LATENCY,
  // DECIMAL
  "WRITE_DATA_WIDTH"    -> WRITE_DATA_WIDTH,
  // DECIMAL
  "WR_DATA_COUNT_WIDTH" -> WR_DATA_COUNT_WIDTH,
  // DECIMAL
  "READ_DATA_WIDTH"     -> READ_DATA_WIDTH,
  // DECIMAL
  "FIFO_WRITE_DEPTH"    -> FIFO_WRITE_DEPTH,
  // DECIMAL
  "RD_DATA_COUNT_WIDTH" -> RD_DATA_COUNT_WIDTH,
  // DECIMAL
  "CASCADE_HEIGHT"      -> 0,
  // DECIMAL
  "CDC_SYNC_STAGES"     -> 2,
  // String
  "DOUT_RESET_VALUE"    -> "0",
  // String
  "ECC_MODE"            -> "no_ecc",
  // String
  "EN_SIM_ASSERT_ERR"   -> "warning",
  // String
  "FIFO_MEMORY_TYPE"    -> "auto",
  // DECIMAL
  "FULL_RESET_VALUE"    -> 0,
  // DECIMAL
  "PROG_EMPTY_THRESH"   -> 10,
  // DECIMAL
  "PROG_FULL_THRESH"    -> 10,
  // String
  "READ_MODE"           -> "std",
  // DECIMAL
  "RELATED_CLOCKS"      -> 0,
  // DECIMAL; 0=disable simulation messages, 1=enable simulation messages
  "SIM_ASSERT_CHK"      -> 0,
  // String
  "USE_ADV_FEATURES"    -> "0707",
  // DECIMAL
  "WAKEUP_TIME"         -> 0
)) {
  require(log2Up(FIFO_WRITE_DEPTH) == (log(FIFO_WRITE_DEPTH) / log(2)).toInt)
  val io = IO(new Bundle {
    // 1-bit output: Almost Empty : When asserted, this signal
    // indicates that only one more read can be performed before the
    // FIFO goes to empty.
    val almost_empty  = Output(Bool())
    // 1-bit output: Almost Full: When asserted, this signal indicates
    // that only one more write can be performed before the FIFO is
    // full.
    val almost_full   = Output(Bool())
    // 1-bit output: Read Data Valid: When asserted, this signal
    // indicates that valid data is available on the output bus
    // (dout).
    val data_valid    = Output(Bool())
    // 1-bit output: Double Bit Error: Indicates that the ECC decoder
    // detected a double-bit error and data in the FIFO core is
    // corrupted.
    val dbiterr       = Output(Bool())
    // READ_DATA_WIDTH-bit output: Read Data: The output data bus is
    // driven when reading the FIFO.
    val dout          = Output(UInt(READ_DATA_WIDTH.W))
    // 1-bit output: Empty Flag: When asserted, this signal indicates
    // that the FIFO is empty. Read requests are ignored when the FIFO
    // is empty, initiating a read while empty is not destructive to
    // the FIFO.
    val empty         = Output(Bool())
    // 1-bit output: Full Flag: When asserted, this signal indicates
    // that the FIFO is full. Write requests are ignored when the FIFO
    // is full, initiating a write when the FIFO is full is not
    // destructive to the contents of the FIFO.
    val full          = Output(Bool())
    // 1-bit output: Overflow: This signal indicates that a write
    // request (wren) during the prior clock cycle was rejected,
    // because the FIFO is full. Overflowing the FIFO is not
    // destructive to the contents of the FIFO.
    val overflow      = Output(Bool())
    // 1-bit output: Programmable Empty: This signal is asserted when
    // the number of words in the FIFO is less than or equal to the
    // programmable empty threshold value. It is de-asserted when the
    // number of words in the FIFO exceeds the programmable empty
    // threshold value.
    val prog_empty    = Output(Bool())
    // 1-bit output: Programmable Full: This signal is asserted when
    // the number of words in the FIFO is greater than or equal to the
    // programmable full threshold value. It is de-asserted when the
    // number of words in the FIFO is less than the programmable full
    // threshold value.
    val prog_full     = Output(Bool())
    // RD_DATA_COUNT_WIDTH-bit output: Read Data Count: This bus
    // indicates the number of words read from the FIFO.
    val rd_data_count = Output(UInt(RD_DATA_COUNT_WIDTH.W))
    // 1-bit output: Read Reset Busy: Active-High indicator that the
    // FIFO read domain is currently in a reset state.
    val rd_rst_busy   = Output(Bool())
    // 1-bit output: Single Bit Error: Indicates that the ECC decoder
    // detected and fixed a single-bit error.
    val sbiterr       = Output(Bool())
    // 1-bit output: Underflow: Indicates that the read request
    // (rd_en) during the previous clock cycle was rejected because
    // the FIFO is empty. Under flowing the FIFO is not destructive to
    // the FIFO.
    val underflow     = Output(Bool())
    // 1-bit output: Write Acknowledge: This signal indicates that a
    // write request (wr_en) during the prior clock cycle is
    // succeeded.
    val wr_ack        = Output(Bool())
    // WR_DATA_COUNT_WIDTH-bit output: Write Data Count: This bus
    // indicates the number of words written into the FIFO.
    val wr_data_count = Output(UInt(WR_DATA_COUNT_WIDTH.W))
    // 1-bit output: Write Reset Busy: Active-High indicator that the
    // FIFO write domain is currently in a reset state.
    val wr_rst_busy   = Output(Bool())
    // WRITE_DATA_WIDTH-bit input: Write Data: The input data bus used
    // when writing the FIFO.
    val din           = Input(UInt(WRITE_DATA_WIDTH.W))
    // 1-bit input: Double Bit Error Injection: Injects a double bit
    // error if the ECC feature is used on block RAMs or UltraRAM
    // macros.
    val injectdbiterr = Input(Bool())
    // 1-bit input: Single Bit Error Injection: Injects a single bit
    // error if the ECC feature is used on block RAMs or UltraRAM
    // macros.
    val injectsbiterr = Input(Bool())
    // 1-bit input: Read clock: Used for read operation. rd_clk must
    // be a free running clock.
    val rd_clk        = Input(Clock())
    // 1-bit input: Read Enable: If the FIFO is not empty, asserting
    // this signal causes data (on dout) to be read from the
    // FIFO. Must be held active-low when rd_rst_busy is active high.
    val rd_en         = Input(Bool())
    // 1-bit input: Reset: Must be synchronous to wr_clk. The clock(s)
    // can be unstable at the time of applying reset, but reset must
    // be released only after the clock(s) is/are stable.
    val rst           = Input(Bool()) 
    // 1-bit input: Dynamic power saving: If sleep is High, the
    // memory/fifo block is in power saving mode.
    val sleep         = Input(Bool())
    // 1-bit input: Write clock: Used for write operation. wr_clk must
    // be a free running clock.
    val wr_clk        = Input(Clock())
    // 1-bit input: Write Enable: If the FIFO is not full, asserting
    // this signal causes data (on din) to be written to the
    // FIFO. Must be held active-low when rst or wr_rst_busy is active
    // high.
    val wr_en          = Input(Bool())
  })
}
