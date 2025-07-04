package saf

import chisel3._
import chisel3.util._

import math.pow

class SAFMul(
  L   : Int = 5,
  W   : Int = 70,
  B   : Int = 150,
  L2N : Int = 16
) extends Module {
  private val SAF_W = W + 8 - L
  private val DW = 32
  private val EW = 8
  private val MW = 23
  // Float bias
  private val FB = 127

  /* I/O */
  val i_safA = IO(Input(UInt(SAF_W.W)))
  val i_safB = IO(Input(UInt(SAF_W.W)))
  val o_res  = IO(Output(UInt(SAF_W.W)))

  /* MUL */
  // Decompose inputs
  val reA = i_safA(SAF_W - 1, SAF_W - (EW - L))
  val maA = i_safA(SAF_W - (EW - L) - 1, 0)
  val reB = i_safB(SAF_W - 1, SAF_W - (EW - L))
  val maB = i_safB(SAF_W - (EW - L) - 1, 0)
  val eitherZero = (maA === 0.U) | (maB === 0.U)
  val isNeg      = maA(W - 1) ^ maB(W - 1)

  // Multiplication
  // Add exponents
  val expt = Wire(UInt(EW.W))
  expt    := (reA << L) + (reB << L) - B.U
  val prodRe = expt(EW - 1, L)
  val resSh  = expt(L - 1, 0)

  // Multiply mantissae
  val m1 = Mux(maA(W - 1),
    1.U + ~maA,
    maA
  )
  val m2 = Mux(maB(W - 1),
    1.U + ~maB,
    maB
  )

  //// PIPELINE ////
  private val PROD_UMA_W = 2 * W + L
  val pipeProdUMaReg = RegInit(0.U(PROD_UMA_W.W))
  // Shift product to compensate for exponent reduction
  pipeProdUMaReg := (m1 * m2) << resSh
  val pipeProdReReg  = RegNext(prodRe)
  //// PIPELINE ////

  // Bring MSB back in right position: between W - 1 and 0
  val msbPos = PROD_UMA_W.U - PriorityEncoder(Reverse(pipeProdUMaReg))

  val prEx  = Wire(UInt((EW - L).W))
  val prUMa = Wire(UInt(PROD_UMA_W.W))
  val shamt = Wire(UInt(log2Up(PROD_UMA_W).W))
  when(msbPos > (W - 1).U) {
    shamt := 1.U + ((msbPos - (W - 1).U) >> L)
    prUMa := (pipeProdUMaReg >> (shamt << L)).asUInt(W - 1, 0)
    prEx  := pipeProdReReg + shamt
  } .otherwise {
    shamt := 0.U
    prUMa := pipeProdUMaReg(W - 1, 0)
    prEx  := pipeProdReReg
  }
  val prMa = Mux(RegNext(isNeg),
    1.U + ~prUMa(W - 1, 0),
    prUMa(W - 1, 0)
  )

  // Output result
  when(RegNext(eitherZero)) {
    o_res := 0.U
  } .otherwise {
    o_res := Cat(prEx, prMa)
  }
}
