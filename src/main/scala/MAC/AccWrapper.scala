/* AccWrapper.scala -- SAF or hardfloat accumulator wrapper
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
package mac

import chisel3._
import chisel3.util._

import saf._
import hardfloat._
import flopoco._

import acc._
import acc.interfaces._
import mac.interfaces._

class AccWrapper(
  FLOAT : String = "saf",
  DW    : Int = 33,
  // Pack in map?
  SAF_W : Int = 70,
  SAF_L : Int = 5
) extends Module {
  // Manually maintained...
  val FLTS = List("saf", "hardfloat", "flopoco")
  require(FLTS.find(FLOAT == _) != None,
    s"[MulWrapper] Float implementation \"${FLOAT}\" not supported."
  )

  // Probably better with just IN_W and OUT_W
  private val SAF_WIDTH = SAF_W + 8 - SAF_L
  private val D_WIDTH   = FLOAT match {
    case "saf" => SAF_WIDTH
    case _     => DW
  }

  /* I/O */
  val io = IO(new GenericAccInterface(D_WIDTH))

  /* ACC */
  val acc : Module {
    def io : GenericAccInterface
    def DELAY_TICKS : Int
  } = FLOAT match {
    case "saf"       =>
      Module(new SAFAcc(SAF_L, SAF_W))
    case "hardfloat" =>
      Module(new HardAcc(DW, 8, 24))
    case "flopoco"   =>
      Module(new FlopocoAcc(DW))
  }

  /* WIRING */
  acc.io <> io

  // Expose delay ticks to upper levels
  val DELAY_TICKS = acc.DELAY_TICKS
}
