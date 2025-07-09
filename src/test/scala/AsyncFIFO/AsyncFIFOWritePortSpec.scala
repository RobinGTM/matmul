package asyncfifo

// Chisel
import chisel3._

// Scala
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import math.pow

// https://github.com/edwardcwang/decoupled-serializer.git
// to generate VCD traces with chisel 6.6.0
import chisel3.simulator.VCDHackedEphemeralSimulator._

// Local
import asyncfifo._
import asyncfifo.interfaces._
import mcp.interfaces._

// Fake read-side MCP
class FakeReadSideReadSyncMCP {
  var loadCnt : Int = 0

  def update(mcp : MCPCrossSrc2DstInterface[UInt]) : Unit = {
    if(loadCnt == 0) {
      mcp.load.poke(!mcp.load.peek().litToBoolean)
    }
    loadCnt = (loadCnt + 1) % 5
  }
}

class FakeReadSideWriteSyncMCP {
  var clkSinceCross : Int = 0
  var prevLoad      : Boolean = false

  def update(mcp : MCPCrossSrc2DstInterface[UInt]) : Unit = {
    val mcpLoad = mcp.load.peek().litToBoolean

    if(mcpLoad ^ prevLoad) {
      clkSinceCross = 0
    } else {
      clkSinceCross = clkSinceCross + 1
    }

    if(clkSinceCross == 2) {
      mcp.ack.poke(!mcp.ack.peek().litToBoolean)
    }

    prevLoad = mcpLoad
  }
}

class AsyncFIFOWritePortSpec extends AnyFlatSpec with Matchers {
  // 8-bit addresses
  val CNT_W = 8
  // 32-bit data
  val DW    = 32

  "AsyncFIFOWritePort" should "work" in {
    simulate(new AsyncFIFOWritePort(CNT_W, UInt(DW.W))) { uut =>
      val rdMcp = new FakeReadSideReadSyncMCP
      val wrMcp = new FakeReadSideWriteSyncMCP

      // Step and update mock-ups
      def step(n : Int = 1) : Unit = {
        for(i <- 1 to n) {
          wrMcp.update(uut.wcnt_cross)
          rdMcp.update(uut.rcnt_cross)
          uut.clock.step()
        }
      }

      uut.clock.step()

      step(10)

      uut.fifo_wr.i_we.poke(true)
      for(i <- 0 to pow(2, CNT_W).toInt) {
        uut.fifo_wr.i_data.poke(i + 1)
        step()
      }

      step(10)

    }
  }
}
