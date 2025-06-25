package matmul

import chisel3._
import chisel3.util._

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

// https://github.com/edwardcwang/decoupled-serializer.git
// to generate VCD traces with chisel 6.6.0
import chisel3.simulator.VCDHackedEphemeralSimulator._

import matmul.utils._
import matmul.testutils._

class MatMulSpec extends AnyFlatSpec with Matchers {
  val file = "src/test/resources/dummy16-matrix.txt"
  val MH = 16
  val MW = 16
  val USE_HARDFLOAT = false

  "MatMul" should "work" in {
    simulate(new MatMul(
      M_HEIGHT      = MH,
      M_WIDTH       = MW,
      USE_HARDFLOAT = USE_HARDFLOAT
      // Defaults
    )) { uut =>
      val matrix = if(USE_HARDFLOAT) {
        readCSVFloat(file)
      } else {
        readCSVSAF(file)
      }

      uut.reset.poke(true)
      uut.clock.step(3)
      uut.reset.poke(false)
      uut.clock.step()

      // Prog sequence
      for(i <- 0 to MH - 1) {
        for(j <- 0 to MW - 1) {
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

      for(i <- 0 to MW - 1) {
        uut.i.data.poke(("b" + floatToSAF((i + 1).toFloat)).U)
        uut.i.valid.poke(true)
        uut.clock.step()
      }
      uut.i.data.poke(0)
      uut.i.valid.poke(false)

      uut.clock.step(256)
    }
  }
}
