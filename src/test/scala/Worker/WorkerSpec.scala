package matmul.worker

import chisel3._
import chisel3.util._

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

// https://github.com/edwardcwang/decoupled-serializer.git to generate
// VCD traces with chisel 6.6.0
import chisel3.simulator.VCDHackedEphemeralSimulator._

import math.pow

import matmul.utils.Parameters

class WorkerSpec extends AnyFlatSpec with Matchers {
  val param = Parameters(16, 16, "src/test/resources/dummy16-matrix.txt")
  val MH = 16
  val MW = 16

  "Worker" should "work_lol" in {
    simulate(new Worker(
      DW       = 73,
      M_HEIGHT = MH,
      M_WIDTH  = MW,
      USE_HARDFLOAT = false
    )) { uut =>
      uut.reset.poke(true)
      uut.clock.step(3)
      uut.reset.poke(false)

      // // Last block only receives its own data
      // uut.wid.poke(MH - 1)
      // for(i <- 1 to 16) {
      //   uut.i.data.poke(
      //     ("b" + param.floatToSAF(pow(-1, ((i + 1) % 2)).toFloat * i.toFloat)).U
      //   )
      //   uut.i.valid.poke(true)
      //   uut.i.prog.poke(true)
      //   uut.clock.step()
      // }

      // First worker has to pass all prog data
      uut.wid.poke(0)
      for(i <- 1 to MH * MW) {
        // uut.i.data.poke(
        //   ("b" + param.floatToSAF(pow(-1, ((i + 1) % 2)).toFloat * i.toFloat)).U
        // )
        uut.i.data.poke(i)
        uut.i.valid.poke(true)
        uut.i.prog.poke(true)
        uut.clock.step()
      }
      uut.i.prog.poke(false)
      uut.i.valid.poke(false)

      uut.clock.step(10)

      for(i <- 1 to 16) {
        uut.i.data.poke(("b" + param.floatToSAF(10 * i.toFloat)).U)
        uut.i.valid.poke(true)
        uut.clock.step()
        if(i == 5) {
          uut.i.valid.poke(false)
          uut.clock.step(5)
        }
      }
      uut.i.valid.poke(false)
      uut.clock.step(10)

      uut.clock.step(16)
    }
  }
}
