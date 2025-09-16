/* SAFAccumulator.scala -- SAF accumulator
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

import saf._
import saf.utils._
import acc.interfaces._

class SAFAcc(
  L : Int = 5,
  W : Int = 70
) extends Module {
  private val SAF_W = W + 8 - L
  private val DW = 32
  private val EW = 8
  private val MW = 23
  // Expose delay ticks to upper levels
  val DELAY_TICKS = 2

  /* I/O */
  val io = IO(new GenericAccInterface(SAF_W))

  /* INTERNALS */
  // Decompose inputs
  val iRe = io.i_in(SAF_W - 1, W)
  val iEm = io.i_in(W - 1, 0).asSInt

  // Pipeline register for incoming mantissa
  val iEmDelayReg = RegNext(iEm)

  // Reduced exponent accumulator
  val accReReg = RegInit(0.U((EW - L).W))
  // Extended mantissa accumulator
  val accEmReg = RegInit(0.S((W + 1).W))
  // Mantissae shift amountss
  // Incoming
  val iEmShamtReg   = RegInit(0.U((EW - L).W))
  // Accumulated
  val accEmShamtReg = RegInit(0.U((EW - L).W))

  // Overflow correction
  // Unsigned sum mantissa
  val uAccEm = Mux(accEmReg(W), 1.U + ~accEmReg.asUInt, accEmReg.asUInt)
  // Compute position of MSB
  val msbPos = (W + 1).U - PriorityEncoder(Reverse(uAccEm))
  // Correction register (for overflow)
  val corrReg = RegNext(msbPos > (W - 1).U)
  // Corrected accumulator reduced exponent
  val corrRe = accReReg + corrReg

  // Reduced exponent accumulation and shift amounts
  when(io.i_rst) {
    accReReg      := 0.U
    accEmReg      := 0.S
    iEmShamtReg   := 0.U
    accEmShamtReg := 0.U
    iEmDelayReg   := 0.S
    corrReg       := 0.U
  } .elsewhen(io.i_acc) {
    // Accumulated reduced exponent
    accReReg      := Mux(corrRe > iRe, corrRe, iRe)
    // Shift amount for incoming mantissa
    iEmShamtReg   := Mux(corrRe > iRe,
      Mux(accReReg - iRe > 3.U, 3.U, corrRe - iRe),
      0.U
    )
    // Shift amount for accumulated mantissa
    accEmShamtReg := Mux(corrRe > iRe,
      0.U,
      Mux(iRe - corrRe > 3.U, 3.U, iRe - corrRe)
    )
  } .otherwise {
    iEmShamtReg   := 0.U
    accEmShamtReg := 0.U
  }

  // Mantissa accumulation
  val shiftedIEm   = iEmDelayReg >> (iEmShamtReg << L)
  val shiftedAccEm = accEmReg    >> (accEmShamtReg << L)

  // Accumulate extended mantissa
  when(RegNext(io.i_acc)) {
    accEmReg := (shiftedIEm +& shiftedAccEm) >> (corrReg << L)
  }

  // Output
  io.o_res := Cat(RegNext(accReReg), accEmReg(W - 1, 0))
}
