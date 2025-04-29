package matmul

import chisel3._
import chisel3.util._

import matmul.worker.Worker
import matmul.interfaces._
import matmul.utils.Parameters

class MatMul(
  PARAM : Parameters
) extends Module {
  /* I/O */
  val i_prog    = IO(Input(Bool()))
  val in        = IO(new MatMulInput(PARAM.SAF_WIDTH))
  val out       = IO(new MatMulOutput(PARAM.SAF_WIDTH))
  val o_prog_ok = IO(Output(Bool()))

  /* MODULES */
  val workers = for(i <- 0 to PARAM.M_HEIGHT - 1) yield {
    Module(new Worker(PARAM, i))
  }

  /* PROG */
  // Count incoming prog values
  val progCntReg = RegInit(0.U(log2Up(PARAM.M_WIDTH * PARAM.M_HEIGHT).W))
  val progReg    = RegInit(false.B)
  when(i_prog) {
    progReg := true.B
    when(in.valid) {
      progCntReg := progCntReg + 1.U
      when(progCntReg === (PARAM.M_WIDTH * PARAM.M_HEIGHT - 1).U) {
        progReg := false.B
      }
    }
  }
  o_prog_ok := ~progReg & RegNext(progReg)

  /* WIRING */
  workers(0).in.data := in.data
  workers(0).in.work := in.valid
  workers(0).in.prog := i_prog
  for(i <- 1 to PARAM.M_HEIGHT - 1) {
    workers(i).in <> workers(i - 1).out
  }
  out.data  := workers(PARAM.M_HEIGHT - 1).out.data
  out.valid := workers(PARAM.M_HEIGHT - 1).out.work
}
