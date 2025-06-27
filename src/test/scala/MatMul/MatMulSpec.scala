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
  val MH = 16
  val MW = 16
  val USE_HARDFLOAT = false
  val file = "src/test/resources/dummy16-matrix.txt"

  "MatMul" should "work" in {
    simulate(new MatMul(
      PARAM = if(USE_HARDFLOAT) {
        new Parameters(Array(
          "-h", s"${MH}", "-w", s"${MW}", "-hf"
        ))
      } else {
        new Parameters(Array(
          "-h", s"${MH}", "-w", s"${MW}"
        ))
      }
    )) { uut =>
      // MatMulController converts to SAF internally
      val matrix = readCSVFloat(file)

      uut.reset.poke(true)
      uut.clock.step(3)
      uut.reset.poke(false)
      uut.clock.step()

      // Write prog
      uut.ctl_reg.i_data.poke(1)
      uut.ctl_reg.i_addr.poke(0)
      uut.ctl_reg.i_we.poke(true)
      uut.clock.step()
      uut.ctl_reg.i_data.poke(0)
      uut.ctl_reg.i_addr.poke(0)
      uut.ctl_reg.i_we.poke(false)

      // Wait a bit
      uut.clock.step(10)

      // Be ready
      uut.m_axis.tready.poke(true)

      // Send matrix
      // Prog sequence
      for(i <- 0 to MH - 1) {
        for(j <- 0 to MW - 1) {
          uut.s_axis.tdata.poke(matrix(i)(j).U)
          uut.s_axis.tvalid.poke(true)
          if(i == MH - 1 && j == MW - 1) {
            uut.s_axis.tlast.poke(true)
          }
          uut.clock.step()
          if(j == 5) {
            uut.s_axis.tvalid.poke(false)
            uut.clock.step(10)
          }
        }
      }
      uut.s_axis.tvalid.poke(false)
      uut.s_axis.tlast.poke(false)

      // Immediately send coeffs
      while(!uut.s_axis.tready.peek().litToBoolean) {
        uut.clock.step()
      }

      for(i <- 0 to MW - 1) {
        uut.s_axis.tdata.poke(floatToBitsUInt((i + 1).toFloat))
        uut.s_axis.tvalid.poke(true)
        uut.clock.step()
      }
      uut.s_axis.tvalid.poke(false)
      uut.s_axis.tdata.poke(0xffff)

      uut.clock.step(MW * MH)
    }
  }
}
