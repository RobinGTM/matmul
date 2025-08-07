# `matmul`

`matmul` is a simple matrix multiplier for AMD / Xilinx Alveo boards
fully written in Chisel. This is a proof-of-concept to demonstrate a
Vitis-less, RTL-only flow written in Chisel that fully exploits the
available resources on the Alveo boards, enables multi-clock designs
and applies advanced RTL design techniques to Chisel.

The `matmul` repo is open-source (GPL3) and shipped with
[this](link_to_be_added) article submission.

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

# Usage
## Building the project
### Dependencies
- openjdk17
- sbt
- C libs: GSL with CBLAS support
- Vivado 2024.1
- Xilinx's [`dma_ip_drivers`](https://github.com/Xilinx/dma_ip_drivers.git)
- gcc
- make

### Building `dma_ip_drivers`
Clone the
[`dma_ip_drivers`](https://github.com/Xilinx/dma_ip_drivers.git)
repository and build the kernel module (refer to the README and pull
requests / issues).

Tested commit: `0b9793ec13cab9c7631910e7f911713b68b272ed`

### Building `matmul`
- Build the Chisel project:
`make [VAR=VAL...] hw`
- Build the host code:
`make [VAR=VAL...] host`
- Build the bitstream
`make [VAR=VAL...] bitstream`

Refer to the [Make variables](#make-variables) section to customize
the build.

### Make variables
Several `make` variables are exposed to allow you to customize the
build:

| VAR             | Default value         | Description                                                                                     |
|-----------------|-----------------------|-------------------------------------------------------------------------------------------------|
| `BUILDDIR`      | `build`               | Top-level build directory                                                                       |
| `CHISEL_OUTDIR` | `CHISEL_OUTDIR`       | Chisel outputs directory (under `$(BUILDDIR)`/)                                                 |
| `M_HEIGHT`      | 16                    | Matrix height (number of PE)                                                                    |
| `M_WIDTH`       | 16                    | Matrix width (PE memory size)                                                                   |
| `FLOAT`         | `saf`                 | Float implementation (`saf` or `hardfloat`)                                                     |
| `PLL_MULT`      | 9                     | Multiplication coefficient for the base frequency (156.25)                                      |
| `PLL_DIV`       | 10                    | Division coefficient for the base frequency (156.25)                                            |
| `OOC`           | 1                     | Enable (1) or disable (0) Vivado out-of-context synthesis                                       |
| `DCP`           | dcp                   | Name of the subdirectory of `$(BUILDDIR)/$(CHISEL_OUTDIR)` that will contain design checkpoints |
| `RPT`           | rpt                   | ... Vivado reports                                                                              |
| `LOG`           | log                   | ... Vivado logs                                                                                 |
| `VIVADO_PART`   | `xcu200-fsgd2104-2-e` | Vivado part (the default is the only one tested and XDMA is configured for it)                  |
| `NRPOC`         | `$(nproc)`            | Number of processors to use                                                                     |
| `SBT_MEM`       | 65535                 | Amount of memory to lend to SBT                                                                 |
| `EXE_NAME`      | `matmul-host`         | Name of the output host executable                                                              |


## Running the project
### Flashing the board
The Alveo board's JTAG USB port must be plugged to one of the host's
USB ports. Also, __Vivado's [cable drivers](
https://docs.amd.com/r/en-US/ug973-vivado-release-notes-install-license/Installing-Cable-Drivers
) must be installed__.

Once the bistream has been built, it can be flashed using the
convenience script `scripts/flash.sh`. Just call it passing the
bitstream path as argument:
```
./scripts/flash.sh <path_to_bitstream>/TopLevel.bit
```
_This script has only been tested on one machine, the bitstream can be
flashed using the Vivado GUI if the script doesn't work._

The host will probably need a **hot restart** to correctly rescan the
PCIe bus. To do so, just type `sudo reboot` in a terminal. Don't
unplug or turn off the board or the host's power supply in order to
keep the Alveo powered and prevent it from losing its configuration.

### Running the host code
Note that since `/dev` belongs to `root`, running the host code
requires root priviledges, or setting up a udev rule to allow the user
to interact with the `/dev/xdma0_*` device files.

Once the bitstream has been flashed, the hardware can be checked
with
```
# ./$(BUILDDIR)/$(CHISEL_OUTDIR)/sw/matmul-host -w
```
This command should ask the matrix's size and the floating point
implementation used to the `matmul` hardware, and print this
information formatted as "`<MH>x<MW>_<FLT>`". For example, the default
configuration yields `16x16_saf`.

The article's benchmark results can be reproduced using this
executable.

### Host executable flags

All toggle flags (with "-" in the default value column) are disabled
by default.

| Flag       | Default <arg> | Description                                             |
|------------|---------------|---------------------------------------------------------|
| `-m <arg>` | 1             | Number of different matrices to generate                |
| `-n <arg>` | 1             | Number of different vectors to generate for each matrix |
| `-s <arg>` | `time(NULL)`  | Random seed                                             |
| `-w`       | -             | Read hardware info in hardware, print it and exit       |
| `-d`       | -             | Dry-run: don't use hardware, just print matrices        |
| `-p`       | -             | Print result vectors while benchmarking                 |
| `-h`       | -             | Print help                                              |

# Work in progress (that will probably never be done)

- The SAF implementation doesn't handle float over/underflow or NaNs
- The SAF implementation doesn't handle rounding or error conditions
- The core processing element pipeline cannot stall: the output FIFO
  must have enough space for outgoing data when sending a vector, or
  the hardware may enter an unpredictible state

# Copyright notice
This project is released under the GNU GPL3 (see
[LICENSE](./LICENSE)).

(C) Copyright 2025 Robin Gay <robin.gay@polymtl.ca>
