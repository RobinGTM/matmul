package axi

import chisel3._
import chisel3.util._

package object interfaces {
  class SimpleBusInterface(
    DW : Int
  ) extends Bundle {
    val data  = UInt(DW.W)
    val valid = Bool()
  }

  class SlaveAXIReadInterface(
    AW : Int,
    DW : Int
  ) extends Bundle {
    // AR channel
    val arvalid = Input(Bool())
    val arready = Output(Bool())
    val araddr  = Input(UInt(AW.W))
    // Hardcoded 8-bit (spec)
    val arlen   = Input(UInt(8.W))
    // R channel
    val rvalid  = Output(Bool())
    val rready  = Input(Bool())
    val rdata   = Output(UInt(DW.W))
    val rlast   = Output(Bool())
  }

  class SlaveAXIWriteInterface(
    AW : Int,
    DW : Int
  ) extends Bundle {
    // AW channel
    val awvalid = Input(Bool())
    val awready = Output(Bool())
    val awaddr  = Input(UInt(AW.W))
    // Hardcoded 8-bit (spec)
    val awlen   = Input(UInt(8.W))
    // W channel
    val wvalid  = Input(Bool())
    val wready  = Output(Bool())
    val wdata   = Input(UInt(DW.W))
    val wstrb   = Input(UInt((DW / 8).W))
    val wlast   = Input(Bool())
    // B channel
    val bvalid  = Output(Bool())
    val bready  = Input(Bool())
  }

  class SlaveAXIInterface(
    AW : Int = 64,
    DW : Int = 64
  ) extends Bundle {
    val araddr   = Input(UInt(AW.W))
    val arlen    = Input(UInt(8.W))
    val arready  = Output(Bool())
    val arvalid  = Input(Bool())
    val awaddr   = Input(UInt(AW.W))
    val awlen    = Input(UInt(8.W))
    val awready  = Output(Bool())
    val awvalid  = Input(Bool())
    // val bresp    = Output(UInt(2.W))
    val bready   = Input(Bool())
    val bvalid   = Output(Bool())
    val rdata    = Output(UInt(DW.W))
    // val rresp    = Output(UInt(2.W))
    val rlast    = Output(Bool())
    val rready   = Input(Bool())
    val rvalid   = Output(Bool())
    val wdata    = Input(UInt(DW.W))
    val wstrb    = Input(UInt((DW / 8).W))
    val wlast    = Input(Bool())
    val wready   = Output(Bool())
    val wvalid   = Input(Bool())
  }

  // Defined as the slave would need it
  class SlaveAXILiteInterface(
    AW : Int,
    DW : Int
  ) extends Bundle {
    //// AXI signals
    // Write address channel
    val awaddr  = Input(UInt(AW.W))
    val awvalid = Input(Bool())
    val awready = Output(Bool())
    // Write data channel
    val wdata   = Input(UInt(DW.W))
    val wstrb   = Input(UInt((DW / 8).W))
    val wvalid  = Input(Bool())
    val wready  = Output(Bool())
    // Write response channel
    val bresp   = Output(UInt(2.W))
    val bvalid  = Output(Bool())
    val bready  = Input(Bool())
    // Read address channel
    val araddr  = Input(UInt(AW.W))
    val arvalid = Input(Bool())
    val arready = Output(Bool())
    // Read data channel
    val rdata   = Output(UInt(DW.W))
    val rresp   = Output(UInt(2.W))
    val rvalid  = Output(Bool())
    val rready  = Input(Bool())
    // Interrupt (useless?)
    // val interrupt = Output(Bool())
  }

  class SlaveAXIStreamInterface(
    DW : Int,
    TW : Int // TID width
  ) extends Bundle {
    val tvalid = Input(Bool())
    val tready = Output(Bool())
    val tdata  = Input(UInt(DW.W))
    val tlast  = Input(Bool())
    // val tkeep  = Input(UInt((DW / 8).W))
    val tid    = Input(UInt(TW.W))
  }

  class SlaveAXIStreamInterfaceNoTid(
    DW : Int
  ) extends Bundle {
    val tvalid = Input(Bool())
    val tready = Output(Bool())
    val tdata  = Input(UInt(DW.W))
    val tlast  = Input(Bool())
    // val tkeep  = Input(UInt((DW / 8).W))
  }

  class MasterAXIStreamInterface(
    DW : Int,
    TW : Int // TID width
  ) extends Bundle {
    val tvalid = Output(Bool())
    val tready = Input(Bool())
    val tdata  = Output(UInt(DW.W))
    val tlast  = Output(Bool())
    // val tkeep  = Output(UInt((DW / 8).W))
    val tid    = Output(UInt(TW.W))
  }

  class MasterAXIStreamInterfaceNoTid(
    DW : Int
  ) extends Bundle {
    val tvalid = Output(Bool())
    val tready = Input(Bool())
    val tdata  = Output(UInt(DW.W))
    val tlast  = Output(Bool())
    // val tkeep  = Output(UInt((DW / 8).W))
  }
}
