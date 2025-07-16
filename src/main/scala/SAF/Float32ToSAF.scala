package saf

// Chisel
import chisel3._
import chisel3.util._

// Scala
import math.{pow,ceil}

import chisel3._
import chisel3.util._

// IEEE-754 float32 to prime SAF converter
class Float32ToSAF(
  L   : Int = 5,   // Bits discarded from the exponent
  W   : Int = 70,  // Width of the extended mantissa (must be greater than 23 + 2 + 2^L)
  L2N : Int = 16   // log2(N)
) extends RawModule {
  private val DW = 32
  private val EW = 8
  private val MW = 23
  // Mantissa width must be greater than extended, signed mantissa
  // width (25) + maximum possible left shift (2^L)
  require(W >= (MW + 2 + (1 << L)))

  /* I/O */
  val i_f32 = IO(Input(UInt(32.W)))
  // Output width is we(8) - L + 2^L - 1 + wm(23) + 2(sign bit and hidden bit)
  // + log2(N) = W + 8 - L
  val o_saf = IO(Output(UInt((W + 8 - L).W)))

  /* CONVERSION*/
  // Unpack
  val sign = i_f32(31)
  val expt = i_f32(30, 23)
  val mant = i_f32(22, 0)
  val zero = (mant === 0.U) & (expt === 0.U)

  // Reduced exponent and mantissa computation
  // Reduced exponent
  val reEx = expt(EW - 1, L)
  // Exponent's low bits
  val loEx = expt(L - 1, 0)
  // Extended mantissa
  val eMa  = Wire(UInt(W.W))
  // Shift extended mantissa
  eMa     := Mux(expt === 0.U,
    Cat(0.U(1.W), mant) << loEx,
    Cat(1.U(1.W), mant) << loEx
  )
  // Extended, Signed mantissa
  val esMa = Wire(UInt(W.W))
  // 2s complement if negative
  when(sign.asBool) {
    esMa  := 1.U + (~eMa)
  } .otherwise {
    esMa  := eMa
  }

  // Conversion to prime SAF
  // Find the MSB: XOR with sign bit
  val xOrS = esMa ^ Fill(W, esMa(W - 1))
  // Find the position of the MSB of the xOrS vector
  val msbPos = (W.U - (~ esMa(W - 1).asBool).asUInt) - PriorityEncoder(Reverse(xOrS))

  // Prime mantissa and exponent
  val prEx = Wire(UInt((EW - L).W))
  val prMa = Wire(UInt(W.W))
  val shiftAmount = Wire(UInt(log2Up(W).W))
  when(msbPos > (MW + pow(2, L).toInt - 1).U) {
    // If MSB is in the log2(N) supplementary bits, shift right to bring MSB in
    // esMa(MW + 2^L - 1, MW), so shift by 1 + (msbpos - MW + 2^L - 1) // 2^L
    shiftAmount := (1.U + ((msbPos - (MW + pow(2, L).toInt - 1).U) >> L))
    prMa := (esMa.asSInt >> (shiftAmount << L)).asUInt
    prEx := reEx + shiftAmount
  } .elsewhen(msbPos < (MW - 1).U) {
    // If MSB is in the WM bits, shift left to put it in the 2^L top bits
    shiftAmount := (((MW - 1).U - msbPos) >> L) + 1.U
    prMa := esMa << (shiftAmount << L)
    prEx := reEx - shiftAmount
  } .otherwise {
    shiftAmount := 0.U
    // Otherwise, nothing to do
    prMa := esMa
    prEx := reEx
  }

  // Output
  when(zero) {
    o_saf := 0.U
  } .otherwise {
    o_saf := Cat(prEx, prMa)
  }
}
