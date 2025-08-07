/* WorkerInterfaces.scala -- Worker module interface definition
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
package matmul.worker

import chisel3._
import chisel3.util._

package object interfaces {
  class WorkerInterface(
    DW : Int
  ) extends Bundle {
    // Data
    val data  = UInt(DW.W)
    // Data valid flag
    val valid = Bool()
    // Prog flag (data must go in worker memory)
    val prog  = Bool()
    // Write flag (worker is sending its own accumulator on the bus)
    val write = Bool()
  }
}
