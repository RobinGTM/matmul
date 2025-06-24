package matmul.worker

import chisel3._
import chisel3.util._

import saf._
import matmul.worker.interfaces._
import matmul.utils.Parameters
import hardfloat._

class Worker(
  // Data width
  DW            : Int,
  // Matrix height (number of workers)
  M_HEIGHT      : Int,
  // Matrix width (number of coefficients stored in each worker)
  M_WIDTH       : Int,
  // Whether or not to use the berkeley-hardfloat package for floating
  // point computations
  USE_HARDFLOAT : Boolean,
  SAF_L         : Int = 5,
  SAF_W         : Int = 70,
  SAF_B         : Int = 150,
  SAF_L2N       : Int = 16
) extends Module {
  // If hardfloat is used, float is 32-bit
  require((USE_HARDFLOAT && DW == 32) || (!USE_HARDFLOAT))
  /* I/O */
  private val WID_W = log2Up(M_HEIGHT)
  // Worker ID: Passed as input to prevent Chisel from creating lots
  // of identical modules that just change by one parameter
  val wid = IO(Input(UInt(WID_W.W)))
  // Input and output are the same
  val i   = IO(Input(new WorkerInterface(DW)))
  val o   = IO(Output(new WorkerInterface(DW)))

  /* INTERNALS */
  // Input buffer
  val iReg    = RegNext(i)
  // Multiplicator-adder pipeline register
  val macReg  = if(USE_HARDFLOAT) {
    // When using hardfloat, store recFNs, which are 1 bit wider
    RegInit(0.U((DW + 1).W))
  } else {
    RegInit(0.U((DW).W))
  }
  // Accumulator
  val accReg  = if(USE_HARDFLOAT) {
    // When using hardfloat, store recFNs, which are 1 bit wider
    RegInit(0.U((DW + 1).W))
  } else {
    RegInit(0.U((DW).W))
  }
  // Output buffer
  val oReg    = RegNext(iReg)
  // Worker memory (matrix coefficients)
  val wkMem   = SyncReadMem(M_WIDTH, UInt(DW.W))
  // Memory address pointer
  val mPtrReg = RegInit(0.U(log2Up(M_WIDTH).W))
  // Worker counter
  // NB: Not really optimal since each worker only needs to count up
  // to its wid, but setting wid as a parameter would make Chisel
  // generate tons of SV modules that only differ by their WID
  val wCntReg = RegInit(0.U(log2Up(M_HEIGHT).W))
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
  val coeff   = Wire(UInt(DW.W))
  coeff      := wkMem.read(mPtrReg, iDoAcc)
  dontTouch(coeff)

  val wCntRegNext = RegNext(wCntReg)
  val mPtrRegNext = RegNext(mPtrReg)
  dontTouch(wCntRegNext)
  dontTouch(mPtrRegNext)

  /* STATE LOGIC */
  // When data comes in with valid & prog set, program memory with the
  // first M_WIDTH values, then forward

  // When data comes in with valid set but not prog nor write, forward
  // AND accumulate

  // When data comes in with valid & write set, forward the first
  // WID - 1 values, then send own accumulator

  /* WIRING */
  // Logic acts on input register
  // Counter logic
  when(i.valid) {
    when(i.prog) {
      // When worker counter reaches M_HEIGHT - WID - 1, prog data is
      // for this worker, so write it in the memory, counting with
      // mPtrReg. Before that, prog data is forwarded. After that, new
      // prog data will be treated as new data for the last block
      mPtrReg := mPtrReg + 1.U
      // Counter logic
      when(mPtrReg === (M_WIDTH - 1).U) {
        mPtrReg := 0.U
        wCntReg := wCntReg + 1.U
        when(wCntReg === (M_HEIGHT - 1).U - wid) {
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
    }
  }

  // Output register logic and data routing
  when(iReg.valid) {
    when(iReg.prog) {
      //// RegNext(wCntReg)?????
      when(wCntReg === (M_HEIGHT - 1).U - wid) {
        //// RegNext(mPtrReg)?????
        wkMem.write(mPtrReg, i.data)
        o := 0.U.asTypeOf(o)
      } .otherwise {
        // Forward incoming data
        o := oReg
      }
    } .elsewhen(iReg.write) {
      //// RegNext(wCntReg)?????
      when(RegNext(wCntReg) === wid) {
        // Generate data: send own acc on the bus
        o.data  := accReg
        o.valid := true.B
        o.prog  := false.B
        o.write := true.B
      } .otherwise {
        // Forward incoming pipelined data
        o       := oReg
      }
    } .otherwise {
      o := oReg
    }
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
  if(USE_HARDFLOAT) {
    // hardfloat multiplier
    val hardMul   = Module(new MulRecFN(8, 24))
    hardMul.io.roundingMode   := 0.U
    hardMul.io.detectTininess := 0.U
    // hardfloat adder
    val hardAdder = Module(new AddRecFN(8, 24))
    hardAdder.io.subOp          := false.B
    hardAdder.io.roundingMode   := 0.U
    hardAdder.io.detectTininess := 0.U
    // Wiring
    hardMul.io.a    := recFNFromFN(8, 24, coeff)
    when(RegNext(iDoAcc)) {
      hardMul.io.b  := recFNFromFN(8, 24, iReg.data)
    }
    // MAC pipeline
    macReg := hardMul.io.out
    // Adder
    hardAdder.io.a := macReg
    hardAdder.io.b := accReg
    when(RegNext(RegNext(iDoAcc))) {
      accReg := hardAdder.io.out
    }
  } else {
    // SAF multiplier
    val safMul   = Module(new SAFMul(SAF_L, SAF_W, SAF_B, SAF_L2N))
    // SAF adder
    val safAdder = Module(new SAFAdder(SAF_L, SAF_W, SAF_B, SAF_L2N))
    // Multiplier wiring
    safMul.i_safA := coeff
    safMul.i_safB := iReg.data
    // MAC pipeline
    macReg := safMul.o_res
    // Adder wiring
    safAdder.i_safA := macReg
    safAdder.i_safB := accReg
    when(RegNext(RegNext(iDoAcc))) {
      accReg := safAdder.o_res
    }
  }
}
