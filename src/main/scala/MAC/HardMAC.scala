package mac

import chisel3._
import chisel3.util._

import hardfloat._

class HardMAC(
  DW : Int = 33
) extends Module {
  /* I/O */
  val i_a   = IO(Input(UInt(DW.W)))
  val i_b   = IO(Input(UInt(DW.W)))
  val i_acc = IO(Input(Bool()))
  val o_res = IO(Output(UInt(DW.W)))

  /* MODULES */
  val hardMul   = Module(new MulRecFN(8, 24))
  hardMul.io.roundingMode   := 0.U
  hardMul.io.detectTininess := 0.U
  val hardAdder = Module(new AddRecFN(8, 24))
  hardAdder.io.subOp          := false.B
  hardAdder.io.roundingMode   := 0.U
  hardAdder.io.detectTininess := 0.U

  /* INTERNALS */
  val macReg = RegInit(0.U(DW.W))
  val accReg = RegInit(0.U(DW.W))

  hardMul.io.a := i_a
  hardMul.io.b := i_b

  macReg := hardMul.io.out

  hardAdder.io.a := macReg
  hardAdder.io.b := accReg

  when(RegNext(i_acc)) {
    accReg := hardAdder.io.out
  }

  o_res := accReg
}
