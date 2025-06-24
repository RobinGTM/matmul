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
  val in  = IO(new MatMulInput(PARAM.SAF_WIDTH))
  val out = IO(new MatMulOutput(PARAM.SAF_WIDTH))

  // /* MODULES */
  // val workers = for(i <- 0 to PARAM.M_HEIGHT - 1) yield {
  //   Module(new PipelinedWorker(PARAM, i))
  // }

  // /* WIRING */
  // workers(0).in.data := in.data
  // workers(0).in.work := in.valid
  // for(i <- 1 to PARAM.M_HEIGHT - 1) {
  //   workers(i).in <> workers(i - 1).out
  // }
  // out.data  := workers(PARAM.M_HEIGHT - 1).out.data
  // out.valid := workers(PARAM.M_HEIGHT - 1).out.work
}
