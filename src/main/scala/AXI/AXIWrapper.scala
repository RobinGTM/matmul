package matmul.axi

// Chisel
import chisel3._
import chisel3.util._

// Local
import mcp.interfaces._
import asyncfifo._
import asyncfifo.interfaces._
import adapters._
import matmul.axi.interfaces._
import matmul.utils._

class AXIWrapper(
  PARAM : Parameters
) extends RawModule {
  /* I/O */
  val axi_aclk  = IO(Input(Clock()))
  val axi_arstn = IO(Input(Bool()))

  // AXI interface
  val s_axi = IO(new SlaveAXIInterface(PARAM.AXI_AW, PARAM.AXI_W))

  // Clock domain crossing interfaces
  val ififo_xwcnt = IO(new MCPCrossSrc2DstInterface(UInt((PARAM.FIFO_CNT_W + 1).W)))
  val ififo_xrcnt = IO(Flipped(new MCPCrossSrc2DstInterface(UInt((PARAM.FIFO_CNT_W + 1).W))))
  // Write memory interface
  val ififo_wmem  = IO(new BasicMemWriteInterface(PARAM.FIFO_CNT_W))

  // Output FIFO crossing / mem signals
  val ofifo_xwcnt = IO(Flipped(new MCPCrossSrc2DstInterface(UInt((PARAM.FIFO_CNT_W + 1).W))))
  val ofifo_xrcnt = IO(new MCPCrossSrc2DstInterface(UInt((PARAM.FIFO_CNT_W + 1).W)))
  // Read memory interface
  val ofifo_rmem  = IO(new BasicMemReadInterface(PARAM.FIFO_CNT_W, UInt(PARAM.AXI_W.W)))

  withClockAndReset(axi_aclk, axi_arstn) {
    /* MODULES */
    val axiIf = Module(new SAXIRW2Full(PARAM.AXI_AW, PARAM.AXI_W))

    // Input FIFO write port
    val iAxiMm2Fifo = Module(new AXIMemory2FIFO(PARAM.AXI_AW, PARAM.AXI_W))
    val iFifoWrPort = Module(new AsyncFIFOWritePort(PARAM.FIFO_CNT_W, UInt(32.W)))

    // Output FIFO read port
    val oFifoRdPort = Module(new AsyncFIFOReadPort(PARAM.FIFO_CNT_W, UInt(32.W)))
    val oFifo2AxiMm = Module(new FIFO2AXIMemory(PARAM.AXI_AW, PARAM.AXI_W))

    /* WIRING */
    // External
    axiIf.s_axi <> s_axi
    // AXI-MM
    // Input
    iAxiMm2Fifo.s_axi_wr   <> axiIf.s_axi_wr
    iAxiMm2Fifo.fifo_wr    <> iFifoWrPort.fifo_wr
    iFifoWrPort.wcnt_cross <> ififo_xwcnt
    iFifoWrPort.rcnt_cross <> ififo_xrcnt
    iFifoWrPort.mem        <> ififo_wmem

    // Output
    oFifo2AxiMm.s_axi_rd   <> axiIf.s_axi_rd
    oFifo2AxiMm.fifo_rd    <> oFifoRdPort.fifo_rd
    oFifoRdPort.wcnt_cross <> ofifo_xwcnt
    oFifoRdPort.rcnt_cross <> ofifo_xrcnt
    oFifoRdPort.mem        <> ofifo_rmem
  }
}
