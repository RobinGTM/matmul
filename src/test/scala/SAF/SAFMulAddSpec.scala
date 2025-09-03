/* SAFMulAddSpec.scala -- Test-bench for validating SAF multiplication
 *                        and addition
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

class SAFAddSpec extends AnyFlatSpec with Matchers {
  "SAFAdd" should "work" in {
    simulate(new SAFAdder(
      L = 5,
      W = 70
    )) { uut =>
      // -1 + 1
      uut.i_safA.poke("b0111111111111111111000000000000000000000000000000000000000000000000000000".U)
      uut.i_safB.poke("b0110000000000000001000000000000000000000000000000000000000000000000000000".U)
      println(uut.o_res.peek())

      // 15.999999 + 10
      uut.i_safA.poke("b1000000000000000000000000000000000000000000000011111111111111111111111100".U)
      uut.i_safB.poke("b1000000000000000000000000000000000000000000000010100000000000000000000000".U)
      println(uut.o_res.peek())

      // -10.78125 + 10
      uut.i_safA.poke("b1001111111111111111111111111111111111111111111101010011100000000000000000".U)
      uut.i_safB.poke("b1000000000000000000000000000000000000000000000010100000000000000000000000".U)
      println(uut.o_res.peek())

      // 228004.2343233 + 3213212.34123
      uut.i_safA.poke("b1000000000000000000000000000000001101111010101001000011110000000000000000".U)
      uut.i_safB.poke("b1000000000000000000000000000011000100000111100111000100000000000000000000".U)
      println(uut.o_res.peek())

      // 65112.3685891 + 1226.39240456
      uut.i_safA.poke("b0111111111001011000010111100101101111011011000000100000000000000000000000".U)
      uut.i_safB.poke("b0110000010011001010011001000111010010100000000000000000000000000000000000".U)
      println(uut.o_res.peek())
    }
  }
}

// class SAFMulWrapper(
//   L : Int = 5,
//   SAF_W : Int = 70 + 8 - 5
// ) extends Module {
//   val i_a = IO(Input(UInt(32.W)))
//   val i_b = IO(Input(UInt(32.W)))
//   val fb_a = IO(Output(UInt(33.W)))
//   val fb_b = IO(Output(UInt(33.W)))
//   val o_saf = IO(Output(UInt(SAF_W.W)))
//   val o_f32 = IO(Output(UInt(32.W)))

//   val mul = Module(new SAFMul(L = L, W = 70))
//   val saf2F32 = Module(new SAFToFloat32(L = L, W = 70))

//   mul.i_a := expandF32(i_a)
//   fb_a    := expandF32(i_a)
//   mul.i_b := expandF32(i_b)
//   fb_b    := expandF32(i_b)

//   saf2F32.i_saf := mul.o_saf

//   o_saf := mul.o_saf
//   o_f32 := saf2F32.o_f32
// }

// class SAFMulSpec extends AnyFlatSpec with Matchers {
//   "SAFMul" should "work" in {
//     simulate(new SAFMulWrapper) { uut =>
//       // -1 * 1
//       println("==================")
//       uut.i_a.poke(0xbf800000)
//       uut.i_b.poke(0x3f800000)
//       println(uut.fb_a.peek())
//       println(uut.fb_b.peek())
//       println(uut.o_saf.peek())
//       println(uut.o_f32.peek())

//       println("==================")
//       uut.clock.step()
//       // 15.999999 * 10
//       uut.i_a.poke(0x417fffff)
//       uut.i_b.poke(0x41200000)
//       println(uut.fb_a.peek())
//       println(uut.fb_b.peek())
//       println(uut.o_saf.peek())
//       println(uut.o_f32.peek())

//       println("==================")
//       uut.clock.step()
//       // -10.78125 * 10
//       uut.i_a.poke(0xc12c8000)
//       uut.i_b.poke(0x41200000)
//       println(uut.fb_a.peek())
//       println(uut.fb_b.peek())
//       println(uut.o_saf.peek())
//       println(uut.o_f32.peek())

//       println("==================")
//       uut.clock.step()
//       uut.i_a.poke(0xc8756f08)
//       uut.i_b.poke(0x43a09d91)
//       println(uut.fb_a.peek())
//       println(uut.fb_b.peek())
//       println(uut.o_saf.peek())
//       println(uut.o_f32.peek())

//       uut.clock.step()
//     }
//   }
// }

class SAFMulAdd extends Module {
  val i_a = IO(Input(UInt(33.W)))
  val i_b = IO(Input(UInt(33.W)))
  val i_c = IO(Input(UInt(33.W)))
  val i_d = IO(Input(UInt(33.W)))
  val o_res = IO(Output(UInt(73.W)))

  val mul1 = Module(new SAFMul(33, 5, 70))
  val mul2 = Module(new SAFMul(33, 5, 70))
  mul1.i_a := expandF32(i_a)
  mul1.i_b := expandF32(i_b)
  mul2.i_a := expandF32(i_c)
  mul2.i_b := expandF32(i_d)

  val add = Module(new SAFAdder(5, 70))
  add.i_safA := mul1.o_saf
  add.i_safB := mul2.o_saf

  o_res := add.o_res
}

class SAFMulAddSpec extends AnyFlatSpec with Matchers {
  "SAFMAC" should "work" in {
    simulate(new SAFMulAdd) { uut =>
      // -1 * 1 + 15.99999 * 10
      uut.i_a.poke(0xbf800000)
      uut.i_b.poke(0x3f800000)
      uut.i_c.poke(0x417fffff)
      uut.i_d.poke(0x41200000)
      uut.clock.step()
      println(uut.o_res.peek())
    }
  }
}
