package mac

import chisel3._
import chisel3.util._

import saf._
import saf.utils._

class SAFMAC(
  DW  : Int = 33,
  L   : Int = 5,
  W   : Int = 70
) extends Module {
  private val SAF_WIDTH = W + 8 - L
  /* I/O */
  val io    = IO(new MACInterface(DW))
  // Only for testing
  val o_saf = IO(Output(UInt(SAF_WIDTH.W)))

  /* MODULES */
  // Multiplier
  val safMul   = Module(new SAFMul(DW, L, W))
  // Adder
  val safAdder = Module(new SAFAdder(L, W))

  /* INTERNALS */
  val macReg = RegInit(0.U(SAF_WIDTH.W))
  val accReg = RegInit(0.U(SAF_WIDTH.W))

  safMul.i_a := io.i_a
  safMul.i_b := io.i_b

  macReg := safMul.o_saf

  safAdder.i_safA := macReg
  safAdder.i_safB := accReg

  when(RegNext(io.i_acc)) {
    accReg := safAdder.o_res
  }

  when(io.i_rst) {
    accReg := 0.U
  }

  // Output expanded float32
  io.o_res := SAFToExpF32(accReg)
  // Control output for testing
  o_saf := accReg
}
