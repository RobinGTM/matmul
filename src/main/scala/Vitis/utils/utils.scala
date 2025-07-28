package vitis

import chisel3._
import chisel3.util._

/**
 * From https://github.com/Wolf-Tungsten/chisel-vitis-template/
 */
package object utils {

  /** Bundles */
  class AxiReadMasterBundle(val addrWidth: Int, val dataWidth: Int) extends Bundle {
    val ar = Decoupled(new Bundle {
      val addr = UInt(addrWidth.W)
      val len = UInt(8.W)
    })

    val r = Flipped(Decoupled(new Bundle {
      val data = UInt(dataWidth.W)
      val last = Bool()
    }))
  }

  class AxiWriteMasterBundle(val addrWidth: Int, val dataWidth: Int) extends Bundle {
    val aw = Decoupled(new Bundle {
      val addr = UInt(addrWidth.W)
      val len = UInt(8.W)
    })

    val w = Decoupled(new Bundle {
      val data = UInt(dataWidth.W)
      val strb = UInt((dataWidth / 8).W)
      val last = Bool()
    })

    val b = Flipped(Decoupled(new Bundle {}))
  }

  class RtlKernelBundle() extends Bundle {
    // Register Args
    val start_iron: UInt = Input(UInt(32.W))
    val stop_iron: UInt = Input(UInt(32.W))
    val restart_iron: UInt = Input(UInt(32.W))
    val program_iron: UInt = Input(UInt(32.W))
    val lambda_h: UInt = Input(UInt(32.W))
    val state: UInt = Output(UInt(32.W))
    val last_variation_round: UInt = Output(UInt(32.W))
    val last_variation_round_locked: UInt = Output(UInt(32.W))

    val read_address_fn: UInt = Input(UInt(64.W))
    val read_length_fn: UInt = Input(UInt(64.W))
    val read_address_ip: UInt = Input(UInt(64.W))
    val read_length_ip: UInt = Input(UInt(64.W))
    val read_address_mc: UInt = Input(UInt(64.W))
    val read_length_mc: UInt = Input(UInt(64.W))
    val write_address_prob: UInt = Input(UInt(64.W))

    // HBM/DDR ports
    // M00 is used for fixed nodes (IN)
    val m00_read  = new AxiReadMasterBundle(64, 512)
    // M01 is used for initial probabilities (IN)
    val m01_read = new AxiReadMasterBundle(64, 512)
    // M02 is used for memory content (IN)
    val m02_read = new AxiReadMasterBundle(64, 512)
    // M03 is used for probabilities (OUT)
    val m03_write = new AxiWriteMasterBundle(64, 512)
  }
}
