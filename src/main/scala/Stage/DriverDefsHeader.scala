/* DriverDefsHeader.scala -- Header generator (used fallbacks in host
 *                           code)
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
    s"#define FLOAT \"${param.FLOAT_TYPE_MAP(param.FLOAT)}\"\n",
    s"#define FLOAT_BITMASK ${param.FLOAT_BITMASK}\n",
    "\n",
    s"#define CTL_REG    ${param.CTL_REG}\n",
    s"#define FLT_BIT    ${param.CTL_FLOAT}\n",
    s"#define HEIGHT_REG ${param.HEIGHT_REG / 4}\n",
    s"#define WIDTH_REG  ${param.WIDTH_REG / 4}\n",
    "\n",
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
