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
  val maA = i_a(24, 0).asSInt
  val exB = i_b(32, 25)
  val maB = i_b(24, 0).asSInt
  // Zero and negative flags
  val eitherZero = (exA === 0.U & maA === 0.S) | (exB === 0.U & maB === 0.S)
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
  val pr   = Wire(SInt(PROD_W.W))
  pr      := maA * maB
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
  // printf(cf"Shift: ${prodLs}\n");

  // printf(cf"${prodRe}<<<<<<<<\n")
  // printf(cf"${prodMa}<<<<<<<<\n")

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
