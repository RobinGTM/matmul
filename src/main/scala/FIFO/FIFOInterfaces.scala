package matmul.fifo

import chisel3._
import chisel3.util._

package object interfaces {
  class FIFOReadInterface[T <: Data](
    dType : T
  ) extends Bundle {
    val i_en     = Input(Bool())
    val o_data   = Output(dType)
    val o_nempty = Output(Bool())
  }

  class FIFOWriteInterface[T <: Data](
    dType : T
  ) extends Bundle {
    val i_we    = Input(Bool())
    val i_data  = Input(dType)
    val o_nfull = Output(Bool())
  }
}
