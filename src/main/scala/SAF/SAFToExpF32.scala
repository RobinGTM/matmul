/* SAFToExpF32.scala -- SAF to custom expanded binary32 converter
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

class SAFToExpF32(
  DW : Int = 33,
  L  : Int = 5,
  W  : Int = 70
) extends Module {
  private val SAF_W = W + 8 - L
  // Expose delay to upper levels
  val DELAY_TICKS = 4

  /* I/O */
  val i_saf  = IO(Input(UInt(SAF_W.W)))
  val o_ef32 = IO(Output(UInt(DW.W)))

  /* CONVERSION PIPELINE */
  // Reduced exponent
  val reExReg = RegInit(0.U((8 - L).W))
  reExReg    := i_saf(SAF_W - 1, SAF_W - (8 - L))
  // Extended mantissa
  val exMaReg = RegInit(0.U(W.W))
  exMaReg    := i_saf(W - 1, 0)
  // Is zero?
  val isZero = exMaReg === 0.U
  dontTouch(isZero)
  // Sign
  val sign = exMaReg(W - 1)
  // Unsign mantissa
  val uMaReg  = RegInit(0.U(W.W))
  uMaReg     := Mux(sign, 1.U + ~exMaReg, exMaReg)
  // Find MSB to determine lshift
  val msbPos = W.U - PriorityEncoder(Reverse(uMaReg))
  dontTouch(msbPos)

  /* PIPELINED SHIFT */
  // Shift amount (theoretically 6 bits)
  val shamt = Wire(UInt(8.W))
  shamt    := W.U - msbPos - 1.U
  dontTouch(shamt)
  // Pipeline stage 1
  val mantShift1Reg = RegInit(0.U(W.W))
  val expt1Reg      = RegInit(0.U(8.W))
  mantShift1Reg    := exMaReg << (shamt(7, 4) << 4)
  expt1Reg         := reExReg << L
  // Pipeline stage 2
  val mantShift2Reg = RegInit(0.U(W.W))
  val expt2Reg      = RegInit(0.U(8.W))
  mantShift2Reg    := mantShift1Reg << RegNext(shamt(3, 0))
  expt2Reg         := expt1Reg + RegNext(msbPos) - 1.U - 23.U

  /* OUTPUT */
  // Force output to 0 after a reset
  val nZeroPipeRegs = RegInit(VecInit(Seq.fill(4)(false.B)))
  nZeroPipeRegs(0) := ~isZero
  for(i <- 1 to 3) {
    nZeroPipeRegs(i) := nZeroPipeRegs(i - 1)
  }

  o_ef32 := Mux(nZeroPipeRegs(3),
    Cat(expt2Reg, mantShift2Reg(W - 1, W - 25)),
    0.U(DW.W)
  )
}
