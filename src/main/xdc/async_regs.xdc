#######################################
# ASYNC_REG properties for clock domain crossing
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
