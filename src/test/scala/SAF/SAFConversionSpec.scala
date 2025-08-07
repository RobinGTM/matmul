/* SAFConversionSpec.scala -- A SAF conversion tester / generator
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

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

// https://github.com/edwardcwang/decoupled-serializer.git
// to generate VCD traces with chisel 6.6.0
import chisel3.simulator.VCDHackedEphemeralSimulator._

import saf.utils._

class Float32ToSAFSpec extends AnyFlatSpec with Matchers {
  "Float32ToSAF" should "work" in {
    simulate(new Float32ToSAF(L = 4)) { uut =>
      // 0
      uut.i_f32.poke(0)
      println(s"${uut.o_saf.peek()}")

      // -1
      uut.i_f32.poke("b10111111100000000000000000000000".U)
      println(s"${uut.o_saf.peek()}")

      // 1
      uut.i_f32.poke("b0111111100000000000000000000000".U)
      println(s"${uut.o_saf.peek()}")

      // 2
      uut.i_f32.poke("b01000000000000000000000000000000".U)
      println(s"${uut.o_saf.peek()}")

      // 3
      uut.i_f32.poke("b01000000010000000000000000000000".U)
      println(s"${uut.o_saf.peek()}")

      // 0.0123
      uut.i_f32.poke("b00111100010010011000010111110000".U)
      println(s"${uut.o_saf.peek()}")

      // -0.0123
      uut.i_f32.poke("b10111100010010011000010111110000".U)
      println(s"${uut.o_saf.peek()}")

      // 5
      uut.i_f32.poke("b01000000101000000000000000000000".U)
      println(s"${uut.o_saf.peek()}")

      // 10.78125
      uut.i_f32.poke("b01000001001011001000000000000000".U)
      println(s"${uut.o_saf.peek()}")

      // -10.78125
      uut.i_f32.poke("b11000001001011001000000000000000".U)
      println(s"${uut.o_saf.peek()}")

      // 10
      uut.i_f32.poke("b01000001001000000000000000000000".U)
      println(s"${uut.o_saf.peek()}")

      // 15.999999
      uut.i_f32.poke("b01000001011111111111111111111111".U)
      println(s"${uut.o_saf.peek()}")

      // 3.4028235e38
      uut.i_f32.poke("b01111111011111111111111111111111".U)
      println(s"${uut.o_saf.peek()}")

      // -3.4028235e38
      uut.i_f32.poke("b11111111011111111111111111111111".U)
      println(s"${uut.o_saf.peek()}")

      // 228004.2343233
      uut.i_f32.poke("b01001000010111101010100100001111".U)
      println(s"${uut.o_saf.peek()}")

      // 3213212.34123
      uut.i_f32.poke("b01001010010001000001111001110001".U)
      println(s"${uut.o_saf.peek()}")

      // 0x1000000008C08000000
      // uut.i_f32.poke("h1000000008C08000000".U)
      // println(s"${uut.o_saf.peek()}")
    }
  }
}

class SAFToFloat32Spec extends AnyFlatSpec with Matchers {
  "SAFToFloat32" should "work" in {
    simulate(new SAFToFloat32) { uut =>
      // 0
      uut.i_saf.poke(0)
      println(s"${uut.o_f32.peek()}")

      // -1
      uut.i_saf.poke("b0111111111111111111000000000000000000000000000000000000000000000000000000".U)
      println(s"${uut.o_f32.peek()}")
      // println(s"${uut.msbPos.peek()}")

      // 0.123
      uut.i_saf.poke("b0110000000000000000000000110010011000010111110000000000000000000000000000".U)
      println(s"${uut.o_f32.peek()}")

      // -0.123
      uut.i_saf.poke("b0111111111111111111111111001101100111101000010000000000000000000000000000".U)
      println(s"${uut.o_f32.peek()}")

      // 5
      uut.i_saf.poke("b1000000000000000000000000000000000000000000000001010000000000000000000000".U)
      println(s"${uut.o_f32.peek()}")

      // 10.78125
      uut.i_saf.poke("b1000000000000000000000000000000000000000000000010101100100000000000000000".U)
      println(s"${uut.o_f32.peek()}")

      // -10.78125
      uut.i_saf.poke("b1001111111111111111111111111111111111111111111101010011100000000000000000".U)
      println(s"${uut.o_f32.peek()}")

      // 10
      uut.i_saf.poke("b1000000000000000000000000000000000000000000000010100000000000000000000000".U)
      println(s"${uut.o_f32.peek()}")

      // 15.999999
      uut.i_saf.poke("b1000000000000000000000000000000000000000000000011111111111111111111111100".U)
      println(s"${uut.o_f32.peek()}")

      // 3.4028235e38
      uut.i_saf.poke("b1110000000000000000111111111111111111111111000000000000000000000000000000".U)
      println(s"${uut.o_f32.peek()}")

      // -3.4028235e38
      uut.i_saf.poke("b1111111111111111111000000000000000000000001000000000000000000000000000000".U)
      println(s"${uut.o_f32.peek()}")
    }
  }
}

class SAFConversionTest extends Module {
  val i_f32 = IO(Input(UInt(32.W)))
  val o_f32 = IO(Output(UInt(32.W)))
  val f32Saf = Module(new Float32ToSAF)
  val safF32 = Module(new SAFToFloat32)
  f32Saf.i_f32 := i_f32
  safF32.i_saf := f32Saf.o_saf
  o_f32        := safF32.o_f32
}

class SAFConversionSpec extends AnyFlatSpec with Matchers {
  "SAFConversions" should "work" in {
    simulate(new SAFConversionTest) { uut =>
      // 0
      uut.i_f32.poke(0)
      println(s"${uut.o_f32.peek()}")

      // -1
      uut.i_f32.poke("b10111111100000000000000000000000".U)
      println("b10111111100000000000000000000000".U)
      println(s"${uut.o_f32.peek()}")
      println("========================================")

      // 0.0123
      uut.i_f32.poke("b00111100010010011000010111110000".U)
      println("b00111100010010011000010111110000".U)
      println(s"${uut.o_f32.peek()}")
      println("========================================")

      // -0.0123
      uut.i_f32.poke("b10111100010010011000010111110000".U)
      println("b10111100010010011000010111110000".U)
      println(s"${uut.o_f32.peek()}")
      println("========================================")

      // 5
      uut.i_f32.poke("b01000000101000000000000000000000".U)
      println("b01000000101000000000000000000000".U)
      println(s"${uut.o_f32.peek()}")
      println("========================================")

      // 10.78125
      uut.i_f32.poke("b01000001001011001000000000000000".U)
      println("b01000001001011001000000000000000".U)
      println(s"${uut.o_f32.peek()}")
      println("========================================")

      // -10.78125
      uut.i_f32.poke("b11000001001011001000000000000000".U)
      println("b11000001001011001000000000000000".U)
      println(s"${uut.o_f32.peek()}")
      println("========================================")

      // 10
      uut.i_f32.poke("b01000001001000000000000000000000".U)
      println("b01000001001000000000000000000000".U)
      println(s"${uut.o_f32.peek()}")
      println("========================================")

      // 15.999999
      uut.i_f32.poke("b01000001011111111111111111111111".U)
      println("b01000001011111111111111111111111".U)
      println(s"${uut.o_f32.peek()}")
      println("========================================")

      // 3.4028235e38
      uut.i_f32.poke("b01111111011111111111111111111111".U)
      println("b01111111011111111111111111111111".U)
      println(s"${uut.o_f32.peek()}")
      println("========================================")

      // -3.4028235e38
      uut.i_f32.poke("b11111111011111111111111111111111".U)
      println("b11111111011111111111111111111111".U)
      println(s"${uut.o_f32.peek()}")
      println("========================================")
    }
  }
}

class SAFToExpF32Test(
  L : Int = 5,
  W : Int = 70,
) extends Module {
  val i_f32 = IO(Input(UInt(32.W)))
  val o_exp = IO(Output(UInt(33.W)))
  val o_saf = IO(Output(UInt((W + 8 - L).W)))
  val o_exp_from_saf = IO(Output(UInt(33.W)))
  val o_restored_f32 = IO(Output(UInt(32.W)))

  val f32Saf = Module(new Float32ToSAF)

  f32Saf.i_f32 := i_f32
  o_saf := f32Saf.o_saf

  o_exp := expandF32(i_f32)

  o_exp_from_saf := SAFToExpF32(f32Saf.o_saf)

  o_restored_f32 := restoreF32(o_exp)
}

class SAFToExpF32TestSpec extends AnyFlatSpec with Matchers {
  "Exp conversions" should "work" in {
    simulate(new SAFToExpF32Test(L = 5, W = 70)) { uut =>
      for(f <- Array(0x41200000, 0xc8756f08)) {
        println(s"====== ${BigInt(f)} ======")
        uut.i_f32.poke(f)
        println(s"expF32: ${uut.o_exp.peek()}")
        println(s"SAF   : ${uut.o_saf.peek()}")
        println(s"EfSAF : ${uut.o_exp_from_saf.peek()}")
        println(s"REST  : ${uut.o_restored_f32.peek()}")
        uut.clock.step()
      }
    }
  }
}
