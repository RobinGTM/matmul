package mac

import chisel3._
import chisel3.util._

import hardfloat._
import saf._

class MACWrapper(
  USE_HARDFLOAT : Boolean = false,
  DW            : Int = 33,
  SAF_L         : Int = 5,
  SAF_W         : Int = 70
) extends Module {
  /* I/O */
  val io = IO(new MACInterface(DW))

  /* CONDITIONAL MAC */
  // Allows plugging to a conditional module,
  // https://stackoverflow.com/questions/70390834/conditional-module-instantiation-in-chisel
  val mac : Module {
    def io : MACInterface
  } = if(USE_HARDFLOAT) {
    Module(new HardMAC(DW))
  } else {
    Module(new SAFMAC(DW, SAF_L, SAF_W))
  }

  /* WIRING */
  mac.io <> io
}
