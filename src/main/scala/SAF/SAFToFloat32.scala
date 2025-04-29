package matmul.saf

// Chisel
import chisel3._
import chisel3.util._

// Scala
import math.pow

// Custom SAF to IEEE-754 float32 converter
class SAFToFloat32(
  L   : Int = 5,
  W   : Int = 70,
  B   : Int = 150,
  L2N : Int = 16
) extends RawModule {
  private val SAF_W = W + 8 - L
  // Extended mantissa's leading bit position (invisible leading bit bit of the
  // original mantissa)
  private val HBIT_POS = (W / 24).toInt * 24
  // Center position
  private val CENTER_POS = ((W + 8 - L) / 2).toInt

  /* I/O */
  val i_saf = IO(Input(UInt(SAF_W.W)))
  val o_f32 = IO(Output(UInt(32.W)))

  /* CONVERSION */
  // Reduced exponent
  val reEx = i_saf(SAF_W - 1, SAF_W - (8 - L))
  // Extended, signed mantissa
  val esMa = i_saf(SAF_W - (8 - L) - 1, 0)

  // Sign bit is given by the mantissa's MSB
  val sign = esMa(W - 1)
  // Unsigned mantissa
  val euMa = Wire(UInt(W.W))
  euMa    := Mux(sign,
    1.U + ~ esMa,
    esMa
  )
  // 8-bit exponent
  val expt = Wire(UInt(8.W))
  // 23-bit mantissa
  val mant = Wire(UInt(23.W))

  when(euMa === 0.U) {
    // IEEE-754 zero
    expt := 0.U
    mant := 0.U
  } .otherwise {
    // Find position of the MSB
    val msbPos = (W - 1).U - PriorityEncoder(Reverse(euMa))
    val high   = msbPos - 1.U

    // Normalize (stolen from CuFP: https://github.com/FahimeHajizadeh/Custom-Float-HLS.git)
    // Low index
    val low = Mux(high < 23.U, 0.U, msbPos - 22.U)
    when(low === 0.U) {
      mant := euMa(22, 0)
    } .otherwise {
      mant := (euMa >> high - 22.U)(22, 0)
    }
    expt := (reEx << L) + W.U - ((W - 1).U - msbPos) - 23.U - 1.U
  }
  o_f32 := Cat(sign, expt, mant)
}
