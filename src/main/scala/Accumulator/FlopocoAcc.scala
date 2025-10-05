/* FlopocoAcc.scala -- flopoco-based float accumulator
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

import flopoco._
import acc.interfaces._

class FlopocoAcc(
  DW : Int = 33
) extends Module {
  val DELAY_TICKS = 1

  /* I/O */
  val io = IO(new GenericAccInterface(DW))

  /* INTERNALS */
  // Accumulator register
  val accReg = RegInit(0.U(DW.W))

  // Flopoco adder
  val adder = Module(new FPAdd(8, 22, 200))

  // Adder is combinational but Flopoco generates an unused clock
  // @200MHz
  adder.io.clk := DontCare
  adder.io.X   := io.i_in
  adder.io.Y   := accReg

  // Accumulator control
  when(io.i_rst) {
    accReg := 0.U
  } .elsewhen(io.i_acc) {
    accReg := adder.io.R
  }

  // Output
  io.o_res := accReg
}
