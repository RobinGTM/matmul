package matmul.stage

import circt.stage.ChiselStage

import matmul.utils.Parameters
import matmul.TopLevel

object Main {
  def main(args : Array[String]) : Unit = {
    val param = new Parameters(args)
    val hwDir = param.OUTDIR + java.io.File.separator + "hw"
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
        "-strip-debug-info"
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
