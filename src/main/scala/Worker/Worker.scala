package matmul.worker

import chisel3._
import chisel3.util._

import mac._
import matmul.worker.interfaces._
import matmul.utils.Parameters

class Worker(
  PARAM : Parameters
) extends Module {
  // If hardfloat is used, float is 32-bit
  // require((PARAM.USE_HARDFLOAT && DW == 32) || (!PARAM.USE_HARDFLOAT))
  /* I/O */
  private val WID_W = log2Up(PARAM.M_HEIGHT)
  // Worker ID: Passed as input to prevent Chisel from creating lots
  // of identical modules that just change by one parameter
  val wid = IO(Input(UInt(WID_W.W)))
  // Input and output are the same
  val i   = IO(Input(new WorkerInterface(PARAM.DW)))
  val o   = IO(Output(new WorkerInterface(PARAM.DW)))

  /* INTERNALS */
  // Input buffer
  val iReg    = RegNext(i)
  // Accumulator
  val accReg  = RegInit(0.U(PARAM.DW.W))
  // Output buffer
  val oReg    = RegNext(iReg)
  // Worker memory (matrix coefficients in float32)
  val wkMem   = SyncReadMem(PARAM.M_WIDTH, UInt(PARAM.DW.W))
  // Memory address pointer
  val mPtrReg = RegInit(0.U(log2Up(PARAM.M_WIDTH).W))
  // Worker counter
  // NB: Not really optimal since each worker only needs to count up
  // to its wid, but setting wid as a parameter would make Chisel
  // generate tons of SV modules that only differ by their WID
  val wCntReg = RegInit(0.U(log2Up(PARAM.M_HEIGHT).W))
  // State registers
  // Forward prog data to next worker
  val pFwdReg = RegInit(false.B)
  // Wires
  // Incoming data must be accumulated (valid, not to be programmed
  // nor forwarded nor previous worker writing data)
  val iDoAcc  = i.valid & ~i.prog & ~i.write
  // Coefficient (memory content)
  // When input data must be accumulated, read memory to get coeff
  // ready for next tick, to be passed into the MAC
  val coeff   = Wire(UInt(PARAM.DW.W))
  coeff      := wkMem.read(mPtrReg, iDoAcc)

  /* STATE LOGIC */
  // When data comes in with valid & prog set, program memory with the
  // first M_WIDTH values, then forward

  // When data comes in with valid set but not prog nor write, forward
  // AND accumulate

  // When data comes in with valid & write set, forward the first
  // WID - 1 values, then send own accumulator

  // Counter logic
  when(i.valid) {
    when(i.prog) {
      // When worker counter reaches M_HEIGHT - WID - 1, prog data is
      // for this worker, so write it in the memory, counting with
      // mPtrReg. Before that, prog data is forwarded. After that, new
      // prog data will be treated as new data for the last block
      mPtrReg := mPtrReg + 1.U
      // Counter logic
      when(mPtrReg === (PARAM.M_WIDTH - 1).U) {
        mPtrReg := 0.U
        wCntReg := wCntReg + 1.U
        when(wCntReg === (PARAM.M_HEIGHT - 1).U - wid) {
          // Reset wCntReg
          wCntReg := 0.U
        }
      }
    } .elsewhen(i.write) {
      wCntReg := wCntReg + 1.U
      // When wCntReg reaches wid, reset wCntReg and send own data.
      // Otherwise, just forward incoming data
      when(wCntReg === wid) {
        wCntReg := 0.U
      }
    } .otherwise {
      // When receiving valid data that is not prog nor write,
      // accumulate and count inputs
      mPtrReg := mPtrReg + 1.U
      when(mPtrReg === (PARAM.M_WIDTH - 1).U) {
        mPtrReg := 0.U
      }
    }
  } .elsewhen(RegNext(i.valid & i.write) & wCntReg === wid) {
    wCntReg := 0.U
  }

  // Programming logic
  when(i.valid & i.prog) {
    when(wCntReg === (PARAM.M_HEIGHT - 1).U - wid) {
      wkMem.write(mPtrReg, i.data)
    }
  }

  // Output logic
  val sendAcc = (
    RegNext(RegNext(wCntReg)) === wid &
    RegNext(
      oReg.valid & ~oReg.prog &
      (oReg.write | (wid === 0.U & RegNext(RegNext(mPtrReg)) === (PARAM.M_WIDTH - 1).U))
    )
  )

  when(oReg.valid) {
    when(oReg.prog & RegNext(RegNext(wCntReg)) >= (PARAM.M_HEIGHT - 1).U - wid) {
      o := 0.U.asTypeOf(o)
    } .otherwise {
      when(wid === (PARAM.M_HEIGHT - 1).U & ~oReg.write) {
        o := 0.U.asTypeOf(o)
      } .otherwise {
        o := oReg
      }
    }
  } .elsewhen(sendAcc) {
    o.data  := macRes
    o.valid := true.B
    o.write := true.B
    o.prog  := false.B
    // Reset worker counter
    wCntReg := 0.U
    // Reset accumulator
    accReg  := 0.U
  } .otherwise {
    o := 0.U.asTypeOf(o)
  }

  // When data comes with valid & prog: program data until mPtrReg
  // reaches its max. Then, reset mPtrReg and set iFwdReg to forward
  // data as long as it comes with data & prog. When data comes in
  // with valid & ~prog, disable iFwdReg and start accumulating. When
  // mPtrReg reaches its max again, accumulation is done, so send
  // accumulator content to next worker with write flag on

  /* MODULES */
  val macRes = Wire(UInt(PARAM.DW.W))
  // Allows plugging to a conditional module,
  // https://stackoverflow.com/questions/70390834/conditional-module-instantiation-in-chisel
  val mac : Module { def i_a : UInt; def i_b : UInt; def i_acc : Bool; def o_res : UInt } =
    if(PARAM.USE_HARDFLOAT) {
      Module(new HardMAC(PARAM.DW))
    } else {
      Module(new SAFMAC(PARAM.DW, PARAM.SAF_W, PARAM.SAF_L))
    }
  mac.i_a   := coeff
  mac.i_b   := iReg.data
  mac.i_acc := RegNext(iDoAcc)
  macRes    := mac.o_res
}
