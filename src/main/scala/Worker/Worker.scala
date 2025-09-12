/* Worker.scala -- Processing element (PE) module, matmul pipeline
 *                 unit
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
package matmul.worker

import chisel3._
import chisel3.util._

import matmul.utils.BetterBRAM
import mac._
import matmul.worker.interfaces._
import matmul.utils.Parameters

class Worker(
  PARAM : Parameters
) extends Module {
  // If hardfloat is used, float is 32-bit
  // require((PARAM.USE_HARDFLOAT && DW == 32) || (!PARAM.USE_HARDFLOAT))
  /* I/O */
  private val WID_W = log2Up(PARAM.M_HEIGHT)
  // Worker ID: Passed as input to prevent Chisel from creating lots
  // of identical modules that just change by one parameter
  val wid = IO(Input(UInt(WID_W.W)))
  // Input and output are the same
  val i   = IO(Input(new WorkerInterface(PARAM.DW)))
  val o   = IO(Output(new WorkerInterface(PARAM.DW)))

  /* INTERNALS */
  // Input buffer
  val iReg    = RegNext(i)
  // Output buffer
  val oReg    = RegNext(iReg)
  // Worker memory (matrix coefficients in float32)
  // val wkMem   = SyncReadMem(PARAM.M_WIDTH, UInt(PARAM.DW.W))
  val wkMem   = Module(new BetterBRAM(PARAM.DW, PARAM.M_WIDTH, 1))
  // Memory address pointer
  val mPtrReg = RegInit(0.U(log2Up(PARAM.M_WIDTH).W))
  // Worker counter
  // NB: Not really optimal since each worker only needs to count up
  // to its wid, but setting wid as a parameter would make Chisel
  // generate tons of SV modules that only differ by their WID
  val wCntReg = RegInit(0.U(log2Up(PARAM.M_HEIGHT).W))
  // State registers
  // Forward prog data to next worker
  val pFwdReg = RegInit(false.B)
  // Wires
  // Incoming data must be accumulated (valid, not to be programmed
  // nor forwarded nor previous worker writing data)
  val iDoAcc  = i.valid & ~i.prog & ~i.write
  // Coefficient (memory content)
  // When input data must be accumulated, read memory to get coeff
  // ready for next tick, to be passed into the MAC
  val coeff   = Wire(UInt(PARAM.DW.W))
  // val coeff   = RegInit(0.U(PARAM.DW.W))
  // coeff      := wkMem.read(mPtrReg, iDoAcc)
  // Memory wiring
  wkMem.io.i_clk  := clock
  wkMem.io.i_rst  := reset
  wkMem.io.i_addr := mPtrReg
  wkMem.io.i_data := i.data
  wkMem.io.i_en   := iDoAcc
  wkMem.io.i_we   := i.valid & i.prog & (wCntReg === (PARAM.M_HEIGHT - 1).U - wid)
  coeff           := wkMem.io.o_data

  // when(i.valid & i.prog) {
  //   when(wCntReg === (PARAM.M_HEIGHT - 1).U - wid) {
  //     wkMem.write(mPtrReg, i.data)
  //   }
  // }

  // MAC result wire
  val macRes  = Wire(UInt(PARAM.DW.W))

  /* STATE LOGIC */
  // When data comes in with valid & prog set, program memory with the
  // first M_WIDTH values, then forward

  // When data comes in with valid set but not prog nor write, forward
  // AND accumulate

  // When data comes in with valid & write set, forward the first
  // WID - 1 values, then send own accumulator

  // Counter logic
  when(i.valid) {
    when(i.prog) {
      // When worker counter reaches M_HEIGHT - WID - 1, prog data is
      // for this worker, so write it in the memory, counting with
      // mPtrReg. Before that, prog data is forwarded. After that, new
      // prog data will be treated as new data for the last block
      mPtrReg := mPtrReg + 1.U
      // Counter logic
      when(mPtrReg === (PARAM.M_WIDTH - 1).U) {
        mPtrReg := 0.U
        wCntReg := wCntReg + 1.U
        when(wCntReg === (PARAM.M_HEIGHT - 1).U - wid) {
          // Reset wCntReg
          wCntReg := 0.U
        }
      }
    } .elsewhen(i.write) {
      wCntReg := wCntReg + 1.U
      // When wCntReg reaches wid, reset wCntReg and send own data.
      // Otherwise, just forward incoming data
      when(wCntReg === wid) {
        wCntReg := 0.U
      }
    } .otherwise {
      // When receiving valid data that is not prog nor write,
      // accumulate and count inputs
      mPtrReg := mPtrReg + 1.U
      when(mPtrReg === (PARAM.M_WIDTH - 1).U) {
        mPtrReg := 0.U
      }
    }
  } .elsewhen(RegNext(i.valid & i.write) & wCntReg === wid) {
    wCntReg := 0.U
  }

  // Send result logic
  val sendAcc = Wire(Bool())
  val gotLastInputReg = RegInit(false.B)
  // Additional pipeline ticks
  // When using SAF: accumulator is pipelined (1 tick) and conversion
  // to expanded F32 is pipelined (4 ticks)
  // Ticks: 1 || 6 (tested)
  val MAC_ADD_TICKS   = if(PARAM.USE_HARDFLOAT) { 2 } else { 7 }
  val MAC_TICKS       = PARAM.MAC_DSP_PIPELINE_REGS + MAC_ADD_TICKS
  val waitForMacReg   = RegInit(0.U((MAC_TICKS).W))
  when(wid === 0.U) {
    // Wait for MAC pipeline
    when(iReg.valid & ~iReg.prog & (RegNext(mPtrReg) === (PARAM.M_WIDTH - 1).U)) {
      gotLastInputReg := true.B
      waitForMacReg   := -1.S(MAC_TICKS.W).asUInt
    }
    // Wait for MAC pipeline (shift register)
    when(gotLastInputReg) {
      waitForMacReg := waitForMacReg << 1
    }
    // Set sendAcc when done
    sendAcc := gotLastInputReg & waitForMacReg === 0.U
    // Reset flags on sendAcc
    when(sendAcc) {
      gotLastInputReg := false.B
      waitForMacReg   := 0.U
    }
  } .otherwise {
    gotLastInputReg := false.B
    waitForMacReg   := 0.U
    sendAcc         := RegNext(
      iReg.valid & iReg.write & ~iReg.prog & wCntReg === wid
    )
  }

  // Output logic
  when(iReg.valid) {
    when(iReg.prog & RegNext(wCntReg) >= (PARAM.M_HEIGHT - 1).U - wid) {
      oReg := 0.U.asTypeOf(oReg)
    } .otherwise {
      when(wid === (PARAM.M_HEIGHT - 1).U & ~iReg.write) {
        oReg := 0.U.asTypeOf(oReg)
      } .otherwise {
        oReg := iReg
      }
    }
  } .elsewhen(sendAcc) {
    oReg.data  := macRes
    oReg.valid := true.B
    oReg.write := true.B
    oReg.prog  := false.B
    // Reset worker counter
    wCntReg    := 0.U
  } .otherwise {
    oReg       := 0.U.asTypeOf(oReg)
  }

  // Output register
  o := oReg

  // When data comes with valid & prog: program data until mPtrReg
  // reaches its max. Then, reset mPtrReg and set iFwdReg to forward
  // data as long as it comes with data & prog. When data comes in
  // with valid & ~prog, disable iFwdReg and start accumulating. When
  // mPtrReg reaches its max again, accumulation is done, so send
  // accumulator content to next worker with write flag on

  /* MAC MODULE */
  val mac = Module(new MAC(
    PARAM.USE_HARDFLOAT,
    PARAM.DW,
    PARAM.SAF_L,
    PARAM.SAF_W,
    PARAM.MAC_DSP_PIPELINE_REGS
  ))
  when(RegNext(iReg.valid & ~iReg.prog & ~iReg.write)) {
    mac.io.i_a := coeff
    mac.io.i_b := RegNext(iReg.data)
  } .otherwise {
    mac.io.i_a := 0.U
    mac.io.i_b := 0.U
  }
  mac.io.i_acc := RegNext(RegNext(iDoAcc))
  macRes       := mac.io.o_res
  mac.io.i_rst := sendAcc
}
