package adapters

// Chisel
import chisel3._
import chisel3.util._

// Local
import matmul.utils.WrAddrData
import matmul.utils.BasicRegInterface
import mcp._
import mcp.interfaces._

class MCPCross2RegAdapter(
  AW : Int,
  DW : Int
) extends Module {
  /* I/O */
  // Cross interfaces: this module is located at the clock-domain boundary
  val wr_dst_cross = IO(Flipped(new MCPCrossSrc2DstInterface(WrAddrData(AW, DW))))
  val ar_dst_cross = IO(Flipped(new MCPCrossSrc2DstInterface(UInt(AW.W))))
  val rd_src_cross = IO(new MCPCrossSrc2DstInterface(UInt(DW.W)))
  // Block register interface
  val io_reg       = IO(Flipped(new BasicRegInterface(AW, DW)))

  /* MCPs */
  val wrMcpDst = Module(new MultiCyclePathDst(WrAddrData(AW, DW)))
  val arMcpDst = Module(new MultiCyclePathDst(UInt(AW.W)))
  val rdMcpSrc = Module(new MultiCyclePathSrc(UInt(DW.W)))

  /* WIRING */
  // Clock-domain crossing interfaces go outside
  wrMcpDst.io_cross <> wr_dst_cross
  arMcpDst.io_cross <> ar_dst_cross
  rdMcpSrc.io_cross <> rd_src_cross

  // Address: take write address if writing, read address otherwise
  // NB: Read address is arMcpDst.io_dst.data
  io_reg.i_addr := Mux(wrMcpDst.io_dst.load_pulse,
    wrMcpDst.io_dst.data.addr,
    arMcpDst.io_dst.data
  )
  // Write data
  io_reg.i_data := wrMcpDst.io_dst.data.data
  // Write enable is set by wrMcpDst.io_dst load_pulse
  io_reg.i_we   := wrMcpDst.io_dst.load_pulse
  // Read enable is set by arMcpDst.io_dst load_pulse
  io_reg.i_en   := arMcpDst.io_dst.load_pulse

  // Reads are asynchronous, so the read data can cross as soon as
  // address and read enable are set
  rdMcpSrc.io_src.cross_pulse := arMcpDst.io_dst.load_pulse
  // Read data MCP gets block read data
  rdMcpSrc.io_src.data        := io_reg.o_data

  // NB: rdMcpSrc.io_src.ack is ignored
}
