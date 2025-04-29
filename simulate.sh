#!/bin/bash

set -e

sbt "testOnly $1"
NAME=$(echo $1 | rev | cut -d '.' -f 1 | rev)
! [ -d test_run_dir/"$NAME" ] && mkdir -p test_run_dir/"$NAME"
cp test_run_dir/chisel3.simulator.VCDHackedEphemeralSimulator/workdir-default/trace.vcd test_run_dir/"$NAME"/"$NAME".vcd
