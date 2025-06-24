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

VCD_PATH=${VCD_PATH:-test_run_dir/chisel3.simulator.VCDHackedEphemeralSimulator/workdir-default/trace.vcd}
# Script to automate storing of VCDs (because of the VCD hack)
CLASS=$1
BASECLASS=${CLASS/*./}
[ -z "$CLASS" ] && echo "Usage: $0 <class_name>" 1>&2 && exit 1
[ -z "$BASECLASS" ] && echo "Wrong class name" 1>&2 && exit 1

# Run chisel simulator
sbt "testOnly $CLASS" || exit 1

# Create class-specific directory if it does not exist
DIR=test_run_dir/$BASECLASS
[ -d "$DIR" ] || mkdir -p $DIR

# Move output VCD to specific dir
mv $VCD_PATH $DIR/$BASECLASS.vcd
# Initialize GTKW file
[ -f $DIR/$BASECLASS.gtkw ] || touch $DIR/$BASECLASS.gtkw
