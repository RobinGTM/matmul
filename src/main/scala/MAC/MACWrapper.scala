/* MACWrapper.scala -- MAC wrapper that facilitates abstraction of the
 *                     low-level float implementation to higher-level
 *                     modules
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
