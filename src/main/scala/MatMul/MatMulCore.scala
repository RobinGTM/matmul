package matmul

import chisel3._
import chisel3.util._

import matmul.worker.Worker
import matmul.interfaces._
import matmul.utils.Parameters

class MatMulCore(
  PARAM : Parameters
) extends Module {
  /* I/O */
  val i = IO(Input(new MatMulInterface(PARAM.DW)))
  val o = IO(Output(new MatMulInterface(PARAM.DW)))

  /* STATE */
  val readyReg = RegInit(true.B)
  // Input counter
  val iCntReg  = RegInit(0.U(PARAM.M_WIDTH.W))
  // Output counter
  val oCntReg  = RegInit(0.U(PARAM.M_HEIGHT.W))

  /* WORKER UNITS */
  val workers = for(w <- 0 to PARAM.M_HEIGHT - 1) yield {
    val wk = Module(new Worker(PARAM))
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
  for(w <- 1 to PARAM.M_HEIGHT - 1) {
    workers(w).i <> workers(w - 1).o
  }

  // Output
  o.data  := workers(PARAM.M_HEIGHT - 1).o.data
  // Only WRITE data is output
  o.valid := workers(PARAM.M_HEIGHT - 1).o.valid & workers(PARAM.M_HEIGHT - 1).o.write
  // No prog signal on output
  o.prog  := false.B

  /* COUNTER AND READY LOGIC */
  when(i.valid & ~i.prog) {
    // Count input data until last vector coeff
    iCntReg := iCntReg + 1.U
    when(iCntReg === (PARAM.M_WIDTH - 1).U) {
      iCntReg  := 0.U
      readyReg := false.B
    }
  } .elsewhen(workers(PARAM.M_HEIGHT - 1).o.valid) {
    // Count output data until last result coeff
    oCntReg := oCntReg + 1.U
    when(oCntReg === (PARAM.M_HEIGHT - 1).U) {
      oCntReg  := 0.U
      readyReg := true.B
    }
  }

  // Ready output
  o.ready := readyReg
}
