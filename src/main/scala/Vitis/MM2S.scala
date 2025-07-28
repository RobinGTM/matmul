package vitis.axi

import chisel3._
import chisel3.util._

import vitis.utils.AxiReadMasterBundle

/**
 * From https://github.com/Wolf-Tungsten/chisel-vitis-template/
 */
class MM2S(val ADDR_WIDTH: Int, val DATA_WIDTH: Int) extends Module {
  /** Constants */
  val LEN_WIDTH = 32
  val DATA_WIDTH_BYTES = DATA_WIDTH / 8
  val ADDR_ALIGN_BITS = log2Ceil(DATA_WIDTH_BYTES) // Force address alignment to data bit width
  val BURST_LEN = 64

  /** IOs */
  val io = IO(new Bundle {
    val req = Flipped(Decoupled(new Bundle {
      val addr = UInt(ADDR_WIDTH.W)
      val len = UInt(LEN_WIDTH.W)
    }))
    val axiRead = new AxiReadMasterBundle(ADDR_WIDTH, DATA_WIDTH)
    val streamOut = Decoupled(new Bundle {
      val data = UInt(DATA_WIDTH.W)
      val last = Bool()
    })
    val busy = Output(Bool())
  })

  /** Hardware */
  val addrReg: UInt = Reg(UInt(ADDR_WIDTH.W))
  val lenReg: UInt = Reg(UInt(LEN_WIDTH.W))
  val remainLenReg: UInt = Reg(UInt(LEN_WIDTH.W))
  val issuedLenReg: UInt = Reg(UInt(LEN_WIDTH.W))

  val bufferQueue: Queue[UInt] = Module(
    new Queue(UInt(DATA_WIDTH.W), (BURST_LEN * 1.5).toInt)
  )
  private val bufferSpace = (bufferQueue.entries.U - bufferQueue.io.count)
  private val isAlignedTo4KBoundary = ((addrReg & Fill(12, 1.U)) === 0.U)
  private val next4KBoundary = ((addrReg + 4096.U) & (~"hfff".U(ADDR_WIDTH.W)).asUInt)
  private val isAcross4KBoundary = (addrReg + (lenReg << ADDR_ALIGN_BITS)) > next4KBoundary
  io.req.ready        := false.B
  io.axiRead.ar.valid := false.B
  io.axiRead.ar.bits  := 0.U.asTypeOf(io.axiRead.ar.bits)
  io.axiRead.r.ready  := false.B

  bufferQueue.io.enq.valid := false.B
  bufferQueue.io.enq.bits  := io.axiRead.r.bits.data

  val sIdle :: sAddrCompute :: sHeadAddr :: sHeadData :: s4KAlignedAddr :: s4KAlignedData :: sTailAddr :: sTailData :: sFlush :: sEmpty :: nil =
    Enum(10)
  val stateReg: UInt = RegInit(sIdle)
  switch(stateReg) {
    is(sIdle) {
      io.req.ready := true.B
      when(io.req.valid) {
        addrReg  := Cat(io.req.bits.addr >> ADDR_ALIGN_BITS, 0.U(ADDR_ALIGN_BITS.W))
        lenReg   := io.req.bits.len
        stateReg := sAddrCompute
      }
    }

    is(sAddrCompute) {
      remainLenReg := lenReg
      issuedLenReg := 0.U
      when(isAlignedTo4KBoundary) {
        when(isAcross4KBoundary) {
          stateReg := s4KAlignedAddr
        }.otherwise {
          stateReg := sTailAddr
        }
      }.otherwise {
        when(isAcross4KBoundary) {
          stateReg := sHeadAddr
        }.otherwise {
          stateReg := sTailAddr
        }

      }
    }
    is(sHeadAddr) {
      val headLen_wire: UInt = ((next4KBoundary - addrReg) >> ADDR_ALIGN_BITS).asUInt
      when(bufferSpace > headLen_wire) {
        io.axiRead.ar.valid     := true.B
        io.axiRead.ar.bits.addr := addrReg
        io.axiRead.ar.bits.len  := headLen_wire - 1.U
        when(io.axiRead.ar.ready) {
          stateReg     := sHeadData
          remainLenReg := lenReg - headLen_wire
        }
      }
    }
    is(sHeadData) {
      io.axiRead.r.ready         := bufferQueue.io.enq.ready
      bufferQueue.io.enq.valid := io.axiRead.r.valid
      when(
        io.axiRead.r.valid && bufferQueue.io.enq.ready && io.axiRead.r.bits.last
      ) {
        when(remainLenReg >= BURST_LEN.U) {
          stateReg := s4KAlignedAddr
        }.elsewhen(remainLenReg === 0.U) {
          stateReg := sFlush
        }.elsewhen(remainLenReg < BURST_LEN.U) {
          stateReg := sTailAddr
        }
      }
    }
    is(s4KAlignedAddr) {
      when(bufferSpace > BURST_LEN.U) {
        io.axiRead.ar.valid     := true.B
        io.axiRead.ar.bits.addr := addrReg + ((lenReg - remainLenReg) << ADDR_ALIGN_BITS)
        io.axiRead.ar.bits.len  := (BURST_LEN - 1).U
        when(io.axiRead.ar.ready) {
          stateReg     := s4KAlignedData
          remainLenReg := remainLenReg - BURST_LEN.U
        }
      }
    }
    is(s4KAlignedData) {
      io.axiRead.r.ready         := bufferQueue.io.enq.ready
      bufferQueue.io.enq.valid := io.axiRead.r.valid
      when(
        io.axiRead.r.valid && bufferQueue.io.enq.ready && io.axiRead.r.bits.last
      ) {
        when(remainLenReg >= BURST_LEN.U) {
          stateReg := s4KAlignedAddr
        }.elsewhen(remainLenReg === 0.U) {
          stateReg := sFlush
        }.elsewhen(remainLenReg < BURST_LEN.U) {
          stateReg := sTailAddr
        }
      }
    }
    is(sTailAddr) {
      when(bufferSpace > remainLenReg) {
        io.axiRead.ar.valid     := true.B
        io.axiRead.ar.bits.addr := addrReg + ((lenReg - remainLenReg) << ADDR_ALIGN_BITS)
        io.axiRead.ar.bits.len  := remainLenReg - 1.U
        when(io.axiRead.ar.ready) {
          stateReg := sTailData
        }
      }
    }
    is(sTailData) {
      io.axiRead.r.ready         := bufferQueue.io.enq.ready
      bufferQueue.io.enq.valid := io.axiRead.r.valid
      when(
        io.axiRead.r.valid && bufferQueue.io.enq.ready && io.axiRead.r.bits.last
      ) {
        stateReg := sFlush
      }
    }
    is(sFlush) {
      when(issuedLenReg === lenReg) {
        stateReg := sEmpty
      }
    }
    is(sEmpty) {
      bufferQueue.io.deq.ready := true.B
      when(bufferQueue.io.count === 0.U) {
        stateReg := sIdle
      }
    }
  }

  io.streamOut.valid := false.B
  io.streamOut.bits := 0.U.asTypeOf(io.streamOut.bits)
  bufferQueue.io.deq.ready := false.B

  when(stateReg =/= sIdle && stateReg =/= sAddrCompute) {
    io.streamOut.valid := bufferQueue.io.deq.valid
    bufferQueue.io.deq.ready := io.streamOut.ready
    io.streamOut.bits.data := bufferQueue.io.deq.bits
    io.streamOut.bits.last := (issuedLenReg === lenReg - 1.U)
    when(bufferQueue.io.deq.valid) {
      when(io.streamOut.ready) {
        issuedLenReg := issuedLenReg + 1.U
      }
    }
  }

  io.busy := stateReg =/= sIdle
}
