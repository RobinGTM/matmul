/* safutils.scala -- SAF conversion and parsing utilities
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

package object utils {
  // Convert IEEE-754 binary32 to float with extended mantissa
  // ("expanded float")
  def expandF32(
    f32 : UInt,
  ) : UInt = {
    val sign       = f32(31)
    val expt       = f32(30, 23)
    val mant       = f32(22, 0)
    val expNonNull = ~(expt === 0.U)
    val isNonNull  = ~(mant === 0.U)
    // Expand mantissa
    val eMant = Wire(UInt(25.W))
    eMant    := Cat(expNonNull.asUInt, mant)
    // Sign mantissa
    val sMant = Wire(UInt(25.W))
    sMant    := Mux(sign, 1.U + ~eMant, eMant)
    val exF32 = Cat(f32(30, 23), sMant)
    exF32
  }

  // Convert float with extended mantissa ("expanded float") to
  // IEEE-754 binary32
  def restoreF32(
    expF32 : UInt
  ) : UInt = {
    // Exponent and extended mantissa
    val exp        = expF32(32, 25)
    val eMant      = expF32(24, 0)
    val isNegative = expF32(24)
    val isZero     = (exp === 0.U) && (eMant === 0.U)
    val expNonNull = ~(exp === 0.U)
    // Unsign mantissa
    val uMant = Mux(isNegative, 1.U + ~expF32(23, 0), expF32(23, 0))(23, 0)
    // Normalize mantissa
    val nMant = (uMant - (expNonNull << 23))(22, 0)
    val reF32 = Mux(isZero, 0.U(32.W), Cat(isNegative, exp, nMant))
    reF32
  }

  // Convert SAF float to expanded float
  def SAFToExpF32(
    saf : UInt,
    DW  : Int = 33,
    L   : Int = 5,
    W   : Int = 70,
  ) : UInt = {
    val SAF_W = W + 8 - L
    // Reduced exponent
    val reEx = saf(SAF_W - 1, SAF_W - (8 - L))
    // Extended mantissa
    val exMa = saf(W - 1, 0)
    // Sign
    val sign = exMa(W - 1)
    // Unsign mantissa
    val uMa = Mux(sign, 1.U + ~exMa, exMa)
    // Find MSB to determine lshift
    val msbPos = (W.U - PriorityEncoder(Reverse(uMa)))
    // Unshift mantissa
    val mant = (exMa << (W.U - msbPos - 1.U))(W - 1, W - 25)
    // printf(cf"${msbPos}<<<<<<<\n")
    // printf(cf"${reEx}<<<<<<<\n")
    // Recreate original exponent
    val expt = (reEx << L) + msbPos - 1.U - 23.U
    Mux(exMa === 0.U, 0.U(DW.W), Cat(expt, mant))
  }

  def floatToSAF(
    f   : Float,
    L   : Int = 5,
    W   : Int = 70,
    B   : Int = 150,
    L2N : Int = 16
  ) : String = {
    val bitString = String.format(
      "%32s", java.lang.Float.floatToIntBits(f).toBinaryString
    ).replace(' ', '0')
    // Extract IEEE-754 float32 fields
    val signStr = bitString(0) // .take to get a String and not a Char
    val sign    = Integer.parseInt(bitString(0).toString, 2)
    val exptStr = bitString.substring(1, 9)
    val expt    = Integer.parseInt(bitString.substring(1, 9), 2)
    val mantStr = bitString.substring(9, 32)
    val mant    = Integer.parseInt(bitString.substring(10, 32), 2)
    if(expt == 0 && mant == 0) {
      "0"
    } else {
      // Stolen from CuFP
      // https://github.com/FahimeHajizadeh/Custom-Float-HLS/blob/main/CuFPSAF/src/custom_float.h
      // in CuFPSAF
      val reStr = exptStr.substring(0, 8 - L)
      val lsh   = Integer.parseInt(exptStr.substring(8 - L, 8), 2)
      val mtStr = if(expt == 0) {
        "0".concat(mantStr).concat("0" * lsh)
      } else {
        "1".concat(mantStr).concat("0" * lsh)
      }
      val maStr = if(sign == 0) {
        String.format("%" + W + "s", mtStr).replace(' ', '0')
      } else {
        String.format(
          "%" + W + "s", (
            - scala.math.BigInt.javaBigInteger2bigInt(new java.math.BigInteger(mtStr, 2))
          ).toLong.toBinaryString
        ).replace(' ', '1')
      }
      reStr.concat(maStr)
    }
  }

  def floatToSAFUInt(
    f   : Float,
    L   : Int = 5,
    W   : Int = 70,
    B   : Int = 150,
    L2N : Int = 16
  ) : UInt = {
    ("b" + floatToSAF(f)).U
  }

  // Read matrix file in SAF format
  def readCSVSAF(
    filePath : String,
    L        : Int = 5,
    W        : Int = 70,
    B        : Int = 150,
    L2N      : Int = 16
  ) : Array[Array[String]] = {
    val source = scala.io.Source.fromFile(filePath)
    val data = source
      .getLines()
      .map { line =>
        line.split(" ").map(_.trim.toFloat)
      }.toArray
    source.close()
    data.map(_.map(elt => "b" + floatToSAF(elt)))
  }

  def readCSVFloat(
    filePath : String
  ) : Array[Array[String]] = {
    val source = scala.io.Source.fromFile(filePath)
    val data = source
      .getLines()
      .map { line =>
        line.split(" ").map(_.trim.toFloat)
      }.toArray
    source.close()
    data.map(_.map(elt => "b" + String.format(
      "%32s", java.lang.Float.floatToIntBits(elt).toBinaryString
    ).replace(' ', '0')))
  }
}
