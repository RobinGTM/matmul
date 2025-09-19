/* BetterBRAM.scala -- Better block RAM implementation
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
package matmul.utils

import chisel3._
import chisel3.util._

import matmul.utils._

// BetterBRAM allows pipelining BRAM primitives by specifying
// additional output registers
// https://www.reddit.com/r/FPGA/comments/157usj0/bram_outpout_pipeline_and_module_boundaries_in/
// UG1387 -- Inferring RAM and ROM
// https://docs.amd.com/r/en-US/ug1387-acap-hardware-ip-platform-dev-methodology/Performance-Considerations-When-Implementing-RAM
class BetterBRAM(
  DW                   : Int = 32,
  SIZE                 : Int = 1024,
  OUTPUT_PIPELINE_REGS : Int = 2
) extends BlackBox with HasBlackBoxInline {
  private val CNT_W = log2Up(SIZE)
  /* I/O */
  val io = IO(new Bundle {
    val i_clk  = Input(Clock())
    val i_rst  = Input(Bool())
    val i_data = Input(UInt(DW.W))
    val i_addr = Input(UInt(log2Up(SIZE).W))
    val i_we   = Input(Bool())
    val i_en   = Input(Bool())
    val o_data = Output(UInt(DW.W))
  })
  // Expose output delay to upper modules
  val OUTPUT_ADD_TICKS = OUTPUT_PIPELINE_REGS

  /* BEHAVIOR */
  setInline("BetterBRAM.sv",
    s"""module BetterBRAM(
      |  input i_clk,
      |  input i_rst,
      |  input [${DW - 1}:0] i_data,
      |  input [${log2Up(SIZE) - 1}:0] i_addr,
      |  input i_we,
      |  input i_en,
      |  output [${DW - 1}:0] o_data
      |);
      |  reg [${DW - 1}:0] mem[0:${SIZE - 1}];
      |  reg [${DW - 1}:0] dout;
      |  reg [${DW - 1}:0] doutPipeline[${OUTPUT_PIPELINE_REGS - 1}:0];
      |  always @(posedge i_clk) begin
      |    if (i_rst) begin
      |      for (int i = 0; i < ${OUTPUT_PIPELINE_REGS}; i = i + 1) begin
      |        doutPipeline[i] <= ${DW}'h0;
      |      end
      |      dout <= ${DW}'h0;
      |    end
      |    else begin
      |      if (i_we)
      |        mem[i_addr] <= i_data;
      |
      |      if (i_en)
      |        dout <= mem[i_addr];
      |
      |      doutPipeline[0] <= dout;
      |      for (int i = 1; i < ${OUTPUT_PIPELINE_REGS}; i = i + 1) begin
      |        doutPipeline[i] <= doutPipeline[i - 1];
      |      end
      |    end
      |  end
      |  if (${OUTPUT_PIPELINE_REGS} == 0)
      |    assign o_data = dout;
      |  else
      |    assign o_data = doutPipeline[${OUTPUT_PIPELINE_REGS - 1}];
      |endmodule
    """.stripMargin
  )
}
