# `MatMul`

A simple matrix multiplier for AMD / Xilinx Alveo board written in Chisel. This
is a proof-of-concept for a Vitis-less, RTL-only flow written in Chisel that
fully exploits the available resources on the Alveo boards.

# Usage

This is a basic Chisel repo. `sbt run` to generate System-Verilog and the Alveo
constraints file (for u200), `sbt 'testOnly <testclass>'` to run tests (located
in `src/test/scala`). A little simulation script is available for convenience,
it creates a dedicated directory and puts the generated VCD in it. Just run
`./simulate.sh <testclass>`.

# Details and kudos

`MatMul` uses a self-aligned format for float computations, based on [Tarek
Ould-Bachir and Jean-Pierre David's
article](https://doi.org/10.1145/2457443.2457444) on the subject.

Also, a big part of the SAF-IEEE754 conversions and SAF computations were
stolen from [CuFP](https://github.com/FahimeHajizadeh/Custom-Float-HLS.git).

The VCD generation hack was taken on [edwardcwang's
repo](https://github.com/edwardcwang/decoupled-serializer).

# Work in progress

- As of now, `MatMul` is not programmable. The matrix coefficient are
  implemented as ROMs that are initialized at compile time
- `MatMul` doesn't handle float over/underflow or NaNs
