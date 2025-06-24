package adapters

// Chisel
import chisel3._
import chisel3.util._

// Local
import axi.interfaces._
import asyncfifo.interfaces._

class AXIMemory2FIFO(
  AXI_AW : Int,
  AXI_DW  : Int
) extends Module {
  // Write to FIFO
  val fifo_wr  = IO(Flipped(new AsyncFIFOWriteInterface(UInt(AXI_DW.W))))
  // AXI-MM write slave interface
  val s_axi_wr = IO(new SlaveAXIWriteInterface(AXI_AW, AXI_DW))

  // Receiving state register
  val recvReg = RegInit(false.B)

  // Sending BRESP state register
  val bValidReg    = RegInit(false.B)
  s_axi_wr.bvalid := bValidReg

  // Forward writes to FIFO
  // Ready to receive address when FIFO is ready (address is ignored) and when
  // not already reading or sending BRESP
  s_axi_wr.awready := fifo_wr.o_ready & ~recvReg & ~bValidReg
  // awlen is always 8-bit
  val awLenCntReg = RegInit(0.U(8.W))
  // awlen and recvReg state logic
  when(s_axi_wr.awvalid & s_axi_wr.awready) {
    awLenCntReg := s_axi_wr.awlen
    recvReg     := true.B
  }

  when(s_axi_wr.wvalid & s_axi_wr.wready) {
    when(~(awLenCntReg === 0.U)) {
      awLenCntReg := awLenCntReg - 1.U
    }
  }

  // Generate bresp (always ok)
  when(s_axi_wr.wlast) {
    bValidReg := true.B
    recvReg   := false.B
  }

  // Stop sending bresp after handshake
  when(s_axi_wr.bvalid & s_axi_wr.bready) {
    bValidReg := false.B
  }

  // Forward wdata (wstrb is ignored)
  s_axi_wr.wready  := fifo_wr.o_ready & recvReg & ~bValidReg
  fifo_wr.i_data   := s_axi_wr.wdata
  fifo_wr.i_we     := s_axi_wr.wvalid
}
