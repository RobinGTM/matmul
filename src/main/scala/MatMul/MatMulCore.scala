/* MatMulCore.scala -- Applicative core that instantiates the
 *                     processing elements
 *
 * (C) Copyright 2025 Robin Gay <robin.gay@polymtl.ca>
 *
 * This file is part of matmul.
 *
 * matmul is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * matmul is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with matmul. If not, see <https://www.gnu.org/licenses/>.
 */
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
  val iCntReg  = RegInit(0.U(log2Up(PARAM.M_WIDTH).W))
  // Output counter
  val oCntReg  = RegInit(0.U(log2Up(PARAM.M_HEIGHT).W))

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
  workers(0).i.valid := i.valid & readyReg
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
  when(i.valid & readyReg & ~i.prog) {
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
