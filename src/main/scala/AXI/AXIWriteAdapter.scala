package matmul.axi

// Chisel
import chisel3._
import chisel3.util._

// Local
import matmul.axi.interfaces._
import matmul.interfaces._

class AXIWriteAdapter(
  AXI_AW : Int,
  AXI_DW  : Int
) extends Module {
  /* I/O */
  // Slave AXI write interface
  val s_axi_wr = IO(new SlaveAXIWriteInterface(AXI_AW, AXI_DW))
  // Simple bus output
  val out      = IO(Flipped(new MatMulInput(AXI_DW)))

  // Receiving state register
  val recvReg = RegInit(false.B)

  // Sending BRESP state register
  val bValidReg    = RegInit(false.B)
  s_axi_wr.bvalid := bValidReg

  // Ready when not already receiving data or sending bvalid
  s_axi_wr.awready := ~recvReg & ~bValidReg
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
  // Always ready to receive
  s_axi_wr.wready := true.B
  // Forward data and valid to simple bus
  out.data  := s_axi_wr.wdata
  out.valid := s_axi_wr.wvalid
}
