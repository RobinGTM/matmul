package asyncfifo

// Chisel
import chisel3._
import chisel3.util._

package object interfaces {
  // Async FIFO read port
  class AsyncFIFOReadInterface[T <: Data](
    dType : T = UInt(32.W)
  ) extends Bundle {
    val i_en     = Input(Bool())
    val o_nempty = Output(Bool())
    val o_data   = Output(dType)
  }

  // Async FIFO write port
  class AsyncFIFOWriteInterface[T <: Data](
    dType : T = UInt(32.W)
  ) extends Bundle {
    val i_we    = Input(Bool())
    val o_ready = Output(Bool())
    val i_data  = Input(dType)
  }

  // NB: Memory interfaces are defined from the FIFO counter logic's
  // point of view. So, flipped with respect to what a memory would
  // use
  // Basic memory read interface
  class BasicMemReadInterface[T <: Data](
    AW    : Int,
    dType : T = UInt(32.W)
  ) extends Bundle {
    val i_en   = Output(Bool())
    val i_addr = Output(UInt(AW.W))
    val o_data = Input(dType)
  }

  // Basic memory write interface
  class BasicMemWriteInterface[T <: Data](
    AW    : Int,
    dType : T = UInt(32.W)
  ) extends Bundle {
    val i_we   = Output(Bool())
    val i_addr = Output(UInt(AW.W))
    val i_data = Output(dType)
  }
}
