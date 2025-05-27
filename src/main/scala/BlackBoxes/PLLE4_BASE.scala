package matmul.blackboxes

// Chisel
import chisel3._
import chisel3.util._
import chisel3.experimental._

class PLLE4_BASE extends BlackBox(Map(
  "CLKFBOUT_MULT" -> "9",
  "CLKOUT0_DIVIDE" -> "4",
  "IS_RST_INVERTED" -> "1'b1"
)) {
  val io = IO(new Bundle {
    val CLKFBOUT = Output(Clock())
    val CLKOUT0  = Output(Clock())
    val LOCKED   = Output(Bool())
    val CLKFBIN  = Input(Clock())
    val CLKIN    = Input(Clock())
    val RST      = Input(Bool())
  })
}
