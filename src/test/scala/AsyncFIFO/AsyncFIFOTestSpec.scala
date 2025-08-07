/* AsyncFIFOTestSpec.scala -- Test-benches for the modules defined in
 *                            AsyncFIFOTest.scala
 *
 * (C) Copyright 2025 Robin Gay <robin.gay@polymtl.ca>
 *
 * This file is part of matmul.
 * 
 * matmul is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * matmul is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with matmul. If not, see <https://www.gnu.org/licenses/>.
 */
package asyncfifo

// Chisel
import chisel3._

// Scala
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

// https://github.com/edwardcwang/decoupled-serializer.git
// to generate VCD traces with chisel 6.6.0
import chisel3.simulator.VCDHackedEphemeralSimulator._

// Local
import asyncfifo._
import asyncfifo.interfaces._
import mcp.interfaces._
import matmul.testutils.FakeClockDivider

class AsyncFIFOTestRdFasterSpec extends AnyFlatSpec with Matchers {
  val CNT_W = 8
  val UINT_W = 32
  val clkDiv = 3
  "AsyncFIFOTest" should "work" in {
    simulate(new AsyncFIFOTestRdFaster(CNT_W, UInt(UINT_W.W))) { uut =>
      val fakeClk = new FakeClockDivider(clkDiv)

      def step(n : Int = 1) : Unit = {
        for(i <- 1 to n) {
          fakeClk.update(uut.wr_clk)
          uut.clock.step()
        }
      }

      // Testing big burst
      uut.wr_rst.poke(true)
      uut.reset.poke(true)
      step(2 * clkDiv)
      uut.wr_rst.poke(false)
      uut.reset.poke(false)

      for(i <- 0 to 256) {
        step()
        uut.fifo_wr.i_we.poke(true)
        uut.fifo_wr.i_data.poke(i)
        if(i > 127) {
          uut.fifo_rd.i_en.poke(true)
        }
        step(2 * clkDiv - 1)
      }
      step()
      uut.fifo_wr.i_we.poke(false)
      uut.fifo_wr.i_data.poke(0)
      step(2 * clkDiv - 1)

      for(i <- 0 to 255) {
        uut.fifo_rd.i_en.poke(true)
        step()
      }
      uut.fifo_rd.i_en.poke(false)
      step()

      step(10)

      // Testing FULL condition
      for(i <- 0 to 260) {
        step()
        uut.fifo_wr.i_we.poke(true)
        uut.fifo_wr.i_data.poke(i)
        step(2 * clkDiv - 1)
      }

      // Testing EMPTY condition
      for(i <- 0 to 300) {
        uut.fifo_rd.i_en.poke(true)
        step()
      }

      step(10)
    }
  }
}

// Same thing but clock regions are swapped
class AsyncFIFOTestWrFasterSpec extends AnyFlatSpec with Matchers {
  val CNT_W = 8
  val UINT_W = 32
  val clkDiv = 3
  "AsyncFIFOTest" should "work" in {
    simulate(new AsyncFIFOTestWrFaster(CNT_W, UInt(UINT_W.W))) { uut =>
      val fakeClk = new FakeClockDivider(clkDiv)

      def step(n : Int = 1) : Unit = {
        for(i <- 1 to n) {
          fakeClk.update(uut.rd_clk)
          uut.clock.step()
        }
      }

      // Testing big burst
      uut.rd_rst.poke(true)
      uut.reset.poke(true)
      step(2 * clkDiv)
      uut.rd_rst.poke(false)
      uut.reset.poke(false)

      for(i <- 0 to 256) {
        step()
        uut.fifo_wr.i_we.poke(true)
        uut.fifo_wr.i_data.poke(i)
        if(i > 127) {
          uut.fifo_rd.i_en.poke(true)
        }
      }
      step()
      uut.fifo_wr.i_we.poke(false)
      uut.fifo_wr.i_data.poke(0)
      step()

      for(i <- 0 to 255) {
        step()
        uut.fifo_rd.i_en.poke(true)
        step(2 * clkDiv - 1)
      }
      step()
      uut.fifo_rd.i_en.poke(false)
      step(2 * clkDiv - 1)

      step(10)

      // Testing FULL condition
      for(i <- 0 to 260) {
        uut.fifo_wr.i_we.poke(true)
        uut.fifo_wr.i_data.poke(i)
      }

      // Testing EMPTY condition
      for(i <- 0 to 300) {
        step()
        uut.fifo_rd.i_en.poke(true)
        step(2 * clkDiv - 1)
      }

      step(10)
    }
  }
}
