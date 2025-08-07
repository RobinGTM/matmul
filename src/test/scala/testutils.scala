/* testutils.scala -- Testing utilities and FakeClockDivider
 *                    definition
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
