package matmul.fifo

import chisel3._
import chisel3.util._

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

// https://github.com/edwardcwang/decoupled-serializer.git
// to generate VCD traces with chisel 6.6.0
import chisel3.simulator.VCDHackedEphemeralSimulator._

class FIFOSpec extends AnyFlatSpec with Matchers {
  "FIFO" should "work" in {
    simulate(new FIFO(
      UInt(32.W),
      16
    )) { uut =>
      uut.reset.poke(true)
      uut.clock.step(3)
      uut.reset.poke(false)

      for(i <- 1 to 16) {
        uut.wr.i_data.poke(i.U)
        uut.wr.i_we.poke(true)
        uut.clock.step()
      }
      uut.clock.step()
      uut.wr.i_we.poke(false)
      uut.clock.step(10)

      for(i <- 1 to 16) {
        uut.rd.i_en.poke(true)
        uut.clock.step()
      }

      uut.rd.i_en.poke(true)
      uut.clock.step(10)
    }
  }
}
