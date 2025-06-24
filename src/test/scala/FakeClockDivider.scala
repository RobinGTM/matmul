package matmul.utils

import chisel3._
import chisel3.util._

// https://github.com/edwardcwang/decoupled-serializer.git
// to generate VCD traces with chisel 6.6.0
import chisel3.simulator.VCDHackedEphemeralSimulator._

class FakeClockDivider(DIV : Int) {
  var counter : Int = 0

  def update(clkPort : Bool) : Unit = {
    if(counter == 0) {
      clkPort.asBool.poke(!clkPort.asBool.peek().litToBoolean)
    }
    counter = (counter + 1) % DIV
  }
}
