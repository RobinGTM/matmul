#!/bin/bash

set -e

REPONAME=matmul
TOPDIR=$(git rev-parse --show-toplevel 2>/dev/null)
if [ "$PWD" != "$TOPDIR" ]; then
  echo "This script must be run from the top-level directory of the $REPONAME repo." 1>&2
  if [ -z "$TOPDIR" ]; then
    echo "The current directory doesn't seem to be in a Git repo."
    echo "Please \`cd\` to the top directory of your local copy of $REPONAME."
  else
    echo "Please \`cd $TOPDIR\` and then ./scripts/simulate.sh" 1>&2
  fi
  exit 1
fi

# Defaults are used in the article
DEFAULT_SIZES="10x10 50x50 100x50 100x100 100x200 300x300 400x400"
SIZES=${@:-$DEFAULT_SIZES}
BUILD_DIR=${BUILD_DIR:-benchmark-builds}
VIVADO_LOGS=()

if echo $SIZES | grep -qE '(-h|--help)'; then
  echo "Usage: $0 <height1>x<width1> <height2>x<width2> ..."
  echo
  echo "Generates bitstreams for specified sizes."
  echo "Pass sizes as <height>x<width>. The script can take any number of dimensions"
  echo "(but it might take a lot of time to complete)."
  echo
  echo "If no arguments are passed, generates the bitstreams for sizes:"
  echo "$DEFAULT_SIZES"
  exit
fi

for s in $SIZES; do
  for float in hardfloat saf; do
    HEIGHT=${s%x*}
    WIDTH=${s#*x}
    if ! [[ "$HEIGHT" =~ ^[0-9]+$ ]]; then
      echo "HEIGHT=\"$HEIGHT\" is not a valid integer"
      exit 1
    elif ! [[ "$HEIGHT" =~ ^[0-9]+$ ]]; then
      echo "WIDTH=\"$WIDTH\" is not a valid integer"
      exit 1
    fi
    # Shorten name
    CHISEL_DIR=${s}_${float/hardfloat/hf}
    # hardfloat can run a little faster
    if [ $float == saf ]; then
      DPLL=10
      MPLL=9
    else
      DPLL=9
      MPLL=6
    fi

    # Cleanup and make
    make clean CHISEL_OUTDIR=$CHISEL_DIR BUILDDIR=$BUILD_DIR \
         M_WIDTH=$WIDTH M_HEIGHT=$HEIGHT FLOAT=$float PLL_DIV=$DPLL PLL_MULT=$MPLL
    make all CHISEL_OUTDIR=$CHISEL_DIR BUILDDIR=$BUILD_DIR \
         M_WIDTH=$WIDTH M_HEIGHT=$HEIGHT FLOAT=$float PLL_DIV=$DPLL PLL_MULT=$MPLL

    LOG=/tmp/vivado-$CHISEL_DIR.log
    VIVADO_LOGS+="$LOG "

    mkdir -p $BUILD_DIR/$CHISEL_DIR/reports

    vivado -mode tcl -source <<EOF | tee $LOG /dev/tty\
      | grep -qE '^ERROR: ' && \
      { echo "Vivado error ($LOG)"; kill $(pgrep vivado); exit 1; }
read_verilog $BUILD_DIR/$CHISEL_DIR/hw/TopLevel.sv
read_ip src/main/ip/xdma/xdma_0.xci
read_xdc src/main/xdc/matmul.xdc

# Synthesis
synth_design -part xcu200-fsgd2104-2-e -top TopLevel
opt_design

# Placement
place_design
phys_opt_design

# Routing
route_design
phys_opt_design

report_utilization -hierarchical -file $BUILD_DIR/$CHISEL_DIR/reports/post-impl-util.txt
report_timing_summary -file $BUILD_DIR/$CHISEL_DIR/reports/post-impl-timing.txt

# Bitstream
write_bitstream -force $BUILD_DIR/$CHISEL_DIR/hw/matmul-${HEIGHT}x${WIDTH}.bit
EOF
  done
done

echo "Vivado logs: ${VIVADO_LOGS[@]}"
