# MatMul

A simple matrix multiplier for AMD / Xilinx Alveo board written in Chisel. This
is a proof-of-concept for a Vitis-less flow that fully exploits the available
resources on Alveo boards.

# Usage

This is a basic Chisel repo. `sbt run` to generate System-Verilog, `sbt
'testOnly <class>'` to run tests (located in `src/test/scala`).

# Work in progress

As of now, MatMul is not programmable.
