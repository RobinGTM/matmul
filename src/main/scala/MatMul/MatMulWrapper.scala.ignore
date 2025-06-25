package matmul

// Chisel
import chisel3._
import chisel3.util._

import adapters._
import asyncfifo._
import asyncfifo.interfaces._
import mcp._
import mcp.interfaces._
import matmul._
import saf._
import axi._
import axi.interfaces._
import matmul.utils.Parameters

class MatMulWrapper(
  PARAM : Parameters
) extends RawModule {
  /* I/O */
  // Clk / rst
  val i_coreclk = IO(Input(Clock()))
  // Async reset (synchronized in the core)
  val i_arstn   = IO(Input(Bool()))

  // Clock-domain crossing interfaces (FIFO pointers)
  val ififo_xwcnt = IO(Flipped(new MCPCrossSrc2DstInterface(UInt((PARAM.FIFO_CNT_W + 1).W))))
  val ififo_xrcnt = IO(new MCPCrossSrc2DstInterface(UInt((PARAM.FIFO_CNT_W + 1).W)))
  val ofifo_xwcnt = IO(new MCPCrossSrc2DstInterface(UInt((PARAM.FIFO_CNT_W + 1).W)))
  val ofifo_xrcnt = IO(Flipped(new MCPCrossSrc2DstInterface(UInt((PARAM.FIFO_CNT_W + 1).W))))
  // FIFO memory interfaces
  // FIFO memories contains float32
  val ififo_rmem  = IO(new BasicMemReadInterface(PARAM.FIFO_CNT_W, UInt(32.W)))
  val ofifo_wmem  = IO(new BasicMemWriteInterface(PARAM.FIFO_CNT_W, UInt(32.W)))

  /* RESET SYNCHRONIZER */
  val sync_rstn = withClock(i_coreclk) { RegNext(RegNext(i_arstn)) }

  withClockAndReset(i_coreclk, ~sync_rstn) {
    /* MODULES */
    // Input FIFO read port
    val iFifoRdPort = Module(new AsyncFIFOReadPort(PARAM.FIFO_CNT_W, UInt(32.W)))
    // Float32 to SAF converter
    val f2SAF       = Module(
      new Float32ToSAF(PARAM.SAF_L, PARAM.SAF_W, PARAM.SAF_B, PARAM.SAF_L2N)
    )
    val matmul      = Module(new MatMul(PARAM))
    // SAF to Float32 converter
    val SAF2F       = Module(
      new SAFToFloat32(PARAM.SAF_L, PARAM.SAF_W, PARAM.SAF_B, PARAM.SAF_L2N)
    )
    // Output FIFO write port
    val oFifoWrPort = Module(new AsyncFIFOWritePort(PARAM.FIFO_CNT_W, UInt(32.W)))

    /* WIRING */
    // Memory interface
    iFifoRdPort.mem            <> ififo_rmem
    // Clock domain-crossing interfaces
    iFifoRdPort.wcnt_cross     <> ififo_xwcnt
    iFifoRdPort.rcnt_cross     <> ififo_xrcnt
    // Always reading as soon as data is available
    iFifoRdPort.fifo_rd.i_en   := true.B
    // Convert input from FIFO to SAF
    f2SAF.i_f32                := iFifoRdPort.fifo_rd.o_data
    // Feed matmul kernel
    matmul.in.data             := f2SAF.o_saf
    // FIFO is async read, so send valid 1 tick after nempty is
    // asserted
    matmul.in.valid            := RegNext(iFifoRdPort.fifo_rd.o_nempty)
    // Convert output back to float32
    SAF2F.i_saf                := matmul.out.data
    // Write to output FIFO as soon as data is valid (no "ready" mechanism)
    oFifoWrPort.fifo_wr.i_we   := matmul.out.valid
    oFifoWrPort.fifo_wr.i_data := SAF2F.o_f32
    // Output clock domain-crossing interfaces
    oFifoWrPort.wcnt_cross     <> ofifo_xwcnt
    oFifoWrPort.rcnt_cross     <> ofifo_xrcnt
    oFifoWrPort.mem            <> ofifo_wmem
  }
}
