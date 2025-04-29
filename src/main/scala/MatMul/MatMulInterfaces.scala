package matmul

// Chisel
import chisel3._
import chisel3.util._

// Local
import matmul.utils.Parameters

package object interfaces {
  class MatMulInput(
    DW : Int
  ) extends Bundle {
    val data  = Input(UInt(DW.W))
    val valid = Input(Bool())
  }
  class MatMulOutput(
    DW : Int
  ) extends Bundle {
    val data  = Output(UInt(DW.W))
    val valid = Output(Bool())
  }
}
