package matmul.worker

import chisel3._
import chisel3.util._

import matmul.saf._
import matmul.utils.Parameters

import matmul.worker.interfaces._

// The Worker accumulates the matrix multiplication for one coefficient of the
// output
class Worker(
  PARAM : Parameters,
  WID   : Int
) extends Module {
  /* I/O */
  // Worker input
  val in  = IO(Input(new WorkerInterface(PARAM)))
  // Worker output
  val out = IO(Output(new WorkerInterface(PARAM)))

  /* INTERNAL REGISTERS */
  // Memory (ROM)
  val workerMem  = RegInit(VecInit(Seq.fill(PARAM.M_WIDTH)(0.U(PARAM.SAF_WIDTH.W))))
  // Accumulator
  val accReg     = RegInit(0.U(PARAM.SAF_WIDTH.W))
  // Input counter: counts the coefficients of the input vector
  val inCntReg   = RegInit(0.U(log2Up(PARAM.M_HEIGHT).W))
  // When input counter reaches M_HEIGHT (number of workers) - 1, send
  // accumulator data to output
  val writeReg   = RegInit(false.B)
  when(~writeReg & (inCntReg === (PARAM.M_HEIGHT - 1).U)) {
    writeReg := true.B
  }
  // Programming counter: when in.prog is asserted, count values, and forward
  // inputs when progCntReg reaches PARAM.M_WIDTH - 1
  val progCntReg = RegInit(0.U(log2Up(PARAM.M_WIDTH).W))
  val fwdProgReg = RegInit(false.B)

  /* MODULES */
  // SAF adder
  val safAdder = Module(new SAFAdder(PARAM.SAF_L, PARAM.SAF_W, PARAM.SAF_B, PARAM.SAF_L2N))
  // SAF multiplier
  val safMul   = Module(new SAFMul(PARAM.SAF_L, PARAM.SAF_W, PARAM.SAF_B, PARAM.SAF_L2N))

  /* MATRIX MULTIPLICATION WIRING */
  // Multiply matrix coeff in memory with input vector
  safMul.i_safA   := workerMem(inCntReg)
  safMul.i_safB   := in.data

  // Add to accumulator
  safAdder.i_safA := accReg
  safAdder.i_safB := safMul.o_res

  // Store back in accumulator (when working)
  when(in.work & ~writeReg) {
    accReg := safAdder.o_res
  }

  /* FSM LOGIC */
  // When receiving write, pass through the data from previous workers in the
  // pipeline, and append accumulator data
  when(in.work & ~writeReg) {
    inCntReg := inCntReg + 1.U
  }

  /* OUTPUT REGISTERS */
  val outReg = RegInit(0.U(PARAM.SAF_WIDTH.W))
  val wkReg  = RegInit(false.B)
  val prReg  = RegInit(false.B)
  out.work  := wkReg
  out.data  := outReg
  out.prog  := prReg
  when(in.prog) {
    when(fwdProgReg) {
      outReg := in.data
      prReg  := true.B
      wkReg  := false.B
    } .otherwise {
      workerMem(progCntReg) := in.data
      prReg  := false.B
      wkReg  := false.B
      progCntReg := progCntReg + 1.U
      when(progCntReg === (PARAM.M_WIDTH - 1).U) {
        fwdProgReg := true.B
      }
    }
  } .elsewhen(in.work) {
    prReg := false.B
    // Just passthrough
    if(WID == PARAM.M_HEIGHT - 1) {
      when(writeReg) {
        outReg := in.data
        wkReg  := in.work
      } .otherwise {
        outReg := 0.U
        wkReg  := false.B
      }
    } else {
      outReg := in.data
      wkReg  := in.work
    }
  } .elsewhen(~in.work & RegNext(in.work) & writeReg) {
    prReg    := false.B
    // When getting a falling edge on work while in write mode, send own data
    outReg   := accReg
    wkReg    := true.B
    // Reset write mode
    writeReg := false.B
    // Reset accumulator to be ready for next input
    accReg   := 0.U
    // Reset input counter
    inCntReg := 0.U
  } .otherwise {
    wkReg  := false.B
    prReg  := false.B
  }
}
