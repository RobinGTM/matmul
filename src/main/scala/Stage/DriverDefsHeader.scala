package matmul.stage

import scala.util.Using
import java.io.PrintWriter
import java.nio.file.Files
import java.io.File
import scala.reflect.io.Directory

import matmul.utils.Parameters

class DriverDefsHeader(
  param : Parameters
) {
  val headerContents : Array[String] = Array(
    "#ifndef __HARDWARE_H__\n",
    "#define __HARDWARE_H__\n",
    "\n",
    s"#define M_HEIGHT ${param.M_HEIGHT}\n",
    s"#define M_WIDTH  ${param.M_WIDTH}\n",
    "\n",
    s"${
      if(param.USE_HARDFLOAT) "#define HARDFLOAT\n"
      else "#ifdef HARDFLOAT\n#undef HARDFLOAT\n#endif /* HARDFLOAT */\n"
    }",
    "\n",
    s"#define CTL_REG  0\n",
    s"#define CMD_PROG (1 << ${param.CTL_PROG})\n",
    s"#define RDY_FLAG (1 << ${param.CTL_READY})\n",
    "\n",
    "#endif /* __HARDWARE_H__ */\n"
  )

  def create(dir : String) : Unit = {
    // Create a directory for host include
    Files.createDirectories(java.nio.file.Paths.get(dir))
      .toAbsolutePath.toString
    val outFile = dir + File.separator + "hardware.h"
    Using(new PrintWriter(outFile)) { writer =>
      for(l <- headerContents) {
        writer.append(l)
      }
    }
  }
}
