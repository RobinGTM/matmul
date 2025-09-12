/* MatMulCoreSpec.scala -- matmul core test-bench
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
package matmul

import chisel3._
import chisel3.util._

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

// https://github.com/edwardcwang/decoupled-serializer.git
// to generate VCD traces with chisel 6.6.0
import chisel3.simulator.VCDHackedEphemeralSimulator._

import matmul.utils._
import matmul.interfaces._
import matmul.testutils._
import saf.utils._
import hardfloat._

class MatMulCoreTest(
  PARAM : Parameters
) extends Module {
  val i = IO(Input(new MatMulInterface(32)))
  val o = IO(Output(new MatMulInterface(32)))
  val core = Module(new MatMulCore(PARAM))

  if(PARAM.USE_HARDFLOAT) {
    core.i.data := recFNFromFN(8, 24, i.data)
  } else {
    core.i.data := expandF32(i.data)
  }
  core.i.valid := i.valid
  core.i.prog  := i.prog
  core.i.ready := i.ready

  if(PARAM.USE_HARDFLOAT) {
    o.data := fNFromRecFN(8, 24, core.o.data)
  } else {
    o.data := restoreF32(core.o.data)
  }
  o.valid := core.o.valid
  o.prog  := core.o.prog
  o.ready := core.o.ready
}

class MatMulCoreSpec extends AnyFlatSpec with Matchers {
  val file = "src/test/resources/dummy10x5-matrix2308555.txt"
  val MH = 10
  val MW = 5
  val USE_HARDFLOAT = true
  val PIPELINE_DEPTH = 4
  val DSP_DEPTH = 4

  "MatMul" should "work" in {
    simulate(new MatMulCoreTest(
      PARAM = new Parameters(Array(
        "-h", s"${MH}", "-w", s"${MW}",
        "-ppd", s"${PIPELINE_DEPTH}",
        "-mpd", s"${DSP_DEPTH}",
        "-hf", s"${USE_HARDFLOAT}"
      ))
    )) { uut =>
      val matrix = readCSVFloat(file)

      uut.reset.poke(true)
      uut.clock.step(3)
      uut.reset.poke(false)
      uut.clock.step()

      // Prog sequence
      for(i <- 0 to MH - 1) {
        for(j <- 0 to MW - 1) {
          if(i === MH / 2 && j === 3) {
            uut.i.valid.poke(false)
            uut.clock.step(10)
          }
          uut.i.data.poke(matrix(i)(j).U)
          uut.i.valid.poke(true)
          uut.i.prog.poke(true)
          uut.clock.step()
          if(j == 5) {
            uut.i.valid.poke(false)
            uut.i.prog.poke(false)
            uut.clock.step(10)
          }
        }
      }
      uut.i.valid.poke(false)
      uut.i.prog.poke(false)

      // uut.clock.step()

      val vec10x5 = (for(i <-
        Array(2.024726, 0.999403, 1.529937, 0.536318, 0.745846)
      ) yield {
        floatToBitsUInt(i.toFloat)
      })

      for(i <- 0 to MW - 1) {
        if(i == MW - 1) {
          uut.i.data.poke(0.U)
          uut.i.valid.poke(false)
          uut.clock.step()
        }
        uut.i.data.poke(vec10x5(i))
        uut.i.valid.poke(true)
        uut.clock.step()
      }

      for(i <- 0 to 99) {
        for(i <- 0 to MW - 1) {
          uut.i.data.poke(vec10x5(i))
          uut.i.valid.poke(true)
          uut.clock.step()
        }
        // while(!uut.o.ready.peek().litToBoolean) {
          // uut.clock.step()
        // }
        // uut.i.data.poke(0)
        // uut.i.valid.poke(false)
        // uut.clock.step()
      }

      uut.clock.step()

      while(!uut.o.ready.peek().litToBoolean) {
        uut.clock.step()
      }

      for(i <- 0 to MW - 1) {
        uut.i.data.poke(floatToBitsUInt((- i - 1).toFloat))
        uut.i.valid.poke(true)
        uut.clock.step()
      }
      uut.i.data.poke(0)
      uut.i.valid.poke(false)

      while(!uut.o.ready.peek().litToBoolean) {
        uut.clock.step()
      }
      for(i <- 0 to MW - 1) {
        uut.i.data.poke(floatToBitsUInt((- i - 1).toFloat))
        uut.i.valid.poke(true)
        uut.clock.step()
      }
      uut.i.data.poke(0)
      uut.i.valid.poke(false)

      for(i <- 0 to 10)
      {
        while(!uut.o.ready.peek().litToBoolean) {
          uut.clock.step()
        }
        for(i <- 0 to MW - 1) {
          uut.i.data.poke(floatToBitsUInt((- i - 1).toFloat))
          if(i == 3) {
            uut.i.valid.poke(false)
            uut.clock.step(40)
          }
          uut.i.valid.poke(true)
          uut.clock.step()
        }
        uut.i.data.poke(0)
        uut.i.valid.poke(false)
      }

      while(!uut.o.ready.peek().litToBoolean) {
        uut.clock.step()
      }
      uut.clock.step(10)
    }
  }
}
