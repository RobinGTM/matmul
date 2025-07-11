SHELL           = /bin/bash
M_HEIGHT        = 16
M_WIDTH         = 16
FLOAT           = saf
SBT_MEM         = 65535
PLL_MULT        = 6
PLL_DIV         = 6
BASE_FREQ       = 156.25
CORE_FREQ      := '$(BASE_FREQ) * $(PLL_MULT) / $(PLL_DIV)'
CHISEL_OUTDIR   = matmul-$(M_HEIGHT)x$(M_WIDTH)_$(FLOAT)-$(shell \
                    echo $(CORE_FREQ) | bc \
                  )MHz
CC              = gcc
CSRCDIR         = src/main/c/src
CINCDIR         = src/main/c/inc
BUILDDIR        = build
BUILDDIR_ABS    = $(PWD)/$(BUILDDIR)
OBJDIR         := $(BUILDDIR)/$(CHISEL_OUTDIR)/objs
CFLAGS          = -O2 -g -Wall
CLIBFLAGS       = -lgsl -lgslcblas
# Includes .../host to get hardware.h
CINCFLAGS       = -I$(CINCDIR) -I$(BUILDDIR)/$(CHISEL_OUTDIR)/sw

EXE_NAME        = matmul-host

objs := matmul.o matvec.o benchmark.o parser.o

default: all

help:
	@echo "\`make host\` builds the host code."
	@echo "\`make hw\` builds the Chisel project."
	@echo "Use \`make all\` or \`make\` to build everything."
	@echo
	@echo "Use M_WIDHT and M_HEIGHT to define \
	the machine's size."
	@echo "Set FLOAT to \`hardfloat\` to use Berkeley's hardfloat \
	floating point implementation. Default is \`saf\`."
	@echo "Use BASE_FREQ, PLL_MULT and PLL_DIV to define \
	the core frequency (BASE_FREQ is fixed by hardware, core \
	will run at BASE_FREQ * PLL_MULT / PLL_DIV)"

base:
	mkdir -p $(BUILDDIR_ABS)/$(CHISEL_OUTDIR)/{sw,hw}
	mkdir -p $(OBJDIR)

hw: base
	sbt -mem $(SBT_MEM) \
	  "run -w $(M_WIDTH) -h $(M_HEIGHT) \
	   -xpll $(PLL_MULT) -dpll $(PLL_DIV) \
	   -fbase $(BASE_FREQ) \
	   -o $(BUILDDIR_ABS)/$(CHISEL_OUTDIR) \
	   $(shell [ x"$(FLOAT)" == x"hardfloat" ] && echo '-hf') \
	  "

# hardware.h: SIM_XDMA.sv
# 	ln -sfv $(BUILDDIR_ABS)/$(CHISEL_OUTDIR)/hardware.h $(CINCDIR)/hardware.h

$(objs): base
	$(CC) $(CLIBFLAGS) $(CINCFLAGS) $(CFLAGS) -c -o $(OBJDIR)/$@ $(CSRCDIR)/$(@:%.o=%.c)

host: $(objs) base
	$(CC) $(CLIBFLAGS) $(CINCFLAGS) $(CFLAGS) \
	-o $(BUILDDIR_ABS)/$(CHISEL_OUTDIR)/sw/$(EXE_NAME) \
	$(OBJDIR)/*.o $(CSRCDIR)/main.c

all: hw host
	@echo "[MatMul done!]"
	@echo "Outputs were written to $(BUILDDIR_ABS)/$(CHISEL_OUTDIR)"

.PHONY: clean
clean:
	rm -rf $(BUILDDIR_ABS)/$(CHISEL_OUTDIR)

.PHONY: distclean
distclean: clean
	rm -rf $(BUILDDIR_ABS)
	find . -name '*.o' -delete
#	rm -rf project
#	rm -rf target
