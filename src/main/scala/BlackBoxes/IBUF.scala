package matmul.blackboxes

// Chisel
import chisel3._
import chisel3.util._
import chisel3.experimental._

class IBUF extends BlackBox {
  val io = IO(new Bundle {
    val O = Output(Bool())
    val I = Input(Bool())
  })
}
