package matmul

// Chisel
import chisel3._
import chisel3.util._

import matmul.CoreWrapper
import vitis.axi.VitisAXIWrapper
import axi.interfaces._
import matmul.utils.Parameters

class VitisTopLevel(
  PARAM : Parameters
) extends RawModule {
  /* I/O */
  // AXI-Lite interface
  val s_axil      = IO(new SlaveAXILiteInterface(PARAM.CTL_AW, PARAM.CTL_W))
  // AXI interface
  // Master is a flipped slave
  val m_axi       = IO(Flipped(new SlaveAXIInterface(PARAM.AXI_AW, PARAM.AXI_W)))
  val axi_aclk    = IO(Input(Clock()))
  val axi_aresetn = IO(Input(Bool()))
  val i_coreclk   = IO(Input(Clock()))
  val i_corearstn = IO(Input(Bool()))

  /* MODULES */
  val axiW = Module(new VitisAXIWrapper(
    PARAM.IFIFO_CNT_W,
    PARAM.OFIFO_CNT_W,
    PARAM.CTL_AW,
    PARAM.CTL_W,
    PARAM.AXI_AW,
    PARAM.AXI_W
  ))
  val core = Module(new CoreWrapper(PARAM))

  // FIFO memories
  // IFIFO is deeper because coefficients (WIDTH * HEIGHT values)
  // arrive there
  val iFifoMem = SyncReadMem(PARAM.IFIFO_DEPTH, UInt(32.W))
  val oFifoMem = SyncReadMem(PARAM.OFIFO_DEPTH, UInt(32.W))

  /* WIRING */
  // Core
  core.i_coreclk := i_coreclk
  core.i_arstn   := i_corearstn

  // Control register clock-domain crossing
  core.ctl_wr_xdst <> axiW.ctl_wr_xsrc
  core.ctl_ar_xdst <> axiW.ctl_ar_xsrc
  core.ctl_rd_xsrc <> axiW.ctl_rd_xdst
  // FIFO clock-domain crossing counters
  core.ififo_xwcnt <> axiW.ififo_xwcnt
  core.ififo_xrcnt <> axiW.ififo_xrcnt
  core.ofifo_xrcnt <> axiW.ofifo_xrcnt
  core.ofifo_xwcnt <> axiW.ofifo_xwcnt

  // AXI wrapper
  axiW.axi_aclk    := axi_aclk
  axiW.axi_arstn   := axi_aresetn

  axiW.s_axil      <> s_axil
  axiW.m_axi       <> m_axi

  /* FIFO MEMORY LOGIC */
  // Output FIFO
  // Write
  when(core.ofifo_wmem.i_we) {
    oFifoMem.write(core.ofifo_wmem.i_addr, core.ofifo_wmem.i_data, i_coreclk)
  }
  // Read
  axiW.ofifo_rmem.o_data := oFifoMem.read(
    axiW.ofifo_rmem.i_addr,
    axiW.ofifo_rmem.i_en,
    axi_aclk
  )
  // Input FIFO
  // Write
  when(axiW.ififo_wmem.i_we) {
    iFifoMem.write(axiW.ififo_wmem.i_addr, axiW.ififo_wmem.i_data, axi_aclk)
  }
  // Read
  core.ififo_rmem.o_data := iFifoMem.read(
    core.ififo_rmem.i_addr, core.ififo_rmem.i_en, i_coreclk
  )
}
