package saf

import chisel3._
import chisel3.util._

import math.pow

// SAFMul takes an extended floating point format (with signed,
// expanded mantissa) and outputs a SAF
class SAFMul(
  DW  : Int = 33,
  L   : Int = 5,
  W   : Int = 81,
  B   : Int = 173,
  L2N : Int = 16
) extends Module {
  // + 1 because of exponent summation
  private val SAF_W = W + 8 - L + 1
  private val EW = 8
  // Signed mantissa width
  private val MW = 25
  // Float bias
  private val FB = 127

  /* I/O */
  val i_a   = IO(Input(UInt(DW.W)))
  val i_b   = IO(Input(UInt(DW.W)))
  val o_saf = IO(Output(UInt(SAF_W.W)))

  /* INTERNALS */
  // Extract float subfields
  val exA = i_a(32, 25)
  val maA = i_a(24, 0).asSInt
  val exB = i_b(32, 25)
  val maB = i_b(24, 0).asSInt
  // Zero and negative flags
  val eitherZero = (exA === 0.U & maA === 0.S) | (exB === 0.U & maB === 0.S)
  val isNeg      = maA(24) ^ maB(24)

  // Product exponent (9 bits)
  val prodEx = (exA +& exB) - FB.U
  // Product of mantissae (50 bits + max shift from low exponent bits)
  private val MANT_W = 50 + (1 << L) - 1
  val prod   = Wire(SInt(MANT_W.W))
  prod      := maA * maB

  // printf(cf"maA: ${maA} maB: ${maB}\n");

  // Product reduced exponent (9 - L bits because of exponent addition)
  val prodRe = prodEx(8, L)
  // Product exponent low bits (left-shift)
  val prodLs = prodEx(L - 1, 0)

  // Shift mantissa by low exponent bits
  val prodMa = Wire(UInt(MANT_W.W))
  prodMa    := prod.asUInt << prodLs
  // printf(cf"Shift: ${prodLs}\n");

  // // Conversion to prime SAF
  // // Find the MSB: XOR with sign bit
  // val xOrS = prodMa.asUInt ^ Fill(W, prodMa(MANT_W - 1))
  // // Find the position of the MSB of the xOrS vector
  // val msbPos = (MANT_W.U - (~ prodMa(W - 1).asBool).asUInt) - PriorityEncoder(Reverse(xOrS))

  // // Prime mantissa and exponent
  // val prEx = Wire(UInt((EW - L).W))
  // val prMa = Wire(UInt(W.W))
  // val shiftAmount = Wire(UInt(log2Up(MANT_W).W))
  // when(msbPos > (MW + pow(2, L).toInt).U) {
  //   // If MSB is in the log2(N) supplementary bits, shift right to bring MSB in
  //   // esMa(MW + 2^L - 1, MW), so shift by 1 + (msbpos - MW + 2^L - 1) // 2^L
  //   shiftAmount := (1.U + ((msbPos - (MW + pow(2, L).toInt).U) >> L))
  //   prMa := (prodMa >> (shiftAmount << L)).asUInt
  //   prEx := (prodEx + shiftAmount).asUInt
  // } .elsewhen(msbPos < (MW - 1).U) {
  //   // If MSB is in the WM bits, shift left to put it in the 2^L top bits
  //   shiftAmount := (((MW - 1).U - msbPos) >> L) + 1.U
  //   prMa := (prodMa << (shiftAmount << L)).asUInt
  //   prEx := (prodEx - shiftAmount).asUInt
  // } .otherwise {
  //   shiftAmount := 0.U
  //   // Otherwise, nothing to do
  //   prMa := prodMa.asUInt
  //   prEx := prodEx.asUInt
  // }

  when(eitherZero) {
    o_saf := 0.U
  } .otherwise {
    // o_saf := Cat(prEx, prMa)
    o_saf := Cat(prodRe, prodMa)
    // o_saf := prodRe
  }
}
