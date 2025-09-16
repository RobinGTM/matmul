/* SAFMul.scala -- SAF multiplier
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

import math.pow

// SAFMul takes an extended floating point format (with signed,
// expanded mantissa) and outputs a SAF
class SAFMul(
  DW                : Int = 33,
  L                 : Int = 5,
  W                 : Int = 81,
  DSP_PIPELINE_REGS : Int = 3
) extends Module {
  private val SAF_W = W + 8 - L
  private val EW = 8
  // Signed, extended mantissa width
  private val MW = 25
  require(W >= MW + (1 << L))
  // Float bias
  private val FB = 127
  val DELAY_TICKS = DSP_PIPELINE_REGS

  /* I/O */
  val i_a   = IO(Input(UInt(DW.W)))
  val i_b   = IO(Input(UInt(DW.W)))
  val o_saf = IO(Output(UInt(SAF_W.W)))

  /* INTERNALS */
  // Extract float subfields
  val exA = i_a(32, 25)
  // Get unsigned mantissae for multiplication
  val maA = Mux(i_a(24), 1.U + ~i_a(24, 0), i_a(24, 0))
  val exB = i_b(32, 25)
  val maB = Mux(i_b(24), 1.U + ~i_b(24, 0), i_b(24, 0))
  val sign = i_a(24) ^ i_b(24)
  // Zero and negative flags
  val eitherZero = (exA === 0.U & maA === 0.U) | (exB === 0.U & maB === 0.U)

  // Product exponent (9 bits)
  // Overflow: set exponent to all 1s (infinity)
  val exSum = (exA +& exB) - FB.U
  // printf(cf"${exSum}\n")
  // printf(cf"${exSum(8)}\n")
  val prodEx = ShiftRegister(Mux(exSum(8), Fill(8, 1.U), exSum(7, 0)), DSP_PIPELINE_REGS)
  // Product of mantissae (50 bits + max shift from low exponent bits)
  private val PROD_W = MW * 2 + (1 << L) - 1
  // Product
  val pr = ShiftRegister((maA(MW - 1, 0) * maB(MW - 1, 0))(2 * MW - 1, 0), DSP_PIPELINE_REGS)
  // Normalize and round
  // Mantissa width will be 2 * MW + 2^L - 1 - (MW - 1) after shifting
  // private val MANT_W = MW + (1 << L)
  val prod = Wire(SInt(W.W))
  prod    := Cat((pr >> (MW - 1)), pr(MW - 2, 0).orR).asSInt

  // Product reduced exponent
  val prodRe = prodEx(7, L)
  // Product exponent low bits (left-shift)
  val prodLs = prodEx(L - 1, 0)

  // Shift mantissa by low exponent bits
  val prodMa = Wire(UInt(W.W))
  prodMa    := prod.asUInt << prodLs

  // Re-sign mantissa
  val maOut = Mux(ShiftRegister(sign, DSP_PIPELINE_REGS), 1.U + ~prodMa, prodMa)

  // Output control
  when(ShiftRegister(eitherZero, DSP_PIPELINE_REGS)) {
    o_saf := 0.U
  } .otherwise {
    o_saf := Cat(prodRe, maOut)
  }
}
