/* Main.scala -- Main class that generates the header and emits
 *               SystemVerilog
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
package matmul.stage

import circt.stage.ChiselStage

import matmul.utils.Parameters
import matmul.TopLevel

object Main {
  def main(args : Array[String]) : Unit = {
    val param = new Parameters(args)
    val hwDir = param.OUTDIR + java.io.File.separator + "hw/sv"
    val swDir = param.OUTDIR + java.io.File.separator + "sw"

    ChiselStage.emitSystemVerilogFile(
      new TopLevel(PARAM = param),
      Array(
        "--target-dir", hwDir,
        // "--split-verilog",
        "--target", "systemverilog"
      ) ++ param.CIRCT_ARGS,
      firtoolOpts = Array(
        "-disable-all-randomization",
        "-strip-debug-info",
        "-strip-fir-debug-info"
      ) ++ param.FIRTOOL_ARGS
    )

    // Create SLR assignment constraints
    val slrCstr = new SLRConstraints(param, "u200")
    slrCstr.create(hwDir)

    // Generate header for software part
    val drvDefs = new DriverDefsHeader(param)
    drvDefs.create(swDir)

    println("Outputs were written to " + param.OUTDIR)
  }
}

// import matmul.utils.Parameters
// import matmul.VitisTopLevel

// object VitisMain {
//   def main(args : Array[String]) : Unit = {
//     val param = new Parameters(args)
//     val hwDir = param.OUTDIR + java.io.File.separator + "hw"
//     val swDir = param.OUTDIR + java.io.File.separator + "sw"

//     ChiselStage.emitSystemVerilogFile(
//       new VitisTopLevel(PARAM = param),
//       Array(
//         "--target-dir", hwDir,
//         "--target", "systemverilog"
//       ) ++ param.CIRCT_ARGS,
//       firtoolOpts = Array(
//         "-disable-all-randomization",
//         "-strip-debug-info"
//       ) ++ param.FIRTOOL_ARGS
//     )

//     // Create SLR assignment constraints
//     val slrCstr = new SLRConstraints(param, "u200")
//     slrCstr.create(hwDir)

//     // Generate header for software part
//     val drvDefs = new DriverDefsHeader(param)
//     drvDefs.create(swDir)

//     println("Outputs were written to " + param.OUTDIR)
//   }
// }
