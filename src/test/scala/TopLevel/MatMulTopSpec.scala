package matmul

import chisel3._
import chisel3.util._

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

// https://github.com/edwardcwang/decoupled-serializer.git
// to generate VCD traces with chisel 6.6.0
import chisel3.simulator.VCDHackedEphemeralSimulator._

import matmul.utils.Parameters

class MatMulTopSpec extends AnyFlatSpec with Matchers {
  val param = Parameters(
    16,
    16,
    "src/test/resources/dummy16-matrix.txt"
  )

  "MatMulTop" should "work" in {
    simulate(new MatMulWrapper(
      PARAM = param
    )) { uut =>
      val randGen0 = new scala.util.Random
      val seed = randGen0.nextInt()
      // val seed = 540120093 // Good for 64x64
      val randGen = new scala.util.Random(seed)

      def write_axi(addr : Int, data : List[UInt]) : Unit = {
        val pauseIdx = randGen.nextInt(data.length)
        val pauseDuration = randGen.between(1, 10)

        // Set awaddr
        uut.s_axi.awaddr.poke(addr)
        uut.s_axi.awvalid.poke(true)
        uut.s_axi.awlen.poke(data.length)
        // while(!uut.s_axi.awready.peek().litToBoolean) {
        //   println(s"write_axi to ${addr} stalled")
        //   uut.clock.step()
        // }
        uut.clock.step()
        uut.s_axi.awvalid.poke(false)
        // Send wdata
        for(i <- 0 to data.length - 1) {
          if(i == pauseIdx) {
            println(s"Pausing write ${data(i)} to ${addr} for ${pauseDuration} ticks")
            uut.s_axi.wvalid.poke(false)
            uut.clock.step(pauseDuration)
          }
          uut.s_axi.wdata.poke(data(i))
          uut.s_axi.wvalid.poke(true)
          if(i == data.length - 1) {
            uut.s_axi.wlast.poke(true)
          }
          // while(!uut.s_axi.wready.peek().litToBoolean) {
          //   println(s"AXI write at ${addr} stalled because of wready low.")
          //   uut.clock.step()
          //   uut.s_axi.awvalid.poke(false)
          // }
          uut.clock.step()
          println(s"Sent ${data(i)} through AXI")
          uut.s_axi.awvalid.poke(false)
        }
        uut.s_axi.wvalid.poke(false)
        uut.s_axi.wlast.poke(false)
        // Get bresp
        uut.s_axi.bready.poke(true)
        while(!uut.s_axi.bvalid.peek().litToBoolean) {
          println(s"write_axi to ${addr} bresp stalled")
          uut.clock.step()
        }
        uut.clock.step()
        uut.s_axi.bready.poke(false)
      }

      def read_axi(addr : Int, len : Int) : Unit = {
        val pauseIdx = randGen.nextInt(len)
        val pauseDuration = randGen.between(1, 10)

        uut.s_axi.araddr.poke(addr)
        uut.s_axi.arvalid.poke(true)
        uut.s_axi.arlen.poke(len - 1)
        while(!uut.s_axi.arready.peek().litToBoolean) {
          println(s"read_axi ${len} transfers at ${addr} stalled.")
          uut.clock.step()
        }
        // uut.clock.step()
        // uut.s_axi.arvalid.poke(false)
        for(i <- 0 to len - 1) {
          if(i == pauseIdx) {
            println(s"Pausing read ${len} transfers at ${addr} for ${pauseDuration} ticks")
            uut.s_axi.rready.poke(false)
            uut.clock.step(pauseDuration)
          }
          uut.s_axi.rready.poke(true)
          while(!uut.s_axi.rvalid.peek().litToBoolean) {
            println(s"Read addr ${addr} data number ${i} stalled by rvalid low.")
            uut.clock.step()
          }
          uut.clock.step()
          println(s"Read ${uut.s_axi.rdata.peek().litValue} on transfer ${i}")
          uut.s_axi.arvalid.poke(false)
        }
      }

      uut.reset.poke(true)
      uut.clock.step(3)
      uut.reset.poke(false)

      uut.clock.step(4)

      write_axi(123, List.tabulate(param.M_HEIGHT)(
        i => ("b" + java.lang.Float.floatToIntBits((i + 1).toFloat).toBinaryString).U
      ))

      read_axi(111, 16)

      uut.clock.step(255)

      write_axi(123, List.tabulate(param.M_HEIGHT)(
        i => ("b" + java.lang.Float.floatToIntBits((2 * i + 1).toFloat).toBinaryString).U
      ))

      read_axi(111, 16)

      uut.clock.step(255)
    }
  }
}
