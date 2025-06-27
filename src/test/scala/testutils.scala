package matmul

import chisel3._
import chisel3.simulator.VCDHackedEphemeralSimulator._

package object testutils {
  def floatToBitsUInt(f : Float) : UInt = {
    ("b" + String.format(
      "%32s", java.lang.Float.floatToIntBits(f).toBinaryString
    ).replace(' ', '0')).U
  }

  class FakeClockDivider(DIV : Int) {
    var counter : Int = 0

    def update(clkPort : Bool) : Unit = {
      if(counter == 0) {
        clkPort.asBool.poke(!clkPort.asBool.peek().litToBoolean)
      }
      counter = (counter + 1) % DIV
    }
  }
}
