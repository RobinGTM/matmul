/* SAFMAC.scala -- SAF-based MAC
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
import saf.utils._

class SAFMAC(
  DW                : Int = 33,
  L                 : Int = 5,
  W                 : Int = 70,
  DSP_PIPELINE_REGS : Int = 2
) extends Module {
  private val SAF_WIDTH = W + 8 - L
  /* I/O */
  val io    = IO(new MACInterface(DW))
  // Only for testing
  val o_saf = IO(Output(UInt(SAF_WIDTH.W)))

  /* MODULES */
  // Multiplier
  val safMul   = Module(new SAFMul(DW, L, W))
  // Adder
  val safAdder = Module(new SAFAdder(L, W))

  /* INTERNALS */
  val macReg = RegInit(0.U(SAF_WIDTH.W))
  val accReg = RegInit(0.U(SAF_WIDTH.W))

  safMul.i_a := io.i_a
  safMul.i_b := io.i_b

  macReg := RegNext(RegNext(safMul.o_saf))

  safAdder.i_safA := macReg
  safAdder.i_safB := accReg

  when(RegNext(RegNext(RegNext(io.i_acc)))) {
    accReg := safAdder.o_res
  }

  when(io.i_rst) {
    accReg := 0.U
  }

  // Output expanded float32
  io.o_res := SAFToExpF32(accReg)
  // Control output for testing
  o_saf := accReg
}
