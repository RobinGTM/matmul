/* MulWrapper.scala -- SAF or hardfloat multiplier wrapper
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

import saf._
import hardfloat._
import flopoco._

import mac.interfaces._

class MulWrapper(
  FLOAT             : String = "saf",
  IN_W              : Int = 33,
  SAF_W             : Int = 70,
  SAF_L             : Int = 5,
  DSP_PIPELINE_REGS : Int = 3
) extends Module {
  // val DELAY_TICKS is exposed at the end of the class
  // Supported float implementations
  // Manually maintained...
  //
  // Lower-level Mult modules should expose a `DELAY_TICKS` attribute
  // that gives the number of cycles between input and output
  //
  val FLTS = List("saf", "hardfloat", "flopoco")
  require(FLTS.find(FLOAT == _) != None,
    s"[MulWrapper] Float implementation \"${FLOAT}\" not supported."
  )

  private val SAF_WIDTH = SAF_W + 8 - SAF_L
  private val OUT_WIDTH = FLOAT match {
    case "saf" => SAF_WIDTH
    case _     => IN_W
  }
  val io = IO(new GenericMulInterface(IN_W, OUT_WIDTH))

  // Expose delay ticks to upper levels
  val DELAY_TICKS = FLOAT match {
    case "saf"       =>
      val mul = Module(new SAFMul(IN_W, SAF_L, SAF_W, DSP_PIPELINE_REGS))
      mul.i_a  := io.i_a
      mul.i_b  := io.i_b
      io.o_res := mul.o_saf
      mul.DELAY_TICKS
    case "hardfloat" =>
      val mul = Module(new PipelinedMulRecFN(8, 24, DSP_PIPELINE_REGS))
      mul.io.roundingMode   := 0.U
      mul.io.detectTininess := 0.U
      mul.io.a              := io.i_a
      mul.io.b              := io.i_b
      io.o_res              := mul.io.out
      mul.DELAY_TICKS
    case "flopoco" =>
      val mul = Module(new FPMult(8, 22, 0, DSP_PIPELINE_REGS))
      // mul.io.clk := clock
      mul.io.X   := io.i_a
      mul.io.Y   := io.i_b
      io.o_res   := mul.io.R
      mul.DELAY_TICKS
  }
}
