package mac

import chisel3._
import chisel3.util._

import saf._

class SAFMAC(
  DW  : Int = 33,
  W   : Int = 81,
  L   : Int = 5,
  B   : Int = 173,
  L2N : Int = 16
) extends Module {
  /* I/O */
  val i_a   = IO(Input(UInt(DW.W)))
  val i_b   = IO(Input(UInt(DW.W)))
  val i_acc = IO(Input(Bool()))
  val o_res = IO(Output(UInt(DW.W)))

  /* MODULES */
  // Multiplier
  val safMul   = Module(new SAFMul(L, W, B, L2N))
  // Adder
  val safAdder = Module(new SAFAdder(L, W, B, L2N))

  /* INTERNALS */
  private val SAF_WIDTH = W + 8 - L
  val macReg = RegInit(0.U(SAF_WIDTH.W))
  val accReg = RegInit(0.U(SAF_WIDTH.W))

  safMul.i_a := i_a
  safMul.i_b := i_b

  macReg := safMul.o_saf

  safAdder.i_safA := macReg
  safAdder.i_safB := accReg

  when(RegNext(i_acc)) {
    accReg := safAdder.o_res
  }

  o_res := accReg ////// TODO convert to expanded float
}
