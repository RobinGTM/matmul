/* MatMulController.scala -- Main core controller
 *
 * (C) Copyright 2025 Robin Gay <robin.gay@polymtl.ca>
 *
 * This file is part of matmul.
 *
 * matmul is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * matmul is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with matmul. If not, see <https://www.gnu.org/licenses/>.
 */
package matmul

// Chisel
import chisel3._
import chisel3.util._

// Local
import mcp.interfaces._
import asyncfifo._
import asyncfifo.interfaces._
import axi.interfaces._
import saf._
import saf.utils._
import hardfloat.{recFNFromFN,fNFromRecFN}
import flopoco.{InputIEEE,OutputIEEE}
import matmul.interfaces._
import matmul.utils._

class MatMulController(
  PARAM : Parameters
) extends Module {
  /* I/O */
  // Control register interface (from AXI-Lite slave)
  val ctl_reg = IO(new BasicRegInterface(PARAM.CTL_AW, PARAM.CTL_W))

  // AXI-Stream from FIFO
  // MatMul controller receives IEEE754 float32 data
  val s_axis = IO(new SlaveAXIStreamInterfaceNoTid(32))
  // From MatMul output to FIFO
  val m_axis = IO(new MasterAXIStreamInterfaceNoTid(32))

  // MatMul interfaces
  val from_matmul = IO(Input(new MatMulInterface(PARAM.DW)))
  val to_matmul   = IO(Output(new MatMulInterface(PARAM.DW)))

  /* STATE REGISTERS */
  // Main control reg
  val ctlReg  = RegInit(VecInit(Seq.fill(PARAM.CTL_W)(false.B)))
  // Counters
  // Worker counter (height)
  val wCntReg = RegInit(0.U(log2Up(PARAM.M_HEIGHT).W))
  // Memory counter (width)
  val mCntReg = RegInit(0.U(log2Up(PARAM.M_WIDTH).W))

  /* REGISTER R/W */
  // Control register is asynchronous R/W (LUT, probably too small to
  // instantiate BRAMs anyway)
  // R/W logic
  // Registers 2 and 3 allow reading matrix width and matrix height
  // from hardware
  when(ctl_reg.i_we) {
    ctl_reg.o_data := 0.U
    when(ctl_reg.i_addr === 0.U) {
      // Control register writes are non-destructive
      ctlReg := (ctl_reg.i_data | ctlReg.asUInt).asBools
    }
  } .elsewhen(ctl_reg.i_en) {
    when(ctl_reg.i_addr === (PARAM.CTL_REG / 4).U) {
      ctl_reg.o_data := ctlReg.asUInt
    } .elsewhen(ctl_reg.i_addr === (PARAM.HEIGHT_REG / 4).U) {
      ctl_reg.o_data := PARAM.M_HEIGHT.U
    } .elsewhen(ctl_reg.i_addr === (PARAM.WIDTH_REG / 4).U) {
      ctl_reg.o_data := PARAM.M_WIDTH.U
    } .otherwise {
      ctl_reg.o_data := 0.U
    }
  } .otherwise {
    ctl_reg.o_data := 0.U
  }

  // FLOAT flag
  for(i <- 0 to PARAM.FLOAT_TYPE_MAP.toList.length - 1) {
    ctlReg(PARAM.CTL_FLOAT + i) := PARAM.FLOAT_TYPE_MAP(PARAM.FLOAT).U(2.W)(i)
  }

  /* FSM */
  s_axis.tready := from_matmul.ready
  when(ctlReg(PARAM.CTL_PROG)) {
    when(s_axis.tvalid) {
      mCntReg := mCntReg + 1.U
      when(mCntReg === (PARAM.M_WIDTH - 1).U) {
        mCntReg := 0.U
        wCntReg := wCntReg + 1.U
        when(wCntReg === (PARAM.M_HEIGHT - 1).U) {
          wCntReg                := 0.U
          ctlReg(PARAM.CTL_PROG) := false.B
        }
      }
    }
  }

  /* WIRING */
  // Write flag
  ctlReg(PARAM.CTL_WRITE) := from_matmul.valid
  // Ready flag
  ctlReg(PARAM.CTL_READY) := from_matmul.ready

  // Warning: m_axis tready is ignored (should be always ready because
  // plugged into a big enough FIFO)
  m_axis.tvalid   := from_matmul.valid
  // TLAST is ignored by FIFO
  m_axis.tlast    := false.B
  // Float input / output conversions
  PARAM.FLOAT match {
    case "saf"        =>
      to_matmul.data  := expandF32(s_axis.tdata)
      m_axis.tdata    := restoreF32(from_matmul.data)
    case "hardfloat"  =>
      to_matmul.data  := recFNFromFN(8, 24, s_axis.tdata)
      m_axis.tdata    := fNFromRecFN(8, 24, from_matmul.data)
    case "flopoco"    =>
      val inIeee2Fp    = Module(new InputIEEE(PARAM.DW, 300))
      inIeee2Fp.io.X  := s_axis.tdata
      to_matmul.data  := inIeee2Fp.io.R
      val outFp2Ieee   = Module(new OutputIEEE(PARAM.DW, 300))
      outFp2Ieee.io.X := from_matmul.data
      m_axis.tdata    := outFp2Ieee.io.R
  }
  // Unused
  to_matmul.ready := false.B
  to_matmul.valid := s_axis.tvalid
  // Prog state feedback
  to_matmul.prog  := ctlReg(PARAM.CTL_PROG)
}
