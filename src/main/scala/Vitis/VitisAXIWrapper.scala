package vitis.axi

// Chisel
import chisel3._
import chisel3.util._

// Local
import mcp.interfaces._
import asyncfifo._
import asyncfifo.interfaces._
import adapters._
import vitis.axi._
import axi._
import axi.interfaces._
import matmul.utils._

class VitisAXIWrapper(
  IFIFO_CNT_W : Int,
  OFIFO_CNT_W : Int,
  CTL_AW      : Int,
  CTL_W       : Int,
  AXI_AW      : Int,
  AXI_W       : Int
) extends RawModule {
  val axi_aclk  = IO(Input(Clock()))
  val axi_arstn = IO(Input(Bool()))
  val m_axi     = IO(Flipped(new SlaveAXIInterface(AXI_AW, AXI_W)))
  val s_axil    = IO(new SlaveAXILiteInterface(CTL_AW, CTL_W))

  // MCP clock-domain crossing interfaces
  val ctl_wr_xsrc = IO(new MCPCrossSrc2DstInterface(WrAddrData(CTL_AW, CTL_W)))
  val ctl_ar_xsrc = IO(new MCPCrossSrc2DstInterface(UInt(CTL_AW.W)))
  val ctl_rd_xdst = IO(Flipped(new MCPCrossSrc2DstInterface(UInt(CTL_W.W))))

  // Clock domain crossing interfaces
  val ififo_xwcnt = IO(new MCPCrossSrc2DstInterface(UInt((IFIFO_CNT_W + 1).W)))
  val ififo_xrcnt = IO(Flipped(new MCPCrossSrc2DstInterface(UInt((IFIFO_CNT_W + 1).W))))
  // Write memory interface
  val ififo_wmem  = IO(new BasicMemWriteInterface(IFIFO_CNT_W, UInt(32.W)))

  // Output FIFO crossing / mem signals
  val ofifo_xwcnt = IO(Flipped(new MCPCrossSrc2DstInterface(UInt((OFIFO_CNT_W + 1).W))))
  val ofifo_xrcnt = IO(new MCPCrossSrc2DstInterface(UInt((OFIFO_CNT_W + 1).W)))
  // Read memory interface
  val ofifo_rmem  = IO(new BasicMemReadInterface(OFIFO_CNT_W, UInt(32.W)))

  withClockAndReset(axi_aclk, ~axi_arstn) {
    val axiLiteSlave = Module(new AXILiteSlaveMCPWrapper(
      CTL_AW, CTL_W, 0x0, 4 // 2 regs for H/W, 1 reg for read addr, 1
                            // reg for write addr
    ))

    // Memory to stream
    val iAxiMm2S    = Module(new MM2S(AXI_AW, AXI_W))
    iAxiMm2S.io.req <> 0.U.asTypeOf(iAxiMm2S.io.req)
    val iAxiS2Fifo  = Module(new AXIS2FIFO(32))
    val iFifoWrPort = Module(new AsyncFIFOWritePort(IFIFO_CNT_W, UInt(32.W)))

    // Stream to memory
    val oFifoRdPort = Module(new AsyncFIFOReadPort(OFIFO_CNT_W, UInt(32.W)))
    val oFifo2AxiS  = Module(new FIFO2AXIS(32))
    val oAxiS2Mm    = Module(new S2MM(AXI_AW, AXI_W))
    oAxiS2Mm.io.req <> 0.U.asTypeOf(oAxiS2Mm.io.req)

    /* WIRING */
    // Control
    axiLiteSlave.s_axil       <> s_axil
    axiLiteSlave.wr_src_cross <> ctl_wr_xsrc
    axiLiteSlave.ar_src_cross <> ctl_ar_xsrc
    axiLiteSlave.rd_dst_cross <> ctl_rd_xdst

    // AXI-MM
    // AR
    m_axi.araddr  := iAxiMm2S.io.axiRead.ar.bits.addr
    m_axi.arlen   := iAxiMm2S.io.axiRead.ar.bits.len
    iAxiMm2S.io.axiRead.ar.ready := m_axi.arready
    m_axi.arvalid := iAxiMm2S.io.axiRead.ar.valid
    // R
    m_axi.rready                    := iAxiMm2S.io.axiRead.r.ready
    iAxiMm2S.io.axiRead.r.valid     := m_axi.rvalid
    iAxiMm2S.io.axiRead.r.bits.data := m_axi.rdata
    iAxiMm2S.io.axiRead.r.bits.last := m_axi.rlast
    // AW
    m_axi.awaddr  := oAxiS2Mm.io.axiWrite.aw.bits.addr
    m_axi.awlen   := oAxiS2Mm.io.axiWrite.aw.bits.len
    m_axi.awvalid := oAxiS2Mm.io.axiWrite.aw.valid
    oAxiS2Mm.io.axiWrite.aw.ready := m_axi.awready
    // W
    m_axi.wdata   := oAxiS2Mm.io.axiWrite.w.bits.data
    m_axi.wlast   := oAxiS2Mm.io.axiWrite.w.bits.last
    m_axi.wstrb   := Fill(AXI_W / 8, 1.U)
    m_axi.wvalid  := oAxiS2Mm.io.axiWrite.w.valid
    oAxiS2Mm.io.axiWrite.w.ready := m_axi.wready
    // B
    m_axi.bready  := oAxiS2Mm.io.axiWrite.b.ready
    oAxiS2Mm.io.axiWrite.b.valid := m_axi.bvalid

    // Streams
    // Input
    iAxiS2Fifo.s_axis.tdata     := iAxiMm2S.io.streamOut.bits.data
    iAxiS2Fifo.s_axis.tlast     := iAxiMm2S.io.streamOut.bits.last
    iAxiS2Fifo.s_axis.tvalid    := iAxiMm2S.io.streamOut.valid
    iAxiMm2S.io.streamOut.ready := iAxiS2Fifo.s_axis.tready
    // Output
    oAxiS2Mm.io.streamIn.bits.data := oFifo2AxiS.m_axis.tdata
    oAxiS2Mm.io.streamIn.bits.last := oFifo2AxiS.m_axis.tlast
    oAxiS2Mm.io.streamIn.valid     := oFifo2AxiS.m_axis.tvalid
    oFifo2AxiS.m_axis.tready       := oAxiS2Mm.io.streamIn.ready

    // FIFOs
    // Input
    iAxiS2Fifo.fifo_wr     <> iFifoWrPort.fifo_wr
    iFifoWrPort.wcnt_cross <> ififo_xwcnt
    iFifoWrPort.rcnt_cross <> ififo_xrcnt
    iFifoWrPort.mem        <> ififo_wmem
    // Output
    oFifo2AxiS.fifo_rd     <> oFifoRdPort.fifo_rd
    oFifoRdPort.wcnt_cross <> ofifo_xwcnt
    oFifoRdPort.rcnt_cross <> ofifo_xrcnt
    oFifoRdPort.mem        <> ofifo_rmem
  }
}
