package matmul.fifo

import chisel3._
import chisel3.util._

import matmul.fifo.interfaces._

class FIFO[T <: Data](
  dType : T,
  DEPTH : Int
) extends Module {
  private val PTR_W = log2Up(DEPTH)

  /* I/O */
  val rd = IO(new FIFOReadInterface(dType))
  val wr = IO(new FIFOWriteInterface(dType))

  /* INTERNAL MEMORY */
  val mem = SyncReadMem(DEPTH, dType)

  /* POINTERS */
  val rCntReg = RegInit(0.U((PTR_W + 1).W))
  val wCntReg = RegInit(0.U((PTR_W + 1).W))
  val rPtr    = rCntReg(PTR_W - 1, 0)
  val wPtr    = wCntReg(PTR_W - 1, 0)
  dontTouch(rPtr)
  dontTouch(wPtr)

  /* INTERNALS */
  val fifoFull  = (rCntReg(PTR_W) ^ wCntReg(PTR_W)) & (rPtr === wPtr)
  val fifoEmpty = (rCntReg === wCntReg)

  /* OUTPUTS AND COUNTER LOGIC */
  rd.o_nempty := ~fifoEmpty
  wr.o_nfull  := ~fifoFull

  when(wr.i_we & ~fifoFull) {
    wCntReg := wCntReg + 1.U
    mem.write(wPtr, wr.i_data)
  }

  when(rd.i_en & ~fifoEmpty) {
    rCntReg := rCntReg + 1.U
  }

  rd.o_data := mem.read(rPtr, ~fifoEmpty)
}
