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
  val i_a   = IO(Input(UInt(32.W)))
  val i_b   = IO(Input(UInt(32.W)))
  val i_acc = IO(Input(Bool()))
  val o_res = IO(Output(UInt(32.W)))
  // Control output
  val o_saf = IO(Output(UInt(SAF_WIDTH.W)))

  val safMac = Module(new SAFMAC(DW, L, W))

  safMac.i_a := expandF32(i_a)
  safMac.i_b := expandF32(i_b)
  safMac.i_acc := i_acc

  o_res := restoreF32(safMac.o_res)
  o_saf := safMac.o_saf
}

class SAFMACSpec extends AnyFlatSpec with Matchers {
  "SAFMAC" should "work" in {
    simulate(new SAFMACTest(
      33, 70, 5
    )) { uut =>
      def print_outs(uut : SAFMACTest) : Unit = {
        println("=========================")
        println(uut.o_res.peek())
        println(uut.o_saf.peek())
      }

      // -1 * 15.99999
      uut.i_a.poke(0xbf800000)
      uut.i_b.poke(0x417fffff)
      uut.i_acc.poke(true)
      uut.clock.step()
      print_outs(uut)

      // 12321.132 * 10
      uut.i_a.poke(0x46408487)
      uut.i_b.poke(0x41200000)
      uut.i_acc.poke(true)
      uut.clock.step()
      print_outs(uut)

      // 123.321 * -65543.435
      uut.i_a.poke(0x42f6a45a)
      uut.i_b.poke(0xc78003b8)
      uut.i_acc.poke(true)
      uut.clock.step()
      print_outs(uut)

      uut.i_acc.poke(false)
      uut.clock.step()
      print_outs(uut)

      uut.clock.step(10)
      print_outs(uut)
    }
  }
}
