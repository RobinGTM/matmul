package matmul

// Chisel
import chisel3._
import chisel3.util._

// Local
import matmul.utils.Parameters

package object interfaces {
  class MatMulInterface(
    DW : Int
  ) extends Bundle {
    val data  = UInt(DW.W)
    val valid = Bool()
    val prog  = Bool()
    val ready = Bool()
  }
}
