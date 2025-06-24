package sim.utils

// Chisel
import chisel3._
import chisel3.util._

class ResetUnit(
  DURATION : Int,
  RST_VAL  : Boolean = true
) extends RawModule {
  val i_clk = IO(Input(Clock()))
  val i_rst = IO(Input(Bool()))
  val o_rst = IO(Output(Bool()))

  withClockAndReset(i_clk, i_rst.asAsyncReset) {
    val rstDelayReg = RegInit(VecInit(Seq.fill(DURATION)(false.B)))
    for(i <- 1 to DURATION - 1) {
      rstDelayReg(i) := rstDelayReg(i - 1)
    }
    rstDelayReg(0) := RST_VAL.B

    // Output reset is held for DURATION ticks
    o_rst := ~rstDelayReg(DURATION - 1)
  }
}
