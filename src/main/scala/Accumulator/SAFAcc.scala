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

  // Reduced exponent accumulation and shift amounts
  when(io.i_rst) {
    accReReg      := 0.U
    accEmReg      := 0.S
    iEmShamtReg   := 0.U
    accEmShamtReg := 0.U
    iEmDelayReg   := 0.S
  } .elsewhen(io.i_acc) {
    // Accumulated reduced exponent
    accReReg      := Mux(accReReg > iRe, accReReg, iRe)
    // Shift amount for incoming mantissa
    iEmShamtReg   := Mux(accReReg > iRe,
      Mux(accReReg - iRe > 3.U, 3.U, accReReg - iRe),
      0.U
    )
    // Shift amount for accumulated mantissa
    accEmShamtReg := Mux(accReReg > iRe,
      0.U,
      Mux(iRe - accReReg > 3.U, 3.U, iRe - accReReg)
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
    accEmReg := shiftedIEm +& shiftedAccEm
  }

  // Unsigned sum mantissa
  val uAccEm = Mux(accEmReg(W), 1.U + ~accEmReg.asUInt, accEmReg.asUInt)
  // Compute position of MSB
  val msbPos = (W + 1).U - PriorityEncoder(Reverse(uAccEm))

  // Output normalization // PIPELINE???
  val outRe = Wire(UInt((EW - L).W))
  val outEm = Wire(UInt(W.W))
  when(msbPos > (W - 1).U) {
    outRe := accReReg + 1.U
    outEm := (accEmReg >> (1.U << L))(W - 1, 0).asUInt
  } .otherwise {
    outRe := accReReg
    outEm := accEmReg.asUInt
  }

  // Output
  io.o_res := Cat(outRe, outEm)
}
