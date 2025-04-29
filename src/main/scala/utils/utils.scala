package matmul

import java.lang.Float.floatToIntBits

package object utils {
  def floatToSAF(
    f   : Float,
    L   : Int = 5,
    W   : Int = 70,
    B   : Int = 150,
    L2N : Int = 16
  ) : String = {
    val bitString = String.format(
      "%32s", java.lang.Float.floatToIntBits(f).toBinaryString
    ).replace(' ', '0')
    // Extract IEEE-754 float32 fields
    val signStr = bitString(0) // .take to get a String and not a Char
    val sign    = Integer.parseInt(bitString(0).toString, 2)
    val exptStr = bitString.substring(1, 9)
    val expt    = Integer.parseInt(bitString.substring(1, 9), 2)
    val mantStr = bitString.substring(9, 32)
    val mant    = Integer.parseInt(bitString.substring(10, 32), 2)
    if(expt == 0 && mant == 0) {
      "0"
    } else {
      // Stolen from CuFP
      // https://github.com/FahimeHajizadeh/Custom-Float-HLS/blob/main/CuFPSAF/src/custom_float.h in CuFPSAF
      val reStr = exptStr.substring(0, 8 - L)
      val lsh   = Integer.parseInt(exptStr.substring(8 - L, 8), 2)
      val mtStr = if(expt == 0) {
        "0".concat(mantStr).concat("0" * lsh)
      } else {
        "1".concat(mantStr).concat("0" * lsh)
      }
      val maStr = if(sign == 0) {
        String.format("%" + W + "s", mtStr).replace(' ', '0')
      } else {
        String.format(
          "%" + W + "s", (
            - scala.math.BigInt.javaBigInteger2bigInt(new java.math.BigInteger(mtStr, 2))
          ).toLong.toBinaryString
        ).replace(' ', '1')
      }
      reStr.concat(maStr)
    }
  }

  // Read matrix file in SAF format
  def readCSVSAF(
    filePath : String,
    L        : Int = 5,
    W        : Int = 70,
    B        : Int = 150,
    L2N      : Int = 16
  ) : Array[Array[String]] = {
    val source = scala.io.Source.fromFile(filePath)
    val data = source
      .getLines()
      .map { line =>
        line.split(" ").map(_.trim.toFloat)
      }.toArray
    source.close()
    data.map(_.map(elt => "b" + floatToSAF(elt)))
  }

  // Read matrix file in SAF format
  def readCSVFloat(
    filePath : String
  ) : Array[Array[String]] = {
    val source = scala.io.Source.fromFile(filePath)
    val data = source
      .getLines()
      .map { line =>
        line.split(" ").map(_.trim.toFloat)
      }.toArray
    source.close()
    data.map(_.map(elt => "b" + String.format(
      "%32s", java.lang.Float.floatToIntBits(elt).toBinaryString
    ).replace(' ', '0')))
  }

  case class Parameters(
    // Matrix width (number of workers)
    M_WIDTH     : Int,
    // Matrix height (number of memory values per worker)
    M_HEIGHT    : Int,
    // Weights values filename
    WEIGHT_FILE : String,
    // SAF parameters
    SAF_L       : Int = 5,
    SAF_W       : Int = 70,
    SAF_B       : Int = 150,
    SAF_L2N     : Int = 16
  ) {
    // SAF total width
    val SAF_WIDTH = 8 - SAF_L + SAF_W
    val memData   = readCSVSAF(WEIGHT_FILE, SAF_L, SAF_W, SAF_B, SAF_L2N)
    val floatData = readCSVFloat(WEIGHT_FILE)
    def floatToSAF(f : Float) : String = {
      matmul.utils.floatToSAF(f, SAF_L, SAF_W, SAF_B, SAF_L2N)
    }
    // AXI buses
    // AXI-Lite
    val CTL_AW = 32
    val CTL_W  = 32
    // AXI
    val AXI_W  = 64
    val AXI_AW = 64
    // Leave some room
    val FIFO_DEPTH = 4 * M_WIDTH
  }
}
