package matmul.axi

// Chisel
import chisel3._
import chisel3.util._

// Local
import matmul.axi.interfaces._
import matmul.fifo.interfaces._

class AXIReadAdapter(
  AXI_AW : Int,
  AXI_DW : Int
) extends Module {
  // AXI-MM read slave interface
  val s_axi_rd = IO(new SlaveAXIReadInterface(AXI_AW, AXI_DW))
  // FIFO read interface
  val fifo_rd  = IO(Flipped(new FIFOReadInterface(UInt(AXI_DW.W))))

  // arlen counter
  val arLenCntReg = RegInit(0.U(8.W))
  // Sending state register
  val rSendReg    = RegInit(false.B)
  // Ready to receive request when not sending
  s_axi_rd.arready := ~rSendReg

  // AR channel handshake
  val arHandshake = s_axi_rd.arvalid & s_axi_rd.arready
  // R channel handshake
  val rHandshake  = s_axi_rd.rvalid & s_axi_rd.rready

  // Output buffer
  val oBuf = Module(new FIFO2AXIOutBuf(UInt(AXI_DW.W)))
  oBuf.fifo_rd <> fifo_rd
  oBuf.i_ready := rSendReg & s_axi_rd.rready

  s_axi_rd.rdata  := oBuf.o_data
  s_axi_rd.rvalid := oBuf.o_valid & rSendReg
  s_axi_rd.rlast  := (arLenCntReg === 0.U) & rSendReg

  // Start transaction upon receiving AR request
  when(arHandshake) {
    arLenCntReg := s_axi_rd.arlen
    rSendReg    := true.B
  }

  // arlen counter logic
  when(rSendReg & rHandshake) {
    when(arLenCntReg === 0.U) {
      rSendReg := false.B
    } .otherwise {
      arLenCntReg := arLenCntReg - 1.U
    }
  }
}
