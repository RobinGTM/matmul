# `matmul`

A simple matrix multiplier for AMD / Xilinx Alveo boards fully written
in Chisel. This is a proof-of-concept to demonstrate a Vitis-less,
RTL-only flow written in Chisel that fully exploits the available
resources on the Alveo boards, enables multi-clock designs and applies
classical RTL design techniques to Chisel.

The `matmul` repo is open-source and shipped with
[this](link_to_be_added) article proposal.

# Usage
## Building the project

### Dependencies
- openjdk17
- sbt
- gcc
- C libs: GSL with CBLAS support
- Vivado 2024.1
- make

### Building
- Build the Chisel project:
`make [VAR=VAL...] hw`
- Build the host code:
`make [VAR=VAL...] host`
- Build the bitstream
`make [VAR=VAL...] bitstream`

### Make variables
Several `make` variables are exposed to allow you to customize the
build:

| VAR             | DEFAULT VALUE         | DESCRIPTION                                                                                     |
|-----------------|-----------------------|-------------------------------------------------------------------------------------------------|
| `BUILDDIR`      | `build`               | Top-level build directory                                                                       |
| `CHISEL_OUTDIR` | `CHISEL_OUTDIR`       | Chisel outputs directory (under `$(BUILDDIR)`/)                                                 |
|-----------------|-----------------------|-------------------------------------------------------------------------------------------------|
| `M_HEIGHT`      | 16                    | Matrix height (number of PE)                                                                    |
| `M_WIDTH`       | 16                    | Matrix width (PE memory size)                                                                   |
| `FLOAT`         | `saf`                 | Float implementation (`saf` or `hardfloat`)                                                     |
| `PLL_MULT`      | 9                     | Multiplication coefficient for the base frequency (156.25)                                      |
| `PLL_DIV`       | 10                    | Division coefficient for the base frequency (156.25)                                            |
|-----------------|-----------------------|-------------------------------------------------------------------------------------------------|
| `OOC`           | 1                     | Enable (1) or disable (0) Vivado out-of-context synthesis                                       |
| `DCP`           | dcp                   | Name of the subdirectory of `$(BUILDDIR)/$(CHISEL_OUTDIR)` that will contain design checkpoints |
| `RPT`           | rpt                   | ... Vivado reports                                                                              |
| `LOG`           | log                   | ... Vivado logs                                                                                 |
| `VIVADO_PART`   | `xcu200-fsgd2104-2-e` | Vivado part (the default is the only one tested and XDMA is configured for it)                  |
|-----------------|-----------------------|-------------------------------------------------------------------------------------------------|
| `NRPOC`         | `$(nproc)`            | Number of processors to use                                                                     |
| `SBT_MEM`       | 65535                 | Amount of memory to lend to SBT                                                                 |
| `EXE_NAME`      | `matmul-host`         | Name of the output host executable                                                              |



# Details and kudos

`MatMul` can use two different floating point arithmetic packages :
- a self-aligned format (SAF) for float computations, based on [Tarek Ould-Bachir and
  Jean-Pierre David's article](https://doi.org/10.1145/2457443.2457444) on the
  subject. A big part of the SAF-IEEE754 conversions and SAF computations were
  stolen from [CuFP](https://github.com/FahimeHajizadeh/Custom-Float-HLS.git).
- Berkeley's [`hardfloat`](https://github.com/ucb-bar/berkeley-hardfloat)
  Chisel package which uses an custom internal recoded floating point format.

The VCD generation hack was taken on [edwardcwang's
repo](https://github.com/edwardcwang/decoupled-serializer).

# Work in progress (that will probably never be done)

- `MatMul` doesn't handle float over/underflow or NaNs
