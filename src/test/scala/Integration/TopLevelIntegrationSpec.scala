package matmul.integration

// Chisel
import chisel3._
import chisel3.util._

// Scala
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

import math.pow

// https://github.com/edwardcwang/decoupled-serializer.git
// to generate VCD traces with chisel 6.6.0
import chisel3.simulator.VCDHackedEphemeralSimulator._

import axi.AXIWrapper
import matmul.CoreWrapper
import axi.SAXIRW2Full
import axi.interfaces._
import mcp._
import asyncfifo._
import matmul.utils._
import matmul.testutils.FakeClockDivider
import matmul.testutils._

class TopLevelIntegration(
  PARAM : Parameters
) extends Module {
  val axi_aclk  = IO(Input(Bool()))
  val axi_arstn = IO(Input(Bool()))
  val s_axil    = IO(new SlaveAXILiteInterface(PARAM.CTL_AW, PARAM.CTL_W))
  val s_axi     = IO(new SlaveAXIInterface(PARAM.AXI_AW, PARAM.AXI_W))

  val core = Module(new CoreWrapper(PARAM))
  val axi  = Module(new AXIWrapper(
    PARAM.FIFO_CNT_W,
    PARAM.CTL_AW,
    PARAM.CTL_W,
    PARAM.AXI_AW,
    PARAM.AXI_W
  ))

  val iFifoMem = SyncReadMem(PARAM.FIFO_DEPTH, UInt(32.W))
  val oFifoMem = SyncReadMem(PARAM.FIFO_DEPTH, UInt(32.W))

  // AXI wrapper
  axi.axi_aclk  := axi_aclk.asClock
  axi.axi_arstn := axi_arstn
  axi.s_axil    <> s_axil
  axi.s_axi     <> s_axi

  // Core
  core.i_coreclk := clock
  core.i_arstn   := reset

  // Control clock-domain crossing
  axi.ctl_wr_xsrc <> core.ctl_wr_xdst
  axi.ctl_ar_xsrc <> core.ctl_ar_xdst
  axi.ctl_rd_xdst <> core.ctl_rd_xsrc

  // FIFO signals
  axi.ififo_xwcnt <> core.ififo_xwcnt
  axi.ififo_xrcnt <> core.ififo_xrcnt
  axi.ofifo_xwcnt <> core.ofifo_xwcnt
  axi.ofifo_xrcnt <> core.ofifo_xrcnt

  /* MEMORY LOGIC */
  // Output FIFO
  // Write
  when(core.ofifo_wmem.i_we) {
    oFifoMem.write(core.ofifo_wmem.i_addr, core.ofifo_wmem.i_data, clock)
  }
  // Read
  axi.ofifo_rmem.o_data := oFifoMem.read(
    axi.ofifo_rmem.i_addr,
    axi.ofifo_rmem.i_en,
    axi_aclk.asClock
  )
  // Input FIFO
  // Write
  when(axi.ififo_wmem.i_we) {
    iFifoMem.write(axi.ififo_wmem.i_addr, axi.ififo_wmem.i_data, axi_aclk.asClock)
  }
  // Read
  core.ififo_rmem.o_data := iFifoMem.read(
    core.ififo_rmem.i_addr, core.ififo_rmem.i_en, clock
  )
}

class TopLevelIntegrationSpec extends AnyFlatSpec with Matchers {
  // Divisor value for fake clk divisor
  val clkDiv  = 3
  val MH = 16
  val MW = 32
  val USE_HARDFLOAT = false
  val file = "src/test/resources/dummy16-matrix.txt"

  "TopLevelIntegration" should "work" in {
    simulate(new TopLevelIntegration(
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
      val matrix = readCSVFloat(file)

      // Randomness
      val randGen0 = new scala.util.Random
      val seed = randGen0.nextInt()
      val randGen = new scala.util.Random(seed)
      // Print seed for reproductibility
      println(s"Random seed is ${seed}.")

      val fakeClk = new FakeClockDivider(clkDiv)
      // Step the fast clock
      def step(n : Int = 1) : Unit = {
        for(i <- 1 to n) {
          fakeClk.update(uut.axi_aclk)
          uut.clock.step()
        }
      }

      // Step the slow clock
      def stepSlow(n : Int = 1) : Unit = {
        step(2 * n * clkDiv)
      }

      def write_ctl(addr : Int, data : Int) : Unit = {
        uut.s_axil.awaddr.poke(addr)
        uut.s_axil.awvalid.poke(true)
        stepSlow()
        uut.s_axil.wdata.poke(data)
        uut.s_axil.wstrb.poke(pow(2, 32 / 8).toInt - 1)
        uut.s_axil.wvalid.poke(true)
        stepSlow(2)
        uut.s_axil.awvalid.poke(false)
        uut.s_axil.wvalid.poke(false)
        uut.s_axil.bready.poke(true)
        while(!uut.s_axil.bvalid.peek().litToBoolean) {
          println(s"Write ${data} at addr ${addr} stalled")
          stepSlow()
        }
        stepSlow()
        uut.s_axil.bready.poke(false)
      }

      def read_ctl(addr : Int) : BigInt = {
        uut.s_axil.araddr.poke(addr)
        uut.s_axil.arvalid.poke(true)
        stepSlow()
        uut.s_axil.arvalid.poke(false)
        uut.s_axil.rready.poke(true)
        // while(!uut.s_axil.rvalid.peek().litToBoolean) {
        //   println(s"Read addr ${addr} stalled")
        //   stepSlow()
        // }
        val output = uut.s_axil.rdata.peek().litValue
        stepSlow()
        uut.s_axil.rready.poke(false)
        output
      }

      def write_axi(addr : Int, data : List[UInt]) : Unit = {
        val pauseIdx = randGen.nextInt(data.length)
        val pauseDuration = randGen.between(1, 10)
        var cnt = 0

        // Set awaddr
        uut.s_axi.awaddr.poke(addr)
        uut.s_axi.awvalid.poke(true)
        // while(!uut.s_axi.awready.peek().litToBoolean) {
        //   println(s"write_axi to ${addr} stalled")
        //   stepSlow()
        // }
        stepSlow()
        uut.s_axi.awvalid.poke(false)
        // Send wdata
        for(i <- 0 to data.length - 1) {
          if(i == pauseIdx) {
            println(s"Pausing write ${data(i)} to ${addr} for ${pauseDuration} ticks")
            uut.s_axi.wvalid.poke(false)
            stepSlow(pauseDuration)
          }
          uut.s_axi.wdata.poke(data(i))
          uut.s_axi.wvalid.poke(true)
          if(i == data.length - 1) {
            uut.s_axi.wlast.poke(true)
          }
          // while(!uut.s_axi.wready.peek().litToBoolean) {
          //   println(s"AXI write at ${addr} stalled because of wready low.")
          //   stepSlow()
          //   uut.s_axi.awvalid.poke(false)
          // }
          stepSlow()
          println(s"Sent ${data(i)} through AXI ($cnt)")
          cnt = cnt + 1
          uut.s_axi.awvalid.poke(false)
        }
        uut.s_axi.wvalid.poke(false)
        uut.s_axi.wlast.poke(false)
        // Get bresp
        uut.s_axi.bready.poke(true)
        while(!uut.s_axi.bvalid.peek().litToBoolean) {
          println(s"write_axi to ${addr} bresp stalled")
          stepSlow()
        }
        stepSlow()
        uut.s_axi.bready.poke(false)
      }

      def write_axi_big(addr : Int, data : List[BigInt]) : Unit = {
        write_axi(addr, data.map(_.U))
      }

      def read_axi(addr : Int, len : Int) : Unit = {
        val pauseIdx = randGen.nextInt(len)
        val pauseDuration = randGen.between(1, 10)

        uut.s_axi.araddr.poke(addr)
        uut.s_axi.arvalid.poke(true)
        uut.s_axi.arlen.poke(len - 1)
        while(!uut.s_axi.arready.peek().litToBoolean) {
          println(s"read_axi ${len} transfers at ${addr} stalled.")
          stepSlow()
        }
        stepSlow()
        uut.s_axi.arvalid.poke(false)
        for(i <- 0 to len - 1) {
          if(i == pauseIdx) {
            println(s"Pausing read ${len} transfers at ${addr} for ${pauseDuration} ticks")
            uut.s_axi.rready.poke(false)
            stepSlow(pauseDuration)
          }
          uut.s_axi.rready.poke(true)
          // while(!uut.s_axi.rvalid.peek().litToBoolean) {
          //   println(s"Read addr ${addr} data number ${i} stalled by rvalid low.")
          //   stepSlow()
          // }
          stepSlow()
          println(s"Read ${uut.s_axi.rdata.peek().litValue} on transfer ${i}")
          uut.s_axi.arvalid.poke(false)
        }
      }

      uut.axi_arstn.poke(false)
      uut.reset.poke(false)
      // Offset clocks to prevent wrong signal updates
      step(4 * clkDiv + 1)
      uut.axi_arstn.poke(true)
      uut.reset.poke(true)

      // Write prog
      write_ctl(0x0, 1)

      // Send prog data
      // val coeffs = (
      //   for(
      //     i <- 0 to MH - 1;
      //     j <- 0 to MW - 1
      //   ) yield {
      //     matrix(i)(j).U
      //   }
      // ).toList
      val coeffs = (for(i <- 0 to MH * MW - 1) yield {
        // print(s"${i} ")
        floatToBitsUInt(i.toFloat)
      }).toList
      println(coeffs)
      write_axi(111, coeffs)

      // Send vector
      val vec = (for(i <- 0 to MW - 1) yield {
        // print(s"${(i + 1) % 2} ")
        floatToBitsUInt(((i + 1) % 2).toFloat)
      }).toList
      println(vec)
      write_axi(222, vec)

      stepSlow(256)

      read_axi(333, MH)

      stepSlow(10)

      write_ctl(0x0, 1)
      // Send prog data
      val coeffs2 = (
        for(
          i <- 0 to MH - 1;
          j <- MW - 1 to 0 by -1
        ) yield {
          floatToBitsUInt((i * 255 + j).toFloat)
          // matrix(i)(j).U
        }
      ).toList
      write_axi(444, coeffs2)

      // Send vector
      val vec2 = (for(i <- 0 to MW - 1) yield {
        floatToBitsUInt((i + 1).toFloat)
      }).toList
      write_axi(555, vec2)

      stepSlow(2 * MH * MW)

      read_axi(666, MH)

      stepSlow(10)
    }
  }
}
