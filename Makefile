# Set default shell (for using bash syntax in shells)
SHELL           = /bin/bash
# Main build directory
BUILDDIR        = build
# Absolute version
BUILDDIR_ABS    = $(PWD)/$(BUILDDIR)
# Number of processors
NPROC           = $(shell nproc)

# Top-level module name. This variable should match the name of the
# top-level module generated by Chisel
TOP_NAME       := TopLevel
# Machine parameters
M_HEIGHT        = 16
M_WIDTH         = 16
FLOAT           = saf
PLL_MULT        = 9
PLL_DIV         = 10
# Fixed (hardware clock)
BASE_FREQ      := 156.25
CORE_FREQ      := '$(BASE_FREQ) * $(PLL_MULT) / $(PLL_DIV)'
CHISEL_OUTDIR   = matmul-$(M_HEIGHT)x$(M_WIDTH)_$(FLOAT)-$(shell \
                    echo $(CORE_FREQ) | bc \
                  )MHz
# Chisel output dir (passed to Chisel Main through '-o')
CHISELDIR      := $(BUILDDIR)/$(CHISEL_OUTDIR)
# Location of Chisel's output: this should match what Chisel does too
SYSTEMVERILOG  := $(CHISELDIR)/hw/$(TOP_NAME).sv
# SBT
# Memory (kB)
SBT_MEM         = 65535
# Flags passed to Main
SBT_RUN_FLAGS = -w $(M_WIDTH) -h $(M_HEIGHT) \
-xpll $(PLL_MULT) -dpll $(PLL_DIV) \
-fbase $(BASE_FREQ) \
-o $(BUILDDIR_ABS)/$(CHISEL_OUTDIR) \
$(shell [ x"$(FLOAT)" == x"hardfloat" ] && echo '-hf')
# Additional flags for circt and firtool
ifdef CIRCT_FLAGS
SBT_RUN_FLAGS += -C \"$(CIRCT_FLAGS)\"
endif
ifdef FIRTOOL_FLAGS
SBT_RUN_FLAGS += -F \"$(FIRTOOL_FLAGS)\"
endif
# Vitis kernel compilation
ifdef VITIS
SBT_RUN = runMain matmul.stage.VitisMain
else
SBT_RUN = runMain matmul.stage.Main
endif
# Final command for sbt
SBT_RUN_CMD = "$(SBT_RUN) $(SBT_RUN_FLAGS)"

# C
CC              = gcc
CSRCDIR         = src/main/c/src
CINCDIR         = src/main/c/inc
CFLAGS          = -O2 -g -Wall
CLIBFLAGS       = -lgsl -lgslcblas
# Includes .../host to get hardware.h
CINCFLAGS       = -I$(CINCDIR) -I$(CHISELDIR)/sw
# Objects
OBJDIR         := $(BUILDDIR)/$(CHISEL_OUTDIR)/objs
# Get objects from sources
SRCS           := $(wildcard $(CSRCDIR)/*.c)
OBJS           := $(patsubst $(CSRCDIR)/%.c,$(OBJDIR)/%.o,$(SRCS))
# Executable name
EXE_NAME        = matmul-host
# Executable location
EXE            := $(CHISELDIR)/sw/$(EXE_NAME)

# Vivado
# Out-of-context enabled by default
OOC             = 1
# Create compilation script
ifeq ($(OOC), 1)
TCL_TEMPLATE   := scripts/$(TOP_NAME).tcl.in
else
TCL_TEMPLATE   := scripts/$(TOP_NAME)_noOOC.tcl.in
endif
# Compilation script location
TCL            := $(CHISELDIR)/$(notdir $(TCL_TEMPLATE:%.in=%))
# Vivado checkpoints
DCP_DIR         = $(CHISELDIR)/dcp
# Vivado reports
RPT_DIR         = $(CHISELDIR)/rpt
# Vivado logs
LOG_DIR         = $(CHISELDIR)/log
BITSTREAM      := $(CHISELDIR)/hw/$(TOP_NAME).bit
# Defaults to Alveo U200
VIVADO_PART     = xcu200-fsgd2104-2-e

default: host

help:
	@echo "\`make all\` builds the chisel project, the associated \
	host code, and the bitstream. Be careful: this can take a \
	long time (>1h, even more if a big instance is compiled)."
	@echo "\`make host\` or \`make\` build the chisel project and \
	the host code only."
	@echo "Use M_WIDHT and M_HEIGHT to define \
	the machine's size."
	@echo "Use AXIL_{AW,W}, AXI_{AW,W} to define data widths"
	@echo "Use BASE_FREQ, PLL_MULT and PLL_DIV to define the core \
	frequency (BASE_FREQ (MHz) is fixed by hardware, core will \
	run at BASE_FREQ * PLL_MULT / PLL_DIV) MHz"
	@echo "Set OOC to 0 (or anything other than 1) to disable \
	out-of-context synthesis"

$(SYSTEMVERILOG):
	sbt --batch --color=always --mem $(SBT_MEM) $(SBT_RUN_CMD)
.PHONY: hw
hw: $(SYSTEMVERILOG)

$(OBJDIR)/%.o: $(CSRCDIR)/%.c $(SYSTEMVERILOG)
	@mkdir -p $(OBJDIR)
	$(CC) $(CLIBFLAGS) $(CINCFLAGS) $(CFLAGS) -c -o $@ $<

$(EXE): $(OBJS) $(SYSTEMVERILOG)
	$(CC) $(CLIBFLAGS) $(CINCFLAGS) $(CFLAGS) \
	  -o $@ $(OBJDIR)/*.o
.PHONY: host
host: $(EXE)

VAR_LIST := $(shell sed -rn 's#^[^/]*//([^/]+)//$$#\1#p' $(TCL_TEMPLATE))
$(TCL):
	rm -f $(TCL)
	cp $(TCL_TEMPLATE) $(TCL)
	@$(foreach var,$(VAR_LIST), \
	echo "Replacing $(var) with $($(value var)) in $(TCL)"; \
	sed -ri 's#//$(var)//#$($(value var))#' $(TCL); \
	)
	@echo "$(TCL) generated."
.PHONY: tcl
tcl: $(TCL)

$(BITSTREAM): $(SYSTEMVERILOG) $(TCL)
	rm -rf $(RPT_DIR)
	@mkdir -p $(LOG_DIR)
	vivado -mode batch -source $(TCL) \
	  -log $(LOG_DIR)/$(TOP_NAME).log -journal $(LOG_DIR)/$(TOP_NAME).jou
.PHONY: bitstream
bitstream: $(BITSTREAM)

all: $(SYSTEMVERILOG) $(OBJS) $(EXE) $(BITSTREAM)
	@echo "[MatMul done!]"
	@echo "Outputs were written to $(CHISELDIR)"

.PHONY: clean
clean:
	find . -name '*.log' -delete
	find . -name '*.jou' -delete
	rm -rf $(BUILDDIR_ABS)/$(CHISEL_OUTDIR)

.PHONY: distclean
distclean: clean
	find $(BUILDDIR_ABS) -name 'matmul*' -delete
	find . -name '*.o' -delete
#	rm -rf project
#	rm -rf target
