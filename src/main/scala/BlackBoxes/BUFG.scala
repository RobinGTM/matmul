package matmul.blackboxes

// Chisel
import chisel3._
import chisel3.util._
import chisel3.experimental._

class BUFG extends BlackBox {
  val io = IO(new Bundle {
    val O = Output(Clock())
    val I = Input(Clock())
  })
}
