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
  val maA = i_safA(SAF_W - (EW - L) - 1, 0)
  val reB = i_safB(SAF_W - 1, SAF_W - (EW - L))
  val maB = i_safB(SAF_W - (EW - L) - 1, 0)

  // Sum
  val sumRe = Mux(reA > reB,
    reA,
    reB
  )
  val aShamt = sumRe - reA
  val bShamt = sumRe - reB
  val sumMa  = (maA >> (aShamt << L)) + (maB >> (bShamt << L))

  when(sumMa === 0.U) {
    o_res := 0.U
  } .otherwise {
    o_res := Cat(sumRe, sumMa)
  }
}
