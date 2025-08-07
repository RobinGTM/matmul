/* WorkerSpec.scala -- Worker module test-bench
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
package matmul.worker

import chisel3._
import chisel3.util._

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

// https://github.com/edwardcwang/decoupled-serializer.git to generate
// VCD traces with chisel 6.6.0
import chisel3.simulator.VCDHackedEphemeralSimulator._

import math.pow

import matmul.utils._
import saf.utils._
import matmul.testutils._

class WorkerSpec extends AnyFlatSpec with Matchers {
  val MH = 16
  val MW = 16
  val USE_HARDFLOAT = true
  val DW = if(USE_HARDFLOAT) {
    32
  } else {
    73
  }

  "Worker" should "work_lol" in {
    simulate(new Worker(
      PARAM = new Parameters(
        Array("-h", s"${MH}", "-w", s"${MW}", "-hf", s"${USE_HARDFLOAT}")
      )
    )) { uut =>
      uut.reset.poke(true)
      uut.clock.step(3)
      uut.reset.poke(false)

      val ID = 0

      // Last block only receives its own data
      uut.wid.poke(ID)
      for(i <- 1 to MW * MH - 16 * ID) {
        val coeff = pow(-1, ((i + 1) % 2)).toFloat * i.toFloat
        if(USE_HARDFLOAT) {
          uut.i.data.poke(floatToBitsUInt(coeff))
          //   ("b" + String.format(
          //     "%32s", java.lang.Float.floatToIntBits(coeff).toBinaryString
          //   ).replace(' ', '0')).U
          // )
        } else {
          uut.i.data.poke(
            ("b" + floatToSAF(coeff)).U
          )
        }
        // uut.i.data.poke(i)
        uut.i.valid.poke(true)
        uut.i.prog.poke(true)
        uut.clock.step()
      }
      uut.i.prog.poke(false)
      uut.i.valid.poke(false)

      uut.clock.step(10)

      // Send vector
      for(i <- 1 to MW) {
        val v = 10 * i.toFloat
        if(USE_HARDFLOAT) {
          uut.i.data.poke(
            ("b" + String.format(
              "%32s", java.lang.Float.floatToIntBits(v).toBinaryString
            ).replace(' ', '0')).U
          )
        } else {
          uut.i.data.poke(("b" + floatToSAF(v)).U)
        }
        uut.i.valid.poke(true)
        uut.clock.step()
        if(i == 5) {
          uut.i.valid.poke(false)
          uut.clock.step(5)
        }
      }
      uut.i.valid.poke(false)
      // uut.clock.step(10)

      // uut.clock.step(16)

      // Write command
      for(i <- 1 to ID) {
        uut.i.write.poke(true)
        uut.i.valid.poke(true)
        uut.i.data.poke(i)
        uut.clock.step()
      }
      uut.i.write.poke(false)
      uut.i.valid.poke(false)
      uut.i.data.poke(0xffff)

      uut.clock.step(16)
    }
  }
}
