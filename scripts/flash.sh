#!/bin/bash
BITFILE=$1

if [ -z "$BITFILE" ]; then
  echo 1>&2 "No bitfile given! Usage: $0 <bitfile>."
  exit 1
elif ! [ -f "$BITFILE" ]; then
  echo 1>&2 "$BITFILE: No such file or directory."
  exit 127
fi

echo "Flashing $BITFILE..."

TMPFILE=$(mktemp)
set -o pipefail
vivado -mode tcl -tclargs $BITFILE -source <<"EOF" |& tee $TMPFILE
set bitfile [lindex $argv 0]
# HW manager
open_hw_manager
# Connect to the hw server
connect_hw_server -url localhost:3121
# Set target (PYNQ-Z2)
current_hw_target [get_hw_targets */Xilinx/21290719R03NA]
# Open HW target
open_hw_target
# Set hw device to Alveo
current_hw_device [get_hw_devices xcu200_0]
# Set bitfile
set_property PROGRAM.FILE $bitfile [current_hw_device]
# Program
program_hw_devices
EOF
EXIT=$?
set +o pipefail

if ! [ $EXIT -eq 0 ] || ERR=$(cat $TMPFILE | grep ERROR); then
  VIVADO_STRIPPED=$(cat $TMPFILE | sed '/^ *$/d;/^ *\*\+/d;s/^/> /')
  TEXT="[See $TMPFILE for full output]\n\
$(echo "$VIVADO_STRIPPED" | head -n5)\n\
[ellipsis]\n\
"
  notify-send "Flash failed ($EXIT)!" "$TEXT" || echo Failed!
else
  notify-send "Flash done!" "$BITFILE flashed successfully"
  rm $TMPFILE
fi
