/* HardMAC.scala -- berkeley-hardfloat-based MAC
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
package mac

import chisel3._
import chisel3.util._

import hardfloat._

class HardMAC(
  DW                : Int = 33,
  DSP_PIPELINE_REGS : Int = 2
) extends Module {
  /* I/O */
  val io = IO(new MACInterface(DW))

  /* MODULES */
  val hardMul   = Module(new MulRecFN(8, 24))
  hardMul.io.roundingMode     := 0.U
  hardMul.io.detectTininess   := 0.U
  val hardAdder = Module(new AddRecFN(8, 24))
  hardAdder.io.subOp          := false.B
  hardAdder.io.roundingMode   := 0.U
  hardAdder.io.detectTininess := 0.U

  /* INTERNALS */
  val macReg = RegInit(0.U(DW.W))
  val accReg = RegInit(0.U(DW.W))

  hardMul.io.a := io.i_a
  hardMul.io.b := io.i_b

  val dspPipelineRegs = RegInit(VecInit(
    Seq.fill(DSP_PIPELINE_REGS)(0.U.asTypeOf(hardMul.io.out))
  ))
  dspPipelineRegs(0) := hardMul.io.out
  for(i <- 1 to DSP_PIPELINE_REGS - 1) {
    dspPipelineRegs(i) := dspPipelineRegs(i - 1)
  }

  macReg := dspPipelineRegs(DSP_PIPELINE_REGS - 1)

  hardAdder.io.a := macReg
  hardAdder.io.b := accReg

  when(RegNext(RegNext(RegNext(io.i_acc)))) {
    accReg := hardAdder.io.out
  }

  when(io.i_rst) {
    accReg := 0.U
  }

  io.o_res := accReg
}
