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

import matmul.worker.interfaces._
import matmul.utils._
import saf.utils._
import matmul.testutils._
import hardfloat.{recFNFromFN,fNFromRecFN}

class WorkerTestWrapper(
  PARAM : Parameters
) extends Module {
  val wid = IO(Input(UInt(log2Up(PARAM.M_HEIGHT).W)))
  val i   = IO(Input(new WorkerInterface(32)))
  val o   = IO(Output(new WorkerInterface(32)))
  val wk  = Module(new Worker(PARAM))
  wk.wid := wid
  
  if(PARAM.USE_HARDFLOAT) {
    wk.i.data := recFNFromFN(8, 24, i.data)
  } else {
    wk.i.data := expandF32(i.data)
  }
  wk.i.prog  := i.prog
  wk.i.write := i.write
  wk.i.valid := i.valid

  if(PARAM.USE_HARDFLOAT) {
    o.data := fNFromRecFN(8, 24, wk.o.data)
  } else {
    o.data := restoreF32(wk.o.data)
  }
  o.prog  := wk.o.prog
  o.write := wk.o.write
  o.valid := wk.o.valid
}

class WorkerSpec extends AnyFlatSpec with Matchers {
  val MH = 16
  val MW = 16
  val USE_HARDFLOAT = false
  val DSP_PIPELINE_REGS = 3

  "Worker" should "work_lol" in {
    simulate(new WorkerTestWrapper(
      PARAM = new Parameters(
        Array(
          "-h", s"${MH}", "-w", s"${MW}",
          "-mpd", s"${DSP_PIPELINE_REGS}"//,
          // "-hf"//, s"${USE_HARDFLOAT}"
        ))
    )) { uut =>
      uut.reset.poke(true)
      uut.clock.step(3)
      uut.reset.poke(false)

      val ID = 0

      // Last block only receives its own data
      uut.wid.poke(ID)
      for(i <- 1 to MW * MH - 16 * ID) {
        val coeff = pow(-1, ((i + 1) % 2)).toFloat * i.toFloat
        uut.i.data.poke(floatToBitsUInt(coeff))
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
        uut.i.data.poke(
          ("b" + String.format(
            "%32s", java.lang.Float.floatToIntBits(v).toBinaryString
          ).replace(' ', '0')).U
        )
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
      uut.clock.step(DSP_PIPELINE_REGS)
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
