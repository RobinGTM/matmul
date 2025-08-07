package mac

import chisel3._
import chisel3.util._

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

// https://github.com/edwardcwang/decoupled-serializer.git
// to generate VCD traces with chisel 6.6.0
import chisel3.simulator.VCDHackedEphemeralSimulator._

import saf._
import saf.utils._

class SAFMACTest(
  DW : Int,
  W  : Int,
  L  : Int
) extends Module {
  private val SAF_WIDTH = W + 8 - L
  val io     = IO(new MACInterface(DW))
  // Control output
  val o_saf  = IO(Output(UInt(SAF_WIDTH.W)))
  val safMac = Module(new SAFMAC(DW, L, W))

  safMac.io.i_a   := expandF32(io.i_a)
  safMac.io.i_b   := expandF32(io.i_b)
  safMac.io.i_acc := io.i_acc
  safMac.io.i_rst := io.i_rst

  io.o_res := restoreF32(safMac.io.o_res)
  o_saf    := safMac.o_saf
}

class SAFMACSpec extends AnyFlatSpec with Matchers {
  "SAFMAC" should "work" in {
    simulate(new SAFMACTest(
      33, 70, 5
    )) { uut =>
      def print_outs(uut : SAFMACTest) : Unit = {
        println("=========================")
        println(uut.io.o_res.peek())
        println(uut.o_saf.peek())
      }

      uut.io.i_rst.poke(0)

      // -1 * 15.99999
      uut.io.i_a.poke(0xbf800000)
      uut.io.i_b.poke(0x417fffff)
      uut.io.i_acc.poke(true)
      uut.clock.step()
      print_outs(uut)

      // 12321.132 * 10
      uut.io.i_a.poke(0x46408487)
      uut.io.i_b.poke(0x41200000)
      uut.io.i_acc.poke(true)
      uut.clock.step()
      print_outs(uut)

      // 123.321 * -65543.435
      uut.io.i_a.poke(0x42f6a45a)
      uut.io.i_b.poke(0xc78003b8)
      uut.io.i_acc.poke(true)
      uut.clock.step()
      print_outs(uut)

      uut.io.i_acc.poke(false)
      uut.clock.step()
      print_outs(uut)

      uut.clock.step(10)
      print_outs(uut)
    }
  }
}
