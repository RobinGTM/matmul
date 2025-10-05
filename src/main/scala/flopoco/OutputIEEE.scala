/* OutputIEEE.scala -- Black-box for Flopoco to IEEE format converter
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
package flopoco

// Chisel
import chisel3._
import chisel3.util._
import chisel3.experimental._

class OutputIEEE(
  DW   : Int = 33,
  FREQ : Int = 300
) extends BlackBox {
  val io = IO(new Bundle {
    val X = Input(UInt(DW.W))
    val R = Output(UInt(DW.W))
  })
  val freq_str = if(FREQ == 0) { "comb" } else { s"Freq${FREQ}" }
  override def desiredName = s"OutputIEEE_8_22_to_8_23_${freq_str}_uid2"
}
