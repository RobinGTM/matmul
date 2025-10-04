/* MACSpec.scala -- MAC test-bench
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
package mac

import chisel3._
import chisel3.util._

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

// https://github.com/edwardcwang/decoupled-serializer.git
// to generate VCD traces with chisel 6.6.0
import chisel3.simulator.VCDHackedEphemeralSimulator._

import mac.interfaces._
import hardfloat._
import saf.utils._

class MACTestWrapper(
  USE_HARDFLOAT     : Boolean = true,
  DW                : Int = 33,
  SAF_L             : Int = 5,
  SAF_W             : Int = 70,
  DSP_PIPELINE_REGS : Int = 2
) extends Module {
  val io = IO(new MACInterface(32))
  val mac = Module(new MAC(
    USE_HARDFLOAT, DW, SAF_L, SAF_W, DSP_PIPELINE_REGS
  ))

  if(USE_HARDFLOAT) {
    mac.io.i_a   := recFNFromFN(8, 24, io.i_a)
    mac.io.i_b   := recFNFromFN(8, 24, io.i_b)
  } else {
    mac.io.i_a   := expandF32(io.i_a)
    mac.io.i_b   := expandF32(io.i_b)
  }
  mac.io.i_acc := io.i_acc
  mac.io.i_rst := io.i_rst

  if(USE_HARDFLOAT) {
    io.o_res := fNFromRecFN(8, 24, mac.io.o_res)
  } else {
    io.o_res := restoreF32(mac.io.o_res)
  }
}

class MACSpec extends AnyFlatSpec with Matchers {
  val DSP_PIPELINE_REGS = 3
  val USE_HARDFLOAT = false
  val DW = 33
  val SAF_W = 70
  val SAF_L = 5

  "MAC" should "work" in {
    simulate(new MACTestWrapper(
      USE_HARDFLOAT = USE_HARDFLOAT, DW, SAF_L, SAF_W, DSP_PIPELINE_REGS
    )) { uut =>
      def print_outs(uut : MACTestWrapper) : Unit = {
        println("=========================")
        println(uut.io.o_res.peek())
      }

      uut.io.i_rst.poke(0)

      // -1 * 15.99999
      uut.io.i_a.poke(0xbf800000)
      uut.io.i_b.poke(0x417fffff)
      uut.io.i_acc.poke(true)
      uut.clock.step()
      uut.io.i_acc.poke(false)

      // 12321.132 * 10
      uut.io.i_a.poke(0x46408487)
      uut.io.i_b.poke(0x41200000)
      uut.io.i_acc.poke(true)
      uut.clock.step()
      uut.io.i_acc.poke(false)
      print_outs(uut)

      uut.clock.step(3)
      print_outs(uut)

      uut.clock.step(10)
    }
  }
}
