package matmul

import chisel3._
import chisel3.util._

import matmul.worker.Worker
import matmul.interfaces._
import matmul.utils.Parameters

class MatMul(
  M_HEIGHT      : Int = 16,
  M_WIDTH       : Int = 16,
  USE_HARDFLOAT : Boolean = false,
  SAF_L         : Int     = 5,
  SAF_W         : Int     = 70,
  SAF_B         : Int     = 150,
  SAF_L2N       : Int     = 16
) extends Module {
  // Data width
  private val DW = if(USE_HARDFLOAT) { 32 } else { 8 - SAF_L + SAF_W }

  /* I/O */
  val i = IO(Input(new MatMulInterface(DW)))
  val o = IO(Output(new MatMulInterface(DW)))

  /* WORKER UNITS */
  val workers = for(w <- 0 to M_HEIGHT - 1) yield {
    val wk = Module(new Worker(
      M_HEIGHT      = M_HEIGHT,
      M_WIDTH       = M_WIDTH,
      USE_HARDFLOAT = USE_HARDFLOAT,
      SAF_L         = SAF_L,
      SAF_W         = SAF_W,
      SAF_B         = SAF_B,
      SAF_L2N       = SAF_L2N
    ))
    // Plug WID
    wk.wid := w.U
    // Yield worker
    wk
  }

  /* WIRING */
  // Input
  workers(0).i.data  := i.data
  workers(0).i.valid := i.valid
  workers(0).i.prog  := i.prog
  // Worker 0 generates the WRITE command itself
  workers(0).i.write := false.B

  // Worker chain
  for(w <- 1 to M_HEIGHT - 1) {
    workers(w).i <> workers(w - 1).o
  }

  // Output
  o.data  := workers(M_HEIGHT - 1).o.data
  // Only WRITE data is output
  o.valid := workers(M_HEIGHT - 1).o.valid & workers(M_HEIGHT - 1).o.write
  // No prog signal on output
  o.prog  := false.B
}
