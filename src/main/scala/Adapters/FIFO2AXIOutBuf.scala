/* FIFO2AXIOutBuf.scala -- Output buffer for FIFO output to AXI read
 *                         slave
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
package adapters

// Chisel
import chisel3._
import chisel3.util._

import math.pow

// Local
import axi.interfaces._
import asyncfifo.interfaces._

class FIFO2AXIOutBuf[T <: Data](
  dType : T
) extends Module {
  val fifo_rd = IO(Flipped(new AsyncFIFOReadInterface(dType)))
  val i_ready = IO(Input(Bool()))
  val o_data  = IO(Output(dType))
  val o_valid = IO(Output(Bool()))

  // Send buffer instead of FIFO output when FIFO has been incremented
  // but data didn't leave, i. e.:
  // - data is stored in output buffer
  // - FIFO was not empty but bus was not ready on previous tick
  // - bus is still not ready now
  val sendBufReg = RegInit(false.B)

  val outBufReg      = RegInit(0.U.asTypeOf(dType))
  val outLoadedReg   = RegInit(false.B)
  val loadOut        = o_valid & ~i_ready & ~outLoadedReg
  when(loadOut) {
    // Store FIFO output in case bus is not ready
    outLoadedReg := true.B
    outBufReg    := fifo_rd.o_data
  }

  // Must send the output buffer if it's not gone the tick after it
  // was loaded
  when(RegNext(loadOut)) {
    sendBufReg   := true.B
  }
  // Reset output buffer if it was sent on handshake
  when(o_valid & i_ready) {
    outLoadedReg := false.B
    sendBufReg   := false.B
  }

  // Enable FIFO when:
  // - bus is ready OR
  // - data is loaded in output buffer and was not read on previous
  //   tick
  fifo_rd.i_en := (loadOut & ~RegNext(fifo_rd.i_en)) | i_ready

  // Assign data to buffer or input data
  o_data  := Mux(sendBufReg | outLoadedReg, outBufReg, fifo_rd.o_data)
  o_valid := RegNext(fifo_rd.o_nempty & fifo_rd.i_en) | sendBufReg | outLoadedReg
}
