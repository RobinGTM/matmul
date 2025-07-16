package mac

import chisel3._
import chisel3.util._

import saf._
import saf.utils._

class SAFMAC(
  DW  : Int = 33,
  W   : Int = 70,
  L   : Int = 5
) extends Module {
  private val SAF_WIDTH = W + 8 - L
  /* I/O */
  val i_a   = IO(Input(UInt(DW.W)))
  val i_b   = IO(Input(UInt(DW.W)))
  val i_acc = IO(Input(Bool()))
  val o_res = IO(Output(UInt(DW.W)))
  val o_saf = IO(Output(UInt(SAF_WIDTH.W)))

  /* MODULES */
  // Multiplier
  val safMul   = Module(new SAFMul(DW, L, W))
  // Adder
  val safAdder = Module(new SAFAdder(L, W))

  /* INTERNALS */
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

  // Output expanded float32
  o_res := SAFToExpF32(accReg)
  o_saf := accReg
}
