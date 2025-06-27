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

class MatMulCoreSpec extends AnyFlatSpec with Matchers {
  val file = "src/test/resources/dummy16-matrix.txt"
  val MH = 16
  val MW = 16
  val USE_HARDFLOAT = true

  "MatMul" should "work" in {
    simulate(new MatMulCore(
      PARAM = new Parameters(Array(
        "-h", s"${MH}", "-w", s"${MW}", "-hf", s"${USE_HARDFLOAT}"
      ))
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
        if(USE_HARDFLOAT) {
          uut.i.data.poke(floatToBitsUInt((i + 1).toFloat))
        } else {
          uut.i.data.poke(floatToSAFUInt((i + 1).toFloat))
        }
        uut.i.valid.poke(true)
        uut.clock.step()
      }
      uut.i.data.poke(0)
      uut.i.valid.poke(false)

      uut.clock.step()

      while(!uut.o.ready.peek().litToBoolean) {
        uut.clock.step()
      }

      for(i <- 0 to MW - 1) {
        if(USE_HARDFLOAT) {
          uut.i.data.poke(floatToBitsUInt((- i - 1).toFloat))
        } else {
          uut.i.data.poke(floatToSAFUInt((- i - 1).toFloat))
        }
        uut.i.valid.poke(true)
        uut.clock.step()
      }
      uut.i.data.poke(0)
      uut.i.valid.poke(false)

      uut.clock.step(256)
    }
  }
}
