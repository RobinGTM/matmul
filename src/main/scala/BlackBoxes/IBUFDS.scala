package matmul.blackboxes

// Chisel
import chisel3._
import chisel3.util._
import chisel3.experimental._

class IBUFDS extends BlackBox {
  val io = IO(new Bundle {
    val I  = Input(Clock())
    val IB = Input(Clock())
    val O  = Output(Clock())
  })
}
