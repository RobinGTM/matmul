package adapters

// Chisel
import chisel3._
import chisel3.util._

import math.pow

// Local
import axi.interfaces._
import asyncfifo.interfaces._

class FIFO2AXIS(
  AXIS_W : Int
) extends Module {
  // Master AXI-Stream interface
  val m_axis  = IO(new MasterAXIStreamInterfaceNoTid(AXIS_W))
  // FIFO read interface
  val fifo_rd = IO(Flipped(new AsyncFIFOReadInterface(UInt(AXIS_W.W))))

  // Output buffer
  val oBuf = Module(new FIFO2AXIOutBuf(UInt(AXIS_W.W)))
  oBuf.fifo_rd <> fifo_rd
  oBuf.i_ready := m_axis.tready

  // m_axis.tkeep  := (pow(2, AXIS_W / 8).toInt - 1).U
  m_axis.tvalid := oBuf.o_valid
  // Last is sent when FIFO becomes empty
  m_axis.tlast  := m_axis.tvalid & ~fifo_rd.o_nempty
  m_axis.tdata  := oBuf.o_data
}
