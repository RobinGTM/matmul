package matmul

// Chisel
import chisel3._
import chisel3.util._

import matmul._
import matmul.saf._
import matmul.axi._
import matmul.axi.interfaces._
import matmul.fifo._
import matmul.utils.Parameters

class MatMulWrapper(
  PARAM : Parameters
) extends Module {
  /* I/O */
  // Slave AXI interface
  val s_axi = IO(new SlaveAXIInterface(PARAM.AXI_AW, PARAM.AXI_W))

  /* MODULES */
  // Float32 to SAF
  val f2SAF  = Module(new Float32ToSAF(PARAM.SAF_L, PARAM.SAF_W, PARAM.SAF_B, PARAM.SAF_L2N))
  // AXI breakdown
  val axiBk  = Module(new SAXIRW2Full(PARAM.AXI_AW, PARAM.AXI_W))
  val axiIn  = Module(new AXIWriteAdapter(PARAM.AXI_AW, PARAM.AXI_W))
  val matmul = Module(new MatMul(PARAM))
  // SAF to float32
  val SAF2F  = Module(new SAFToFloat32(PARAM.SAF_L, PARAM.SAF_W, PARAM.SAF_B, PARAM.SAF_L2N))
  // Floats32 are used so hardcode 32-bit vectors
  val fifo   = Module(new FIFO(UInt(32.W), PARAM.FIFO_DEPTH))
  val axiOut = Module(new AXIReadAdapter(PARAM.AXI_AW, PARAM.AXI_W))

  /* WIRING */
  // Breakdown AXI interface into read / write
  axiBk.s_axi     <> s_axi
  // AXI bus directly writes in the MatMul kernel
  axiIn.s_axi_wr  <> axiBk.s_axi_wr
  f2SAF.i_f32     := axiIn.out.data(31, 0)
  matmul.in.data  := f2SAF.o_saf
  matmul.in.valid := axiIn.out.valid
  // Convert back to float32
  SAF2F.i_saf     := matmul.out.data
  // MatMul ignores FIFO's nfull, so FIFO must be read before asking too many
  // computations
  fifo.wr.i_we    := matmul.out.valid
  fifo.wr.i_data  := SAF2F.o_f32
  // AXI reads from FIFO
  fifo.rd         <> axiOut.fifo_rd
  // AXI output adapter handles read requests
  axiBk.s_axi_rd  <> axiOut.s_axi_rd
}
