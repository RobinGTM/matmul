package saf

import chisel3._
import chisel3.util._

import math.pow

// SAFMul takes an extended floating point format (with signed,
// expanded mantissa) and outputs a SAF
class SAFMul(
  DW  : Int = 33,
  L   : Int = 5,
  W   : Int = 81
) extends Module {
  private val SAF_W = W + 8 - L
  private val EW = 8
  // Signed, extended mantissa width
  private val MW = 25
  require(W >= MW + (1 << L))
  // Float bias
  private val FB = 127

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
  val isNeg      = maA(24) ^ maB(24)

  // Product exponent (9 bits)
  // Overflow: set exponent to all 1s (infinity)
  val exSum = (exA +& exB) - FB.U
  // printf(cf"${exSum}\n")
  // printf(cf"${exSum(8)}\n")
  val prodEx = Mux(exSum(8), Fill(8, 1.U), exSum(7, 0))
  // Product of mantissae (50 bits + max shift from low exponent bits)
  private val PROD_W = MW * 2 + (1 << L) - 1
  // Product
  val pr = (maA(MW - 1, 0) * maB(MW - 1, 0))(2 * MW - 1, 0)
  // Normalize and round
  // Mantissa width will be 2 * MW + 2^L - 1 - (MW - 1) after shifting
  // private val MANT_W = MW + (1 << L)
  val prod = Wire(SInt(W.W))
  prod    := Cat((pr >> (MW - 1)), pr(MW - 2, 0).orR).asSInt

  // printf(cf"maA: ${maA} maB: ${maB}\n");

  // Product reduced exponent
  val prodRe = prodEx(7, L)
  // Product exponent low bits (left-shift)
  val prodLs = prodEx(L - 1, 0)

  // Shift mantissa by low exponent bits
  val prodMa = Wire(UInt(W.W))
  prodMa    := prod.asUInt << prodLs

  // Re-sign mantissa
  val maOut = Mux(sign, 1.U + ~prodMa, prodMa)

  // Output control
  when(eitherZero) {
    o_saf := 0.U
  } .otherwise {
    o_saf := Cat(prodRe, maOut)
  }
}
