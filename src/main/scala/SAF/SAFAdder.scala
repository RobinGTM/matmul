package matmul.saf

import chisel3._
import chisel3.util._

import math.pow

// Outputs a + b in SAF
class SAFAdder(
  L   : Int = 5,
  W   : Int = 70,
  B   : Int = 150,
  L2N : Int = 16
) extends RawModule {
  private val SAF_W = W + 8 - L
  private val DW = 32
  private val EW = 8
  private val MW = 23

  /* I/O */
  val i_safA = IO(Input(UInt(SAF_W.W)))
  val i_safB = IO(Input(UInt(SAF_W.W)))
  val o_res  = IO(Output(UInt(SAF_W.W)))

  /* SUM */
  // Decompose inputs
  val reA = i_safA(SAF_W - 1, SAF_W - (EW - L))
  val maA = i_safA(SAF_W - (EW - L) - 1, 0).asSInt
  val reB = i_safB(SAF_W - 1, SAF_W - (EW - L))
  val maB = i_safB(SAF_W - (EW - L) - 1, 0).asSInt

  // Sum
  val sumRe = Mux(reA > reB,
    reA,
    reB
  )
  val aShamt = sumRe - reA
  val bShamt = sumRe - reB
  val sumMa  = (maA >> (aShamt << L)) +& (maB >> (bShamt << L))

  // Prime SAF conversion
  // Normalize if mantissa has its MSB set while none of the input
  // mantissae were negative (possible?)
  // Also normalize if the sum has its 2nd MSB set, otherwise it will
  // be interpreted as negative
  val outMa = Wire(UInt(W.W))
  val outRe = Wire(UInt((EW - L).W))
  when((sumMa(W) | sumMa(W - 1)) & ~maA(W - 1) & ~maB(W - 1)) {
    outMa := (sumMa.asUInt >> (1.U << L))(W - 1, 0)
    outRe := sumRe + 1.U
  } .otherwise {
    outMa := sumMa(W - 1, 0)
    outRe := sumRe
  }

  when(sumMa === 0.S) {
    o_res := 0.U
  } .otherwise {
    o_res := Cat(outRe, outMa)
  }
}
