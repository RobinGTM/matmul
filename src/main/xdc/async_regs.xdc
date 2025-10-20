######################################################################
## async_regs.xdc -- matmul constraints file
##
## (C) Copyright 2025 Robin Gay <robin.gay@polymtl.ca>
##
## This file is part of matmul.
##
## matmul is free software: you can redistribute it and/or modify it
## under the terms of the GNU General Public License as published by
## the Free Software Foundation, either version 3 of the License, or
## (at your option) any later version.
##
## matmul is distributed in the hope that it will be useful, but
## WITHOUT ANY WARRANTY; without even the implied warranty of
## MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
## General Public License for more details.
##
## You should have received a copy of the GNU General Public License
## along with matmul. If not, see <https://www.gnu.org/licenses/>.
#######################################
#
# ASYNC_REG properties for clock domain crossing
set_property ASYNC_REG true [get_cells core/sync_rstn_reg]
set_property ASYNC_REG true [get_cells core/sync_rstn_REG_reg]
# (should probably automate this)
# Control MCPs
set_property ASYNC_REG true [get_cells axiW/axiLiteSlave/arMcpSrc/ackSyncFF1_reg]
set_property ASYNC_REG true [get_cells axiW/axiLiteSlave/arMcpSrc/ackSyncFF2_reg]
set_property ASYNC_REG true [get_cells axiW/axiLiteSlave/wrMcpSrc/ackSyncFF1_reg]
set_property ASYNC_REG true [get_cells axiW/axiLiteSlave/wrMcpSrc/ackSyncFF2_reg]
set_property ASYNC_REG true [get_cells axiW/axiLiteSlave/rdMcpDst/loadSyncFF1_reg]
set_property ASYNC_REG true [get_cells axiW/axiLiteSlave/rdMcpDst/loadSyncFF2_reg]
set_property ASYNC_REG true [get_cells core/mcpAdapter/wrMcpDst/loadSyncFF1_reg]
set_property ASYNC_REG true [get_cells core/mcpAdapter/wrMcpDst/loadSyncFF2_reg]
set_property ASYNC_REG true [get_cells core/mcpAdapter/arMcpDst/loadSyncFF1_reg]
set_property ASYNC_REG true [get_cells core/mcpAdapter/arMcpDst/loadSyncFF2_reg]
# FIFO MCPs
# Input FIFO write port (AXI side)
set_property ASYNC_REG true [get_cells iFifoWrPort/rCntMcpDst/loadSyncFF1_reg]
set_property ASYNC_REG true [get_cells iFifoWrPort/rCntMcpDst/loadSyncFF2_reg]
set_property ASYNC_REG true [get_cells iFifoWrPort/rCntSyncReg]
set_property ASYNC_REG true [get_cells iFifoWrPort/wCntMcpSrc/ackSyncFF1_reg]
set_property ASYNC_REG true [get_cells iFifoWrPort/wCntMcpSrc/ackSyncFF2_reg]
# Output FIFO read port (AXI side)
set_property ASYNC_REG true [get_cells oFifoRdPort/wCntMcpDst/loadSyncFF1_reg]
set_property ASYNC_REG true [get_cells oFifoRdPort/wCntMcpDst/loadSyncFF2_reg]
set_property ASYNC_REG true [get_cells oFifoRdPort/wCntSyncReg]
set_property ASYNC_REG true [get_cells oFifoRdPort/rCntMcpSrc/ackSyncFF1_reg]
set_property ASYNC_REG true [get_cells oFifoRdPort/rCntMcpSrc/ackSyncFF2_reg]
# Output FIFO write port (core side)
set_property ASYNC_REG true [get_cells oFifoWrPort/wCntMcpSrc/ackSyncFF1_reg]
set_property ASYNC_REG true [get_cells oFifoWrPort/wCntMcpSrc/ackSyncFF2_reg]
set_property ASYNC_REG true [get_cells oFifoWrPort/rCntSyncReg]
set_property ASYNC_REG true [get_cells oFifoWrPort/rCntMcpDst/loadSyncFF1_reg]
set_property ASYNC_REG true [get_cells oFifoWrPort/rCntMcpDst/loadSyncFF2_reg]
# Output FIFO read port (core side)
set_property ASYNC_REG true [get_cells iFifoRdPort/wCntMcpDst/loadSyncFF1_reg]
set_property ASYNC_REG true [get_cells iFifoRdPort/wCntMcpDst/loadSyncFF2_reg]
set_property ASYNC_REG true [get_cells iFifoRdPort/wCntSyncReg]
set_property ASYNC_REG true [get_cells iFifoRdPort/rCntMcpSrc/ackSyncFF1_reg]
set_property ASYNC_REG true [get_cells iFifoRdPort/rCntMcpSrc/ackSyncFF2_reg]
#######################################
