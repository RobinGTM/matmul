package matmul.blackboxes

// Chisel
import chisel3._
import chisel3.util._
import chisel3.experimental._

class IBUFDS_GTE4 extends BlackBox(Map(
  "REFCLK_HROW_CK_SEL" -> "2'b00"
)) {
  val io = IO(new Bundle {
    val O     = Output(Clock())
    val ODIV2 = Output(Clock())
    val I     = Input(Clock())
    val IB    = Input(Clock())
    val CEB   = Input(Bool())
  })
}
