/* PLLE4_BASE.scala -- PLLE4_BASE black-box
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

class PLLE4_BASE(
  PLL_MULT : Int,
  PLL_DIV  : Int
) extends BlackBox(Map(
  "CLKFBOUT_MULT" -> PLL_MULT.toString,
  "CLKOUT0_DIVIDE" -> PLL_DIV.toString,
  "IS_RST_INVERTED" -> "1'b1"
)) {
  val io = IO(new Bundle {
    val CLKFBOUT = Output(Clock())
    val CLKOUT0  = Output(Clock())
    val LOCKED   = Output(Bool())
    val CLKFBIN  = Input(Clock())
    val CLKIN    = Input(Clock())
    val RST      = Input(Bool())
  })
}
