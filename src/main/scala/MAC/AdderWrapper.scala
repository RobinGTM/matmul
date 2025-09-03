/* SAFAdder.scala -- SAF or hardfloat adder wrapper
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

class AdderWrapper(
  USE_HARDFLOAT : Boolean = false,
  DW            : Int = 33,
  SAF_W         : Int = 70,
  SAF_L         : Int = 5
) extends Module {
  private val SAF_WIDTH = SAF_W + 8 - SAF_L
  private val D_WIDTH = if(USE_HARDFLOAT) { DW } else { SAF_WIDTH }
  val io = IO(new GenericAdderInterface(D_WIDTH))

  io.o_res = if(USE_HARDFLOAT) {
    val adder = Module(new AddRecFN(8, 24))
    adder.io.subOp          := false.B
    adder.io.roundingMode   := 0.U
    adder.io.detectTininess := 0.U
    adder.io.a              := io.a
    adder.io.b              := io.b
    adder.io.out
  } else {
    val adder = Module(new SAFAdder(SAF_L, SAF_W))
    adder.i_safA := io.i_a
    adder.i_safB := io.i_b
    adder.o_res
  }
}
