package matmul.worker

import chisel3._
import chisel3.util._

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

// https://github.com/edwardcwang/decoupled-serializer.git
// to generate VCD traces with chisel 6.6.0
import chisel3.simulator.VCDHackedEphemeralSimulator._

import matmul.utils.Parameters

class PipelinedWorkerSpec extends AnyFlatSpec with Matchers {
  val param = Parameters(
    16,
    16,
    "src/test/resources/dummy16-matrix.txt"
  )

  "Worker" should "work_lol" in {
    simulate(new PipelinedWorker(
      PARAM = param,
      WID   = 0
    )) { uut =>
      uut.reset.poke(true)
      uut.clock.step(3)
      uut.reset.poke(false)

      for(i <- 0 to 31) {
        uut.in.data.poke(("b" + param.floatToSAF(((i % 16) + 1).toFloat)).U)
        uut.in.work.poke(true)
        uut.clock.step()
        if(i == 5) {
          uut.in.work.poke(false)
          uut.clock.step(5)
        }
        if (i == 15) {
          uut.clock.step()
        }
      }
      uut.in.work.poke(false)
      uut.clock.step(10)

      uut.clock.step(16)
    }
  }
}
