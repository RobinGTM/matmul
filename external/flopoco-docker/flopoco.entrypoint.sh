#!/bin/bash
CMDS=("$@")

# Batch execute commands passed as space-separated strings
for cmd in "${CMDS[@]}"; do
  /flopoco/build/flopoco $cmd
done
