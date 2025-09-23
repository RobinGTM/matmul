# XDMA synthesis
set_property KEEP_HIERARCHY SOFT \
    [get_cells -hier -filter {REF_NAME==xdma_0 || ORIG_REF_NAME==xdma_0} -quiet] -quiet
