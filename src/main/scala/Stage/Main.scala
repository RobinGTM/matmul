package matmul.stage

import circt.stage.ChiselStage

import matmul.utils.Parameters
import matmul.TopLevel

object Main {
  def main(args : Array[String]) : Unit = {
    val param = new Parameters(args)
    val hwDir = param.OUTDIR + java.io.File.separator + "hw"

    ChiselStage.emitSystemVerilogFile(
      new TopLevel(PARAM = param),
      Array(
        "--target-dir", hwDir,
        "--target", "systemverilog"
      ),
      firtoolOpts = Array(
        "-disable-all-randomization",
        "-strip-debug-info"
      )
    )

    // Create SLR assignment constraints
    val slrCstr = new SLRConstraints(param, "u200")
    slrCstr.create(hwDir)

    // TODO gen host header

    println("Outputs were written to " + param.OUTDIR)
  }
}
