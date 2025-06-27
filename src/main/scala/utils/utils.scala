package matmul

import chisel3._
import chisel3.util._

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
      // https://github.com/FahimeHajizadeh/Custom-Float-HLS/blob/main/CuFPSAF/src/custom_float.h
      // in CuFPSAF
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

  def floatToSAFUInt(
    f   : Float,
    L   : Int = 5,
    W   : Int = 70,
    B   : Int = 150,
    L2N : Int = 16
  ) : UInt = {
    ("b" + floatToSAF(f)).U
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

  case class Parameters(args : Array[String] = Array()) {
    // Basic command-line parser
    val argMap = args.sliding(2, 2).collect {
      case Array(flag, value) if flag.startsWith("-") =>
        flag.stripPrefix("-") -> Option(value)
    } .collect {
      case (key, Some(value)) => key -> value
    } .toMap

    def parseOpt(
      longOpt  : String,
      shortOpt : String,
      default  : Int) : Int = {
      if(argMap.contains(longOpt)) {
        argMap(longOpt).toInt
      } else if(argMap.contains(shortOpt)) {
        argMap(shortOpt).toInt
      } else {
        default
      }
    }
    def parseOpt(
      longOpt  : String,
      shortOpt : String,
      default  : Double) : Double = {
      if(argMap.contains(longOpt)) {
        argMap(longOpt).toDouble
      } else if(argMap.contains(shortOpt)) {
        argMap(shortOpt).toDouble
      } else {
        default
      }
    }
    def parseOpt(
      longOpt  : String,
      shortOpt : String,
      default  : String) : String = {
      if(argMap.contains(longOpt)) {
        argMap(longOpt)
      } else if(argMap.contains(shortOpt)) {
        argMap(shortOpt)
      } else {
        default
      }
    }

    def parseSwitch(
      longSwitch  : String,
      shortSwitch : String
    ) : Boolean = {
      argMap.contains(longSwitch) || argMap.contains(shortSwitch)
    }

    // Base clock (hw QSPI clock at 156.25)
    val BASE_CLK = parseOpt("BASE_CLK", "fbase", 156.25)

    // Core clock PLL
    val PLL_MULT        = parseOpt("PLL_MULT", "xpll", 9)
    val PLL_DIV         = parseOpt("PLL_DIV", "dpll", 4)

    // Matrix width (number of workers)
    val M_WIDTH  = parseOpt("M_WIDTH", "w", 16)
    // Matrix height (number of memory values per worker)
    val M_HEIGHT = parseOpt("M_HEIGHT", "h", 16)

    // Use hardfloat if true else SAF
    val USE_HARDFLOAT = args.contains("-USE_HARDFLOAT") || args.contains("-hf")

    // Output directory
    private val hfString = if(USE_HARDFLOAT) { "hardfloat" } else { "saf" }
    private val defaultOut : String =
      s"outputs/MatMul-${M_HEIGHT}x${M_WIDTH}" +
      s"_${hfString}-${BASE_CLK * PLL_MULT / PLL_DIV}MHz"
    // Output directory
    val OUTDIR = parseOpt("OUTDIR", "o", defaultOut)

    // SAF parameters
    val SAF_L    = parseOpt("SAF_L", "sl", 5)
    val SAF_W    = parseOpt("SAF_W", "sw", 70)
    val SAF_B    = parseOpt("SAF_B", "sb", 150)
    val SAF_L2N  = parseOpt("SAF_L2N", "sn", 16)
    // AXI-Lite widths (defined by XDMA config)
    val CTL_AW   = parseOpt("CTL_AW", "caw", 32)
    val CTL_W    = parseOpt("CTL_W", "cw", 32)
    // SAF total width
    val SAF_WIDTH = 8 - SAF_L + SAF_W
    // val memData = if(USE_HARDFLOAT) {
    //   readCSVFloat(WEIGHT_FILE)
    // } else {
    //   readCSVSAF(WEIGHT_FILE, SAF_L, SAF_W, SAF_B, SAF_L2N)
    // }
    // AXI buses
    val AXI_W  = parseOpt("AXI_W", "aw", 64)
    val AXI_AW = parseOpt("AXI_AW", "aaw", 64)
    // Leave some room
    val FIFO_DEPTH = 4 * M_HEIGHT
    val FIFO_CNT_W = log2Up(FIFO_DEPTH)

    val DW = if(USE_HARDFLOAT) { 32 } else { SAF_WIDTH }

    val CTL_PROG  = 0x0
    val CTL_WRITE = 0x1
    val CTL_READY = 0x2
    // Set to 1 if SAF is enabled (convenience)
    val CTL_SAF   = 0x10
  }

  // Small bundle to carry write control addr / data through only one
  // MultiCyclePath instance
  case class WrAddrData(
    AW : Int,
    DW : Int
  ) extends Bundle {
    val addr = UInt(AW.W)
    val data = UInt(DW.W)
  }

  // Basic RW register bank interface
  class BasicRegInterface(
    AW : Int,
    DW : Int
  ) extends Bundle {
    val i_data = Input(UInt(DW.W))
    val i_addr = Input(UInt(AW.W))
    val i_we   = Input(Bool())
    val i_en   = Input(Bool())
    val o_data = Output(UInt(DW.W))
  }
}
