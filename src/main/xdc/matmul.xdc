
##-----------------------------------------------------------------------------
##
## (c) Copyright 2020-2025 Advanced Micro Devices, Inc. All rights reserved.
##
## This file contains confidential and proprietary information
## of AMD and is protected under U.S. and
## international copyright and other intellectual property
## laws.
##
## DISCLAIMER
## This disclaimer is not a license and does not grant any
## rights to the materials distributed herewith. Except as
## otherwise provided in a valid license issued to you by
## AMD, and to the maximum extent permitted by applicable
## law: (1) THESE MATERIALS ARE MADE AVAILABLE "AS IS" AND
## WITH ALL FAULTS, AND AMD HEREBY DISCLAIMS ALL WARRANTIES
## AND CONDITIONS, EXPRESS, IMPLIED, OR STATUTORY, INCLUDING
## BUT NOT LIMITED TO WARRANTIES OF MERCHANTABILITY, NON-
## INFRINGEMENT, OR FITNESS FOR ANY PARTICULAR PURPOSE; and
## (2) AMD shall not be liable (whether in contract or tort,
## including negligence, or under any other theory of
## related to, arising under or in connection with these
## materials, including for any direct, or any indirect,
## special, incidental, or consequential loss or damage
## (including loss of data, profits, goodwill, or any type of
## loss or damage suffered as a result of any action brought
## by a third party) even if such damage or loss was
## reasonably foreseeable or AMD had been advised of the
## possibility of the same.
##
## CRITICAL APPLICATIONS
## AMD products are not designed or intended to be fail-
## safe, or for use in any application requiring fail-safe
## performance, such as life-support or safety devices or
## systems, Class III medical devices, nuclear facilities,
## applications related to the deployment of airbags, or any
## other applications that could lead to death, personal
## injury, or severe property or environmental damage
## (individually and collectively, "Critical
## Applications"). Customer assumes the sole risk and
## liability of any use of AMD products in Critical
## Applications, subject only to applicable laws and
## regulations governing limitations on product liability.
##
## THIS COPYRIGHT NOTICE AND DISCLAIMER MUST BE RETAINED AS
## PART OF THIS FILE AT ALL TIMES.
##
##-----------------------------------------------------------------------------
##
## Project    : The Xilinx PCI Express DMA
## File       : xilinx_pcie_xdma_ref_board.xdc
## Version    : 4.1
##-----------------------------------------------------------------------------
#
# User Configuration
# Link Width   - x1
# Link Speed   - Gen3
# Family       - virtexuplus
# Part         - xcu200
# Package      - fsgd2104
# Speed grade  - -2
#
# PCIe Block INT - 8
# PCIe Block STR - X1Y2
#
# Xilinx Reference Board is AU200
###############################################################################
# User Time Names / User Time Groups / Time Specs
###############################################################################
##
## Free Running Clock is Required for IBERT/DRP operations.
##
#############################################################################################################
create_clock -period 10.000 -name sys_clk [get_ports sys_clk_p]
create_clock -period 6.400 -name coreclk_ref [get_ports coreclk_ref_p]
# Core clock
create_generated_clock -name coreclk \
    -source [get_pins coreClkPll/CLKIN] [get_pins coreClkPll/CLKOUT0]
#create_generated_clock -name sim_clk #    -source [get_pins simClkIBufDS/O] #    [get_pins simClkBufG/O]
# Declare asynchronous clock groups
set_clock_groups -asynchronous \
    -group [get_clocks coreclk] \
    -group [get_clocks _xdma_axi_aclk]
set_clock_groups -asynchronous \
    -group [get_clocks _xdma_axi_aclk] \
    -group [get_clocks coreclk]
#set_clock_groups -asynchronous #    -group [get_clocks sim_clk] #    -group [get_clocks _xdma_axi_aclk]
#
#############################################################################################################
# False paths for clock domain crossing
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
#############################################################################################################
set_false_path -from [get_ports sys_rst_n]
set_property CLOCK_DEDICATED_ROUTE FALSE [get_nets sysRstIBuf/O]
set_property PULLTYPE PULLUP [get_ports sys_rst_n]
set_property IOSTANDARD POD12 [get_ports sys_rst_n]
#
set_property PACKAGE_PIN BD21 [get_ports sys_rst_n]
#
set_property CONFIG_VOLTAGE 1.8 [current_design]
#
#############################################################################################################
set_property PACKAGE_PIN AM10 [get_ports sys_clk_n]
set_property PACKAGE_PIN AM11 [get_ports sys_clk_p]
set_property -dict {IOSTANDARD LVDS PACKAGE_PIN AV19} [get_ports coreclk_ref_n]
set_property -dict {IOSTANDARD LVDS PACKAGE_PIN AU19} [get_ports coreclk_ref_p]
#
#############################################################################################################
