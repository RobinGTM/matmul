package matmul.axi

import chisel3._
import chisel3.util.log2Up
// import chisel3.util.Enum

import math.pow

import matmul.utils.Parameters
import matmul.axi.interfaces._

class AXILiteSlave(
  AW                : Int,
  DW                : Int,
  CTL_OFFSET_BYTES  : Int = 0x0,
  N_ADDITIONAL_REGS : Int = 0
) extends Module {
  // AXI-Lite addresses point to each byte, but we want to address
  // C_S_AXI_DATA_WIDTH-bits widde registers, so drop LSBs
  // (https://zipcpu.com/blog/2020/03/08/easyaxil.html // ADDRLSB)
  // We only need to address every C_S_AXI_DATA_WIDTH position (no
  // sub-words)
  private val DROP_LSBS = log2Up(DW) - 3
  // Total number of registers to address (a control register is always
  // assumed)
  private val TOTAL_NREG = N_ADDITIONAL_REGS + 1
  // Base offset must be a multiple of 4
  require(CTL_OFFSET_BYTES % pow(2, DROP_LSBS) == 0);
  /* Constants */
  // Define control offset in word address
  private val CTL_OFFSET     = (CTL_OFFSET_BYTES / pow(2, DROP_LSBS)).toInt
  // Maximum address
  private val ADDR_END       = CTL_OFFSET + N_ADDITIONAL_REGS + 1
  // Define width for internal addresses
  private val INT_ADDR_WIDTH = AW - DROP_LSBS - 1
  // Define offsets as UInts
  private val CTL_OFFSET_U   = (CTL_OFFSET).U(INT_ADDR_WIDTH.W)
  private val ADDR_END_U     = ADDR_END.U(INT_ADDR_WIDTH.W)

  // AXI-Lite interface
  val s_axil = IO(new SlaveAXILiteInterface(AW, DW))

  // Simplified, write-only AXI-Lite controller that just has 1 register, reg 0
  val o_data = IO(Output(UInt(DW.W)))
  val o_we   = IO(Output(Bool()))

  /* READ CHANNEL (DISABLED) */
  s_axil.arready := false.B
  s_axil.rdata   := 0.U
  s_axil.rresp   := 0.U
  s_axil.rvalid  := false.B

  /* WRITE CHANNEL */
  val awHandshake = s_axil.awvalid & s_axil.awready
  val wHandshake = s_axil.wvalid & s_axil.wready

  // Ready after wvalid is set and not waiting for ack
  val awReadyReg = RegInit(false.B)
  when(~awReadyReg & (s_axil.awvalid & s_axil.wvalid)) {
    awReadyReg := true.B
  } .elsewhen(wHandshake) {
    awReadyReg := false.B
  }
  // Synchronize W and AW channels
  s_axil.awready := awReadyReg
  s_axil.wready  := awReadyReg

  // Force channel sync
  o_we   := awHandshake & wHandshake
  o_data := Mux(o_we, s_axil.wdata, 0.U)

  // Write response
  // Always OK
  s_axil.bresp := 0.U
  // Stall bus until receiving the ack pulse from the MCP
  val bHandshake = s_axil.bready & s_axil.bvalid
  val bValidReg = RegInit(false.B)
  when(o_we & ~bHandshake) {
    bValidReg := true.B
  } .elsewhen(bHandshake) {
    bValidReg := false.B
  }
  s_axil.bvalid := bValidReg | o_we
}
