/* PipelinedHardfloatMultiplier.scala -- hardfloat-based pipelined
 *                                       multiplier
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
package hardfloat

import chisel3._
import chisel3.util._

class PipelinedMulFullRawFN(
  EXP_W             : Int = 8,
  SIG_W             : Int = 24,
  DSP_PIPELINE_REGS : Int = 3
) extends Module {
  /* I/O */
  val io = IO(new Bundle {
    val a = Input(new RawFloat(EXP_W, SIG_W))
    val b = Input(new RawFloat(EXP_W, SIG_W))
    val invalidExc = Output(Bool())
    val rawOut = Output(new RawFloat(EXP_W, SIG_W * 2 - 1))
  })

  // Shamelessly copied from hardfloat.MulFullRawFN
  val notSigNaN_invalidExc = ShiftRegister(
    (io.a.isInf && io.b.isZero) || (io.a.isZero && io.b.isInf), DSP_PIPELINE_REGS
  )
  val notNaN_isInfOut      = ShiftRegister(io.a.isInf || io.b.isInf, DSP_PIPELINE_REGS)
  val notNaN_isZeroOut     = ShiftRegister(io.a.isZero || io.b.isZero, DSP_PIPELINE_REGS)
  val notNaN_signOut       = ShiftRegister(io.a.sign ^ io.b.sign, DSP_PIPELINE_REGS)
  val common_sExpOut       = ShiftRegister(
    io.a.sExp + io.b.sExp - (1 << EXP_W).S,
    DSP_PIPELINE_REGS
  )
  val common_sigOut        = ShiftRegister(
    (io.a.sig * io.b.sig)(SIG_W * 2 - 1, 0),
    DSP_PIPELINE_REGS
  )
  /* WIRING */
  io.invalidExc    := (
    isSigNaNRawFloat(ShiftRegister(io.a, DSP_PIPELINE_REGS))
      || isSigNaNRawFloat(ShiftRegister(io.b, DSP_PIPELINE_REGS))
      || notSigNaN_invalidExc
  )
  io.rawOut.isInf  := notNaN_isInfOut
  io.rawOut.isZero := notNaN_isZeroOut
  io.rawOut.sExp   := common_sExpOut
  io.rawOut.isNaN  := (
    ShiftRegister(io.a.isNaN, DSP_PIPELINE_REGS)
      || ShiftRegister(io.b.isNaN, DSP_PIPELINE_REGS)
  )
  io.rawOut.sign   := notNaN_signOut
  io.rawOut.sig    := common_sigOut
}

class PipelinedMulRawFN(
  EXP_W             : Int = 8,
  SIG_W             : Int = 24,
  DSP_PIPELINE_REGS : Int = 3
) extends Module {
  val io = IO(new Bundle {
    val a          = Input(new RawFloat(EXP_W, SIG_W))
    val b          = Input(new RawFloat(EXP_W, SIG_W))
    val invalidExc = Output(Bool())
    val rawOut     = Output(new RawFloat(EXP_W, SIG_W + 2))
  })

  val mulFullRaw = Module(new PipelinedMulFullRawFN(EXP_W, SIG_W, DSP_PIPELINE_REGS))

  mulFullRaw.io.a := io.a
  mulFullRaw.io.b := io.b

  io.invalidExc := mulFullRaw.io.invalidExc
  io.rawOut     := mulFullRaw.io.rawOut
  io.rawOut.sig := {
    val sig = mulFullRaw.io.rawOut.sig
    Cat(sig >> (SIG_W - 2), sig(SIG_W - 3, 0).orR)
  }
}

class PipelinedMulRecFN(
  EXP_W             : Int = 8,
  SIG_W             : Int = 24,
  DSP_PIPELINE_REGS : Int = 3
) extends Module {
  val io = IO(new Bundle {
    val a              = Input(UInt((EXP_W + SIG_W + 1).W))
    val b              = Input(UInt((EXP_W + SIG_W + 1).W))
    val roundingMode   = Input(UInt(3.W))
    val detectTininess = Input(Bool())
    val out            = Output(UInt((EXP_W + SIG_W + 1).W))
    val exceptionFlags = Output(UInt(5.W))
  })

  /* MULTIPLIER */
  val mulRawFN = Module(new PipelinedMulRawFN(EXP_W, SIG_W, DSP_PIPELINE_REGS))

  mulRawFN.io.a := rawFloatFromRecFN(EXP_W, SIG_W, io.a)
  mulRawFN.io.b := rawFloatFromRecFN(EXP_W, SIG_W, io.b)

  /* WIRING */
  val roundRawFNToRecFN =
    Module(new RoundRawFNToRecFN(EXP_W, SIG_W, 0))
  roundRawFNToRecFN.io.invalidExc     := mulRawFN.io.invalidExc
  roundRawFNToRecFN.io.infiniteExc    := false.B
  roundRawFNToRecFN.io.in             := mulRawFN.io.rawOut
  roundRawFNToRecFN.io.roundingMode   := io.roundingMode
  roundRawFNToRecFN.io.detectTininess := io.detectTininess
  io.out                              := roundRawFNToRecFN.io.out
  io.exceptionFlags                   := roundRawFNToRecFN.io.exceptionFlags
}
