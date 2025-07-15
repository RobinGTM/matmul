package matmul.utils

import chisel3._
import chisel3.util._

class ExpandFloat(
) extends Module {
  val i_f32 = IO(Input(UInt(32.W)))
  val o_exp = IO(Output(UInt(33.W)))

  val sign = i_f32(31)
  val expt = i_f32(30, 23)
  val mant = i_f32(22, 0)

  // Expand mantissa
  val eMant = Wire(UInt(24.W))
  when(expt === 0.U) {
    eMant  := Cat(0.U(1.W), mant)
  } .otherwise {
    eMant  := Cat(1.U(1.W), mant)
  }
  // Sign mantissa
  val sMant = Wire(UInt(25.W))
  sMant    := Mux(sign, 1.U + ~eMant, eMant)
  val exF32 = Cat(expt, sMant)

  o_exp := exF32
}
