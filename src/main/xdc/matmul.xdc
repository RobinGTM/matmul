######################################################################
## matmul.xdc -- matmul constraints file
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
######################################################################
#
########
# Clocks
create_clock -period 10.000 -name sys_clk [get_ports sys_clk_p]
create_clock -period 6.400 -name coreclk_ref [get_ports coreclk_ref_p]
# Core clock
create_generated_clock -name coreclk \
    -source [get_pins coreClkPll/CLKIN] [get_pins coreClkPll/CLKOUT0]
# Declare asynchronous clock groups
set_clock_groups -asynchronous \
    -group [get_clocks coreclk] \
    -group [get_clocks _xdma_axi_aclk]
set_clock_groups -asynchronous \
    -group [get_clocks _xdma_axi_aclk] \
    -group [get_clocks coreclk]
#
#######################################
# False paths for clock domain crossing
# (should probably automate this)
set_property ASYNC_REG true [get_cells axiW/axiLiteSlave/arMcpSrc/ackSyncFF1_reg]
set_property ASYNC_REG true [get_cells axiW/axiLiteSlave/arMcpSrc/ackSyncFF2_reg]
set_property ASYNC_REG true [get_cells axiW/axiLiteSlave/wrMcpSrc/ackSyncFF1_reg]
set_property ASYNC_REG true [get_cells axiW/axiLiteSlave/wrMcpSrc/ackSyncFF2_reg]
set_property ASYNC_REG true [get_cells axiW/iFifoWrPort/rCntMcpDst/loadSyncFF1_reg]
set_property ASYNC_REG true [get_cells axiW/iFifoWrPort/rCntMcpDst/loadSyncFF2_reg]
set_property ASYNC_REG true [get_cells axiW/iFifoWrPort/wCntMcpSrc/ackSyncFF1_reg]
set_property ASYNC_REG true [get_cells axiW/iFifoWrPort/wCntMcpSrc/ackSyncFF2_reg]
set_property ASYNC_REG true [get_cells axiW/oFifoRdPort/wCntMcpDst/loadSyncFF1_reg]
set_property ASYNC_REG true [get_cells axiW/oFifoRdPort/wCntMcpDst/loadSyncFF2_reg]
set_property ASYNC_REG true [get_cells axiW/oFifoRdPort/rCntMcpSrc/ackSyncFF1_reg]
set_property ASYNC_REG true [get_cells axiW/oFifoRdPort/rCntMcpSrc/ackSyncFF2_reg]
set_property ASYNC_REG true [get_cells axiW/axiLiteSlave/rdMcpDst/loadSyncFF1_reg]
set_property ASYNC_REG true [get_cells axiW/axiLiteSlave/rdMcpDst/loadSyncFF2_reg]
set_property ASYNC_REG true [get_cells core/oFifoWrPort/rCntMcpDst/loadSyncFF1_reg]
set_property ASYNC_REG true [get_cells core/oFifoWrPort/rCntMcpDst/loadSyncFF2_reg]
set_property ASYNC_REG true [get_cells core/iFifoRdPort/rCntMcpSrc/ackSyncFF1_reg]
set_property ASYNC_REG true [get_cells core/iFifoRdPort/rCntMcpSrc/ackSyncFF2_reg]
set_property ASYNC_REG true [get_cells core/mcpAdapter/wrMcpDst/loadSyncFF1_reg]
set_property ASYNC_REG true [get_cells core/mcpAdapter/wrMcpDst/loadSyncFF2_reg]
set_property ASYNC_REG true [get_cells core/oFifoWrPort/wCntMcpSrc/ackSyncFF1_reg]
set_property ASYNC_REG true [get_cells core/oFifoWrPort/wCntMcpSrc/ackSyncFF2_reg]
set_property ASYNC_REG true [get_cells core/iFifoRdPort/wCntMcpDst/loadSyncFF1_reg]
set_property ASYNC_REG true [get_cells core/iFifoRdPort/wCntMcpDst/loadSyncFF2_reg]
set_property ASYNC_REG true [get_cells core/mcpAdapter/arMcpDst/loadSyncFF1_reg]
set_property ASYNC_REG true [get_cells core/mcpAdapter/arMcpDst/loadSyncFF2_reg]
#
#####################
# Reset configuration
set_false_path -from [get_ports sys_rst_n]
set_property CLOCK_DEDICATED_ROUTE FALSE [get_nets sysRstIBuf/O]
set_property PULLTYPE PULLUP [get_ports sys_rst_n]
set_property IOSTANDARD POD12 [get_ports sys_rst_n]
#
set_property PACKAGE_PIN BD21 [get_ports sys_rst_n]
# Voltage
set_property CONFIG_VOLTAGE 1.8 [current_design]
#
######################################################
# General-purpose 156.25MHz clock and PCIe clock ports
set_property PACKAGE_PIN AM10 [get_ports sys_clk_n]
set_property PACKAGE_PIN AM11 [get_ports sys_clk_p]
set_property -dict {IOSTANDARD LVDS PACKAGE_PIN AV19} [get_ports coreclk_ref_n]
set_property -dict {IOSTANDARD LVDS PACKAGE_PIN AU19} [get_ports coreclk_ref_p]
