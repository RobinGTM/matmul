package matmul

// Chisel
import chisel3._
import chisel3.util._

import adapters._
import asyncfifo._
import asyncfifo.interfaces._
import mcp._
import mcp.interfaces._
import saf._
import axi._
import axi.interfaces._
import matmul._
import matmul.utils._

class CoreWrapper(
  PARAM : Parameters
) extends RawModule {
  /* I/O */
  // Clk / rst
  val i_coreclk = IO(Input(Clock()))
  // Async reset (synchronized in the core)
  val i_arstn   = IO(Input(Bool()))

  // Control clock-domain crossing interfaces
  // Write address + data
  val ctl_wr_xdst = IO(Flipped(new MCPCrossSrc2DstInterface(WrAddrData(
    PARAM.CTL_AW, PARAM.CTL_W
  ))))
  // Read address
  val ctl_ar_xdst = IO(Flipped(new MCPCrossSrc2DstInterface(UInt(PARAM.CTL_AW.W))))
  // Read data
  val ctl_rd_xsrc = IO(new MCPCrossSrc2DstInterface(UInt(PARAM.CTL_W.W)))

  // Clock-domain crossing interfaces (FIFO pointers)
  // Input FIFO write counter
  val ififo_xwcnt = IO(Flipped(new MCPCrossSrc2DstInterface(UInt((PARAM.IFIFO_CNT_W + 1).W))))
  // Input FIFO read counter
  val ififo_xrcnt = IO(new MCPCrossSrc2DstInterface(UInt((PARAM.IFIFO_CNT_W + 1).W)))
  // Output FIFO write counter
  val ofifo_xwcnt = IO(new MCPCrossSrc2DstInterface(UInt((PARAM.OFIFO_CNT_W + 1).W)))
  // Output FIFO read counter
  val ofifo_xrcnt = IO(Flipped(new MCPCrossSrc2DstInterface(UInt((PARAM.OFIFO_CNT_W + 1).W))))
  // FIFO memory interfaces
  // FIFO memories contain float32
  val ififo_rmem  = IO(new BasicMemReadInterface(PARAM.IFIFO_CNT_W, UInt(32.W)))
  val ofifo_wmem  = IO(new BasicMemWriteInterface(PARAM.OFIFO_CNT_W, UInt(32.W)))

  /* RESET SYNCHRONIZER */
  // Reset is synchronized since it comes from a different clock domain
  val sync_rstn = withClock(i_coreclk) { RegNext(RegNext(i_arstn)) }

  // Reset is active low
  withClockAndReset(i_coreclk, ~sync_rstn) {
    /* MODULES */
    // MCP adapter for control register
    val mcpAdapter  = Module(new MCPCross2RegAdapter(PARAM.CTL_AW, PARAM.CTL_W))
    // Input FIFO read port
    val iFifoRdPort = Module(new AsyncFIFOReadPort(PARAM.IFIFO_CNT_W, UInt(32.W)))
    // Input FIFO to AXI-Stream adapter
    val iFifo2AxiS  = Module(new FIFO2AXIS(32))
    // Matrix multiplier core and controller
    val core        = Module(new MatMul(PARAM))
    // Output AXI-Stream to FIFO adapter
    val oAxiS2Fifo  = Module(new AXIS2FIFO(32))
    // Output FIFO write port
    val oFifoWrPort = Module(new AsyncFIFOWritePort(PARAM.OFIFO_CNT_W, UInt(32.W)))

    /* WIRING */
    // Clock-domain crossing
    mcpAdapter.wr_dst_cross <> ctl_wr_xdst
    mcpAdapter.ar_dst_cross <> ctl_ar_xdst
    mcpAdapter.rd_src_cross <> ctl_rd_xsrc
    // Simple register interface
    core.ctl_reg            <> mcpAdapter.io_reg

    // Input FIFO to AXI-Stream adapter
    iFifoRdPort.fifo_rd     <> iFifo2AxiS.fifo_rd
    // Input AXI-Stream goes to core input
    core.s_axis             <> iFifo2AxiS.m_axis
    // Core output AXI-Stream goes to AXI-Stream output
    core.m_axis             <> oAxiS2Fifo.s_axis
    // Output AXI-Stream to FIFO adapter
    oAxiS2Fifo.fifo_wr      <> oFifoWrPort.fifo_wr

    // Input FIFO memory interface
    iFifoRdPort.mem         <> ififo_rmem
    // Clock-domain crossing counters
    iFifoRdPort.wcnt_cross  <> ififo_xwcnt
    iFifoRdPort.rcnt_cross  <> ififo_xrcnt
    // Ouptut FIFO memory interface
    oFifoWrPort.mem         <> ofifo_wmem
    // Clock-domain crossing interfaces
    oFifoWrPort.rcnt_cross  <> ofifo_xrcnt
    oFifoWrPort.wcnt_cross  <> ofifo_xwcnt
  }
}
