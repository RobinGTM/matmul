/* SLRConstrants.scala -- XDC constraint generator for SLR placement
 *                        (unused)
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

import collection.mutable.Queue
import scala.util.Using
import java.io.PrintWriter

import matmul.utils.Parameters

// package object SLRConstraints {
//   def createU200(param : Parameters, dir : String) : Unit = {
//     val constraintBuilder = new SLRConstraints(param)
//     constraintBuilder.createU200(dir)
//   }
// }

class SLRConstraints(param : Parameters, model : String = "u200") {
  val lowerModel = model.toLowerCase
  val nSlr = if(lowerModel == "u200") {
    3
  } else if(lowerModel == "u280") {
    4
  } else {
    -1
  }
  require(nSlr > 0, s"nSlr not defined, likely undefined model (${model})")

  val comSlr = if(lowerModel == "u200") {
    1
  } else if(lowerModel == "u280") {
    0
  } else {
    -1
  }
  require(comSlr >= 0, s"comSlr not defined, likely undefined model (${model})")

  val nBlk = param.M_HEIGHT

  val slrCstrPrefix = "set_property USER_SLR_ASSIGNMENT "

  var currSlr = comSlr
  var slrDirection = 1

  def nextSlr() : Unit = {
    if(currSlr + slrDirection == nSlr || currSlr + slrDirection == -1) {
      slrDirection = - slrDirection
    }
    currSlr = currSlr + slrDirection
  }

  def create(dir : String) : Unit = {
    val outFile = dir + s"/slr_assignments_${lowerModel}.xdc"

    // Minimum blocks per SLR
    val minBlkPerSlr = (nBlk / nSlr).toInt
    // Remaning blocks
    val nLeftOffBlks = nBlk % nSlr

    // SLR1 blocks
    val blksPerSlr = new Array[Int](nSlr)
    var assigned = 0
    for(slr <- 0 to blksPerSlr.length - 1) {
      if(slr == comSlr) {
        blksPerSlr(slr) = minBlkPerSlr
      } else if(slr == blksPerSlr.length - 1) {
        blksPerSlr(slr) = nBlk - assigned
      } else {
        blksPerSlr(slr) = minBlkPerSlr + nLeftOffBlks / (nSlr - 1)
      }
      assigned = assigned + blksPerSlr(slr)
    }

    // Initialize SLR assignments
    val slrAssignments = new Array[List[Int]](nSlr)
    for(slr <- 0 to nSlr - 1) {
      // println(blksPerSlr(slr))
      slrAssignments(slr) = Nil
    }

    for(blk <- 0 to nBlk - 1) {
      slrAssignments(currSlr) = slrAssignments(currSlr) :+ blk
      if(((currSlr != nSlr - 1) && (currSlr != 0) &&
        slrAssignments(currSlr).length == blksPerSlr(currSlr) / 2) ||
        slrAssignments(currSlr).length == blksPerSlr(currSlr)
      ) {
        nextSlr()
      }
    }

    var outString = ""
    outString = outString + "## Communication layer\n"
    // XDMA
    outString = outString + slrCstrPrefix + s"SLR${comSlr} [get_cells xdma/*]\n"
    outString = outString + "## MatMul\n"

    for(slr <- 0 to slrAssignments.length - 1) {
      if(slrAssignments(slr).length > 0) {
        outString = outString + s"# SLR${slr}\n"
        for(blk <- 0 to slrAssignments(slr).length - 1) {
          var currBlk = slrAssignments(slr)(blk)
          var blkStr = s"core/core/matmulCore/workers_${currBlk}/*"
          outString = outString +
          slrCstrPrefix + s"SLR${slr}" + " " + s"[get_cells ${blkStr}]\n"
        }
      }
    }

    // Write outfile
    Using(new PrintWriter(outFile)) { writer =>
      writer.write(outString)
    }
  }
}
