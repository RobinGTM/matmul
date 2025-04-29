package matmul.stage

import circt.stage.ChiselStage

import matmul.utils.Parameters
import matmul.Top

object Main extends App {
  val size = 400
  val param = Parameters(
    M_WIDTH     = size,
    M_HEIGHT    = size,
    WEIGHT_FILE = s"src/main/resources/random${size}-matrix.txt"
  )

  val outDir = s"outputs/MatMul-${param.M_WIDTH}x${param.M_HEIGHT}"
  // val outDir = "/tmp"

  ChiselStage.emitSystemVerilogFile(
    new Top(PARAM = param),
    Array(
      "--target-dir", outDir,
      "--target", "systemverilog"
    ),
    firtoolOpts = Array(
      "-disable-all-randomization",
      "-strip-debug-info"
    )
  )

  // Create SLR assignment constraints
  val slrCstr = new SLRConstraints(param, "u200")
  slrCstr.create(outDir)
}
