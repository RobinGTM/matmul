package adapters

// Chisel
import chisel3._
import chisel3.util._

import math.pow

// Local
import axi.interfaces._
import asyncfifo.interfaces._

class AXIS2FIFO(
  AXIS_W : Int
) extends Module {
  // AXI-Stream slave
  val s_axis  = IO(new SlaveAXIStreamInterfaceNoTid(AXIS_W))
  // FIFO write interface
  val fifo_wr = IO(Flipped(new AsyncFIFOWriteInterface(UInt(AXIS_W.W))))

  // AXI-S tkeep is ignored
  fifo_wr.i_we   := s_axis.tvalid & s_axis.tready
  fifo_wr.i_data := s_axis.tdata
  s_axis.tready  := fifo_wr.o_ready

  // tlast and tkeep are ignored
}
