# `MatMul`

A simple matrix multiplier for AMD / Xilinx Alveo boards fully written in
Chisel. This is a proof-of-concept for a Vitis-less, RTL-only flow written in
Chisel that fully exploits the available resources on the Alveo boards.

# Usage

This is a basic Chisel repo. `sbt run` to generate System-Verilog and the Alveo
constraints file (for u200), `sbt 'testOnly <testclass>'` to run tests (located
in `src/test/scala`). A little simulation script is available for convenience,
it creates a dedicated directory and puts the generated VCD in it. Just run
`./scripts/simulate.sh <testclass>`.

# Details and kudos

`MatMul` can use two different floating point arithmetic packages :
- a self-aligned format for float computations, based on [Tarek Ould-Bachir and
  Jean-Pierre David's article](https://doi.org/10.1145/2457443.2457444) on the
  subject. A big part of the SAF-IEEE754 conversions and SAF computations were
  stolen from [CuFP](https://github.com/FahimeHajizadeh/Custom-Float-HLS.git).
- Berkeley's [`hardfloat`](https://github.com/ucb-bar/berkeley-hardfloat)
  Chisel package which uses an custom internal recoded floating point format.

The VCD generation hack was taken on [edwardcwang's
repo](https://github.com/edwardcwang/decoupled-serializer).

# Work in progress

- `MatMul` doesn't handle float over/underflow or NaNs
