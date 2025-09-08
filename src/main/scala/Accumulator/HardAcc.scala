/* HardAcc.scala -- hardfloat-based float accumulator
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
package acc

import chisel3._
import chisel3.util._

import hardfloat._
import acc.interfaces._

class HardAcc(
  DW    : Int = 33,
  EXP_W : Int = 8,
  SIG_W : Int = 24
) extends Module {
  /* I/O */
  val io = IO(new GenericAccInterface(DW))

  /* INTERNALS */
  // Accumulator register
  val accReg = RegInit(0.U(DW.W))

  // hardfloat adder
  val adder = Module(new AddRecFN(EXP_W, SIG_W))
  adder.io.subOp          := false.B
  adder.io.roundingMode   := 0.U
  adder.io.detectTininess := 0.U
  adder.io.a              := io.i_in
  adder.io.b              := accReg

  // Accumulator control
  when(io.i_rst) {
    accReg                := 0.U
  } .elsewhen(io.i_acc) {
    accReg                := adder.io.out
  }

  // Output
  io.o_res := accReg
}
