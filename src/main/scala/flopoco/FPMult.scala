/* FPMult.scala -- Black-box for Flopoco float multiplier
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

class FPMult(
  WE             : Int = 8,
  WF             : Int = 22,
  FREQ           : Int = 0,
  PIPELINE_DEPTH : Int = 0
) extends BlackBox {
  val DELAY_TICKS = PIPELINE_DEPTH // Flopoco defined
  val io = IO(new Bundle {
    // val clk = Input(Clock())
    val X   = Input(UInt((WE + WF + 3).W))
    val Y   = Input(UInt((WE + WF + 3).W))
    val R   = Output(UInt((WE + WF + 3).W))
  })
  val sub = if(FREQ == 0) { "comb" } else { s"Freq${FREQ}" }
  override def desiredName = s"FPMult_${WE}_${WF}_uid2_${sub}_uid3"
}
