/* MAC.scala -- Templated MAC with SAF or hardfloat arithmetic cores
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
package mac

import chisel3._
import chisel3.util._

import mac.interfaces._
import saf._

class MAC(
  FLOAT             : String = "saf",
  DW                : Int = 33,
  SAF_L             : Int = 5,
  SAF_W             : Int = 70,
  DSP_PIPELINE_REGS : Int = 3
) extends Module {
  /* I/O */
  val io = IO(new MACInterface(DW))

  /* MODULES */
  val mul = Module(new MulWrapper(FLOAT, DW, SAF_W, SAF_L, DSP_PIPELINE_REGS))
  val acc = Module(new AccWrapper(FLOAT, DW, SAF_W, SAF_L))

  /* INTERNALS */
  private val SAF_WIDTH = SAF_W + 8 - SAF_L
  private val REG_WIDTH = FLOAT match {
    case "saf" => SAF_WIDTH
    case _     => DW
  }

  // Additional pipeline register between mul and acc
  val macReg = RegInit(0.U(REG_WIDTH.W))

  mul.io.i_a := io.i_a
  mul.io.i_b := io.i_b

  // // Control signals pipelines
  val accPipelineReg = ShiftRegister(io.i_acc, DSP_PIPELINE_REGS)
  val rstPipelineReg = ShiftRegister(io.i_rst, DSP_PIPELINE_REGS)

  // MAC register
  // macReg := dspPipelineRegs(DSP_PIPELINE_REGS - 1)
  macReg := mul.io.o_res

  // Accumulator wiring
  acc.io.i_acc := RegNext(accPipelineReg)
  acc.io.i_rst := io.i_rst//RegNext(rstPipelineReg)
  acc.io.i_in  := macReg

  // Output
  val DELAY_TICKS = acc.DELAY_TICKS + mul.DELAY_TICKS + (
    FLOAT match {
      case "saf" =>
        val outConv      = withReset(io.i_rst) { Module(new SAFToExpF32(DW, SAF_L, SAF_W)) }
        outConv.i_saf   := acc.io.o_res
        io.o_res        := outConv.o_ef32
        outConv.DELAY_TICKS
      case _     =>
        io.o_res := acc.io.o_res
        0
    })
}
