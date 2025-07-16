package saf

import chisel3._
import chisel3.util._

// Prime-SAF aligner for multiplier output
class SAFMulNormalizer(
  L   : Int = 5,
  W   : Int = 70,
  L2N : Int = 16
) extends RawModule {
  private val SAF_W = W + 8 - L
  private val DW = 32
  private val EW = 8
  private val MW = 23
  private val PROD_MA_W = 2 * W + L
  require(W >= MW + 2 + (1 << L))

  /* I/O */
  val i_safmul    = IO(Input(UInt((2 * W + 8 - L).W)))
  val o_prime_saf = IO(Output(UInt(SAF_W.W)))

  // Whether mantissa is negative
  val prodRe  = i_safmul(SAF_W - 1, SAF_W - (EW - L))
  val prodMa  = i_safmul(SAF_W - (EW - L) - 1, 0)
  val isNeg   = prodMa( - 1)
  val isZero  = prodMa === 0.U
  val prodUMa = Wire(UInt(PROD_MA_W.W))
  when(isNeg) {
    prodUMa := 1.U + ~prodMa
  } .otherwise {
    prodUMa := prodMa
  }

  // Bring MSB back in right position: between W - 1 and 0
  val msbPos = PROD_MA_W.U - PriorityEncoder(Reverse(prodUMa))

  val prEx = Wire(UInt((EW - L).W))
  val prUMa = Wire(UInt(PROD_MA_W.W))
  val shamt = Wire(UInt(log2Up(PROD_MA_W).W))
  when(msbPos > (W - 1).U) {
    shamt := 1.U + ((msbPos - (W - 1).U) >> L)
    prUMa := (prodUMa >> (shamt << L)).asUInt(W - 1, 0)
    prEx  := prodRe + shamt
  } .otherwise {
    shamt := 0.U
    prUMa := prodUMa(W - 1, 0)
    prEx  := prodRe
  }
  val prMa = Mux(isNeg,
    1.U + ~prUMa(W - 1, 0),
    prUMa(W - 1, 0)
  )

  // Output prime SAF
  o_prime_saf := Mux(isZero, 0.U, Cat(prEx, prMa))
}
