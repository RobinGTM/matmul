package vitis.axi

import chisel3._
import chisel3.util._

import vitis.utils.AxiWriteMasterBundle

/**
 * From https://github.com/Wolf-Tungsten/chisel-vitis-template/
 */
class S2MM(val ADDR_WIDTH: Int, val DATA_WIDTH: Int) extends Module {
  /** Constants */
  val LEN_WIDTH = 32
  val DATA_WIDTH_BYTES: Int = DATA_WIDTH / 8
  val ADDR_ALIGN_BITS:  Int = log2Ceil(DATA_WIDTH_BYTES) // Force address alignment to data bit width
  val BURST_LEN = 64

  /** IOs */
  val io = IO(new Bundle {
    val req = Flipped(Decoupled(new Bundle {
      val addr = UInt(ADDR_WIDTH.W)
    }))
    val axiWrite = new AxiWriteMasterBundle(ADDR_WIDTH, DATA_WIDTH)
    val streamIn = Flipped(Decoupled(new Bundle {
      val data = UInt(DATA_WIDTH.W)
      val last = Bool()
    }))
    val busy = Output(Bool())
  })

  /** Hardware */
  val addrReg: UInt = Reg(UInt(ADDR_WIDTH.W))
  val burstLenReg: UInt = Reg(UInt(LEN_WIDTH.W))
  val issuedLenReg: UInt = Reg(UInt(LEN_WIDTH.W))
  val eotReg: Bool = RegInit(true.B)
  val lastBurst: Bool = (burstLenReg <= 1.U)

  val bufferQueue: Queue[UInt] = Module(
    new Queue(UInt(DATA_WIDTH.W), (BURST_LEN * 1.5).toInt)
  )

  val isAlignedTo4KBoundary: Bool = ((addrReg & Fill(12, 1.U)) === 0.U)
  val next4KBoundary: UInt = ((addrReg + 4096.U) & (~"hfff".U(ADDR_WIDTH.W)).asUInt)
  val headLen: UInt = (next4KBoundary - addrReg) >> ADDR_ALIGN_BITS

  val sIdle :: sAddrCompute :: sHeadWaitBuffer :: sHeadAddr :: sHeadData :: sHeadResp :: sWaitBuffer :: sAddr :: sData :: sResp :: Nil =
    Enum(10)
  val stateReg: UInt = RegInit(sIdle)
  io.busy := stateReg =/= sIdle

  // Initial state of each interface
  io.req.ready := false.B

  io.axiWrite.aw.valid     := false.B
  io.axiWrite.aw.bits.addr := addrReg + (issuedLenReg << ADDR_ALIGN_BITS)
  io.axiWrite.aw.bits.len  := 0.U

  io.axiWrite.w.valid     := false.B
  io.axiWrite.w.bits.data := bufferQueue.io.deq.bits
  io.axiWrite.w.bits.last := lastBurst
  io.axiWrite.w.bits.strb := Fill(io.axiWrite.w.bits.strb.getWidth, 1.U)

  io.axiWrite.b.ready := false.B

  bufferQueue.io.enq.bits := io.streamIn.bits.data
  bufferQueue.io.deq.ready := false.B

  // enqueue flow control
  val freezeBuffer_wire: Bool =
    (eotReg || stateReg === sIdle || stateReg === sAddrCompute || stateReg === sHeadAddr || stateReg === sAddr)
  bufferQueue.io.enq.valid := !freezeBuffer_wire && io.streamIn.valid
  io.streamIn.ready := !freezeBuffer_wire && bufferQueue.io.enq.ready
  when(io.streamIn.fire && io.streamIn.bits.last) {
    // eot is set when last is valid in streamIn, no more inputs until the next idle.
    eotReg := true.B
  }

  switch(stateReg) {
    is(sIdle) {
      io.req.ready := true.B
      when(io.req.valid){
        eotReg := false.B
        issuedLenReg := 0.U
        addrReg := Cat(io.req.bits.addr >> ADDR_ALIGN_BITS, 0.U(ADDR_ALIGN_BITS.W))
        stateReg := sAddrCompute
      }
    }

    is(sAddrCompute){
      when(isAlignedTo4KBoundary){
        // Directly processed in 4K
        stateReg := sWaitBuffer
      }.otherwise{
        // Make it up to 4K first
        stateReg := sHeadWaitBuffer
      }
    }

    is(sHeadWaitBuffer){
      when(eotReg || bufferQueue.io.count >= headLen){
        stateReg := sHeadAddr
      }
    }

    is(sHeadAddr){
      io.axiWrite.aw.valid := true.B
      val burstLen_wire = Mux(bufferQueue.io.count >= headLen, headLen, bufferQueue.io.count)
      io.axiWrite.aw.bits.len := burstLen_wire - 1.U
      burstLenReg := burstLen_wire
      when(io.axiWrite.aw.ready){
        stateReg := sHeadData
        issuedLenReg := issuedLenReg + burstLen_wire
      }
    }

    is(sHeadData){
      io.axiWrite.w.valid := bufferQueue.io.deq.valid
      bufferQueue.io.deq.ready := io.axiWrite.w.ready
      when(bufferQueue.io.deq.fire){
        burstLenReg := burstLenReg - 1.U
        when(lastBurst){
          stateReg := sHeadResp
        }
      }
    }

    is(sHeadResp){
      io.axiWrite.b.ready := true.B
      when(io.axiWrite.b.valid){
        stateReg := sWaitBuffer
      }
    }

    is(sWaitBuffer){
      when(bufferQueue.io.count >= BURST_LEN.U){
        stateReg := sAddr
      }.elsewhen(eotReg){
        when(bufferQueue.io.count > 0.U){
          stateReg := sAddr
        }.otherwise {
          stateReg := sIdle
        }
      }
    }

    is(sAddr){
      io.axiWrite.aw.valid := true.B
      val burstLen_wire = Mux(bufferQueue.io.count >= BURST_LEN.U, BURST_LEN.U, bufferQueue.io.count)
      io.axiWrite.aw.bits.len := burstLen_wire - 1.U
      burstLenReg := burstLen_wire
      when(io.axiWrite.aw.ready){
        stateReg := sData
        issuedLenReg := issuedLenReg + burstLen_wire
      }
    }

    is(sData){
      io.axiWrite.w.valid := bufferQueue.io.deq.valid
      bufferQueue.io.deq.ready := io.axiWrite.w.ready
      when(bufferQueue.io.deq.fire){
        burstLenReg := burstLenReg - 1.U
        when(lastBurst){
          stateReg := sResp
        }
      }
    }

    is(sResp){
      io.axiWrite.b.ready := true.B
      when(io.axiWrite.b.valid){
        stateReg := sWaitBuffer
      }
    }
  }

}
