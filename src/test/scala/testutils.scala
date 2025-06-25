package matmul

import chisel3._

package object testutils {
  def floatToBitsUInt(f : Float) : UInt = {
    ("b" + String.format(
      "%32s", java.lang.Float.floatToIntBits(f).toBinaryString
    ).replace(' ', '0')).U
  }
}
