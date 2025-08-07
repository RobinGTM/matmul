/* SAFAdder.scala -- SAF adder
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
package saf

import chisel3._
import chisel3.util._

// Outputs a + b in SAF
class SAFAdder(
  L   : Int = 5,
  W   : Int = 70
) extends RawModule {
  private val SAF_W = W + 8 - L
  private val DW = 32
  private val EW = 8
  private val MW = 23
  require(W >= MW + 2 + (1 << L))

  /* I/O */
  val i_safA = IO(Input(UInt(SAF_W.W)))
  val i_safB = IO(Input(UInt(SAF_W.W)))
  val o_res  = IO(Output(UInt(SAF_W.W)))

  /* SUM */
  // Decompose inputs
  val reA = i_safA(SAF_W - 1, SAF_W - (EW - L))
  val maA = i_safA(SAF_W - (EW - L) - 1, 0).asSInt
  val reB = i_safB(SAF_W - 1, SAF_W - (EW - L))
  val maB = i_safB(SAF_W - (EW - L) - 1, 0).asSInt

  // Sum
  val sumRe = Mux(reA > reB,
    reA,
    reB
  )
  val aShamt = sumRe - reA
  val bShamt = sumRe - reB
  val sumMa  = (maA >> (aShamt << L)) +& (maB >> (bShamt << L))

  // Unsigned sum mantissa
  val uSumMa = Mux(sumMa(W), 1.U + ~sumMa.asUInt, sumMa.asUInt)
  // Compute position of MSB
  val msbPos = (W + 1).U - PriorityEncoder(Reverse(uSumMa))

  // Prime SAF conversion
  // Normalize if mantissa has its MSB set while none of the input
  // mantissae were negative (possible?)
  // Also normalize if the sum has its 2nd MSB set, otherwise it will
  // be interpreted as negative
  val outMa = Wire(UInt(W.W))
  val outRe = Wire(UInt((EW - L).W))
  // If MSB is set, must normalize
  when(msbPos > (W - 1).U) {
    outMa := (sumMa >> (1.U << L))(W - 1, 0).asUInt
    // when(sumMa(W - 1) & ~maA(W - 1) & ~maB(W - 1)) {
    //   // Negative overflow: result is negative while both operands are
    //   // positive -> logical shift (result is positive)
    //   outMa := (sumMa.asUInt >> (1.U << L))(W - 1, 0)
    // } .otherwise {
    //   // Positive overflow: result is positive while both operands are
    //   // negative -> arithmetic shift (result is negative)
    //   outMa := (sumMa >> (1.U << L))(W - 1, 0).asUInt
    // }
    outRe := sumRe + 1.U
  } .otherwise {
    outMa := sumMa(W - 1, 0)
    outRe := sumRe
  }

  when(sumMa === 0.S) {
    o_res := 0.U
  } .otherwise {
    o_res := Cat(outRe, outMa)
  }
}
