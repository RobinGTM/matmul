package mac

import chisel3._
import chisel3.util._

case class MACInterface(
  DW : Int = 33
) extends Bundle {
  val i_a   = Input(UInt(DW.W))
  val i_b   = Input(UInt(DW.W))
  val i_acc = Input(Bool())
  val i_rst = Input(Bool())
  val o_res = Output(UInt(DW.W))
}
