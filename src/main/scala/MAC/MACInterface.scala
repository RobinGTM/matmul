/* MACInterface.scala -- Common MAC interface definition
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

package object interfaces {
  case class MACInterface(
    IN_W  : Int = 33,
    OUT_W : Int = 33
  ) extends Bundle {
    val i_a   = Input(UInt(IN_W.W))
    val i_b   = Input(UInt(IN_W.W))
    val i_acc = Input(Bool())
    val i_rst = Input(Bool())
    val o_res = Output(UInt(OUT_W.W))
  }

  case class GenericAdderInterface(
    IN_W : Int = 33,
    OUT_W : Int = 33
  ) extends Bundle {
    val i_a   = Input(UInt(IN_W.W))
    val i_b   = Input(UInt(IN_W.W))
    val o_res = Output(UInt(IN_W.W))
  }

  case class GenericMulInterface(
    IN_W  : Int = 33,
    OUT_W : Int = 73
  ) extends Bundle {
    val i_a   = Input(UInt(IN_W.W))
    val i_b   = Input(UInt(IN_W.W))
    val o_res = Output(UInt(OUT_W.W))
  }
}
