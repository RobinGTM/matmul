/* utils.scala -- General utilities and Parameters class definition
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
import chisel3.util._

import math.pow

import java.lang.Float.floatToIntBits

package object utils {
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
    def parseOpt(
      longOpt  : String,
      shortOpt : String,
      default  : Array[String]
    ) : Array[String] = {
      if(argMap.contains(longOpt)) {
        argMap(longOpt).split(" ")
      } else if(argMap.contains(shortOpt)) {
        argMap(shortOpt).split(" ")
      } else {
        default
      }
    }

    // FIRRTL and chisel parameters
    val CIRCT_ARGS   = parseOpt("CIRCT_ARGS", "C", Array[String]())
    val FIRTOOL_ARGS = parseOpt("FIRTOOL_ARGS", "F", Array[String]())

    def parseSwitch(
      longSwitch  : String,
      shortSwitch : String
    ) : Boolean = {
      argMap.contains(longSwitch) || argMap.contains(shortSwitch)
    }

    // Base clock (hw QSPI clock at 156.25)
    val BASE_CLK = parseOpt("BASE_CLK", "fbase", 156.25)

    // Core clock PLL
    val PLL_MULT = parseOpt("PLL_MULT", "xpll", 8)
    val PLL_DIV  = parseOpt("PLL_DIV", "dpll", 4)

    // Matrix width (number of workers)
    val M_WIDTH  = parseOpt("M_WIDTH", "w", 16)
    // Matrix height (number of memory values per worker)
    val M_HEIGHT = parseOpt("M_HEIGHT", "h", 16)

    // Use hardfloat if true else SAF
    val USE_HARDFLOAT = args.contains("-USE_HARDFLOAT") || args.contains("-hf")

    // Use XPM for FIFOs
    val FIFO_TYPE = parseOpt("FIFO_TYPE", "fifo", "default")
    require(FIFO_TYPE == "xpm" || FIFO_TYPE == "default",
      "FIFO_TYPE must be one of 'xpm' (Xilinx Parametrized Macros)" +
      " or 'default' (custom Chisel implementation)"
    )

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
    // Leave some room and ensure FIFOs are a power of 2-deep
    val OFIFO_DEPTH = pow(2, log2Up(4 * M_HEIGHT)).toInt
    val OFIFO_CNT_W = log2Up(OFIFO_DEPTH)
    // Input FIFO is bigger
    val IFIFO_DEPTH = pow(2, log2Up(4 * M_WIDTH)).toInt
    val IFIFO_CNT_W = log2Up(IFIFO_DEPTH)
    // MatMul chain pipeline registers
    val PIPELINE_REGS = parseOpt("PIPELINE_REGS", "ppd", 2)
    // MAC DSP pipeline registers
    val MAC_DSP_PIPELINE_REGS = parseOpt("MAC_DSP_PIPELINE_DEPTH", "mpd", 4)

    // Datapaths width
    val DW = 33

    // Control register bit fields
    val CTL_PROG  = 0x0
    val CTL_WRITE = 0x1
    val CTL_READY = 0x2
    // Set to 1 if SAF is enabled (convenience)
    val CTL_SAF   = 0x10

    // Register addresses
    val CTL_REG    = 0x0
    val HEIGHT_REG = 0x4
    val WIDTH_REG  = 0x8
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
