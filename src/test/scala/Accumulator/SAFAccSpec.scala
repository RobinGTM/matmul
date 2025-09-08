/* SAFAccSpec.scala -- SAF accumulator test-bench
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

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

// https://github.com/edwardcwang/decoupled-serializer.git
// to generate VCD traces with chisel 6.6.0
import chisel3.simulator.VCDHackedEphemeralSimulator._

import saf._
import saf.utils._
import acc.interfaces._

class SAFAccTest(
  W : Int,
  L : Int
) extends Module {
  private val SAF_WIDTH = W + 8 - L
  val i_saf = IO(Input(UInt(SAF_WIDTH.W)))
  val i_acc = IO(Input(Bool()))
  val i_rst = IO(Input(Bool()))
  // Output float
  val o_res = IO(Output(UInt(32.W)))

  val safAcc = Module(new SAFAcc(L, W))
  safAcc.io.i_acc := i_acc
  safAcc.io.i_rst := i_rst
  safAcc.io.i_in  := i_saf
  o_res           := restoreF32(SAFToExpF32(safAcc.io.o_res, 33, L, W))
}

class SAFAccSpec extends AnyFlatSpec with Matchers {
  "SAFAcc" should "work" in {
    simulate(new SAFAccTest(70, 5)) { uut =>
      def print_outs(uut : SAFAccTest) : Unit = {
        println("=========================")
        println(uut.o_res.peek())
      }

      uut.i_rst.poke(false)

      uut.i_acc.poke(true)
      uut.i_saf.poke(floatToSAFUInt(15.05F, 5, 70))
      uut.clock.step()
      uut.i_acc.poke(true)
      uut.i_saf.poke(floatToSAFUInt(10.0F, 5, 70))
      uut.clock.step()
      uut.i_acc.poke(true)
      uut.i_saf.poke(floatToSAFUInt(-5.06F, 5, 70))
      uut.clock.step()
      uut.i_acc.poke(true)
      uut.i_saf.poke(floatToSAFUInt(0.322F, 5, 70))
      uut.clock.step()

      uut.i_acc.poke(false)

      uut.clock.step(10)
    }
  }
}

// class SAFMACSpec extends AnyFlatSpec with Matchers {
//   "SAFMAC" should "work" in {
//     simulate(new SAFMACTest(
//       33, 70, 5
//     )) { uut =>
//       def print_outs(uut : SAFMACTest) : Unit = {
//         println("=========================")
//         println(uut.io.o_res.peek())
//         println(uut.o_saf.peek())
//       }

//       uut.io.i_rst.poke(0)

//       // -1 * 15.99999
//       uut.io.i_a.poke(0xbf800000)
//       uut.io.i_b.poke(0x417fffff)
//       uut.io.i_acc.poke(true)
//       uut.clock.step()
//       print_outs(uut)

//       // 12321.132 * 10
//       uut.io.i_a.poke(0x46408487)
//       uut.io.i_b.poke(0x41200000)
//       uut.io.i_acc.poke(true)
//       uut.clock.step()
//       print_outs(uut)

//       // 123.321 * -65543.435
//       uut.io.i_a.poke(0x42f6a45a)
//       uut.io.i_b.poke(0xc78003b8)
//       uut.io.i_acc.poke(true)
//       uut.clock.step()
//       print_outs(uut)

//       uut.io.i_acc.poke(false)
//       uut.clock.step()
//       print_outs(uut)

//       uut.clock.step(10)
//       print_outs(uut)
//     }
//   }
// }
