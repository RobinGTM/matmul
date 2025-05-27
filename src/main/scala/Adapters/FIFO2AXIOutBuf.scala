package adapters

// Chisel
import chisel3._
import chisel3.util._

import math.pow

// Local
import matmul.axi.interfaces._
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

  // Data will be available on next tick but bus isn't ready: must
  // store it in the output buffer in case the bus is still not ready
  // on next tick
  val nemptyNotReady = fifo_rd.o_nempty & ~i_ready
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
  o_valid := RegNext(fifo_rd.o_nempty) | sendBufReg
}
