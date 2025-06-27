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
  val ctlReg   = RegInit(VecInit(Seq.fill(PARAM.CTL_W)(false.B)))
  // Counters
  // Worker counter (height)
  val wCntReg  = RegInit(0.U(PARAM.M_HEIGHT.W))
  // Memory counter (width)
  val mCntReg  = RegInit(0.U(PARAM.M_WIDTH.W))

  /* REGISTER R/W */
  // Control register is asynchronous R/W (LUT, probably too small to
  // instantiate BRAMs anyway)
  // R/W logic
  when(ctl_reg.i_we) {
    ctl_reg.o_data := 0.U
    when(ctl_reg.i_addr === 0.U) {
      // Control register writes are non-destructive
      ctlReg := (ctl_reg.i_data | ctlReg.asUInt).asBools
    }
  } .elsewhen(ctl_reg.i_en) {
    when(ctl_reg.i_addr === 0.U) {
      ctl_reg.o_data := ctlReg.asUInt
    } .otherwise {
      ctl_reg.o_data := 0.U
    }
  } .otherwise {
    ctl_reg.o_data := 0.U
  }

  // SAF flag
  ctlReg(PARAM.CTL_SAF) := (!PARAM.USE_HARDFLOAT).B

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
  // When in SAF mode, MatMul controller converts outgoing SAFs to
  // floats
  if(PARAM.USE_HARDFLOAT) {
    m_axis.tdata  := from_matmul.data
  } else {
    val saf2Float = Module(new SAFToFloat32(
      PARAM.SAF_L, PARAM.SAF_W,
      PARAM.SAF_B, PARAM.SAF_L2N
    ))
    saf2Float.i_saf := from_matmul.data
    m_axis.tdata    := saf2Float.o_f32
  }

  // Unused
  to_matmul.ready := false.B
  to_matmul.valid := s_axis.tvalid
  // When in SAF mode, MatMul controller converts incoming floats to
  // SAF
  if(PARAM.USE_HARDFLOAT) {
    to_matmul.data  := s_axis.tdata
  } else {
    val float2Saf = Module(new Float32ToSAF(
      PARAM.SAF_L, PARAM.SAF_W,
      PARAM.SAF_B, PARAM.SAF_L2N
    ))
    float2Saf.i_f32 := s_axis.tdata
    to_matmul.data  := float2Saf.o_saf
  }
  to_matmul.prog  := ctlReg(PARAM.CTL_PROG)
}
