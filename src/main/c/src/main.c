/* main.c -- Host code main file
 *
 * (C) Copyright 2025 Robin Gay <robin.gay@polymtl.ca>
 *
 * This file is part of matmul.
 * 
 * matmul is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * matmul is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with matmul. If not, see <https://www.gnu.org/licenses/>.
 */
#include <stdio.h>
#include <time.h>
#include <gsl/gsl_vector.h>
#include <gsl/gsl_matrix.h>

#include "parser.h"
#include "benchmark.h"
#include "matvec.h"
#include "matmul.h"
#include "hardware.h"

#define SOCK_H2C "/dev/xdma0_h2c_0"
#define SOCK_C2H "/dev/xdma0_c2h_0"
#define MM_CTL   "/dev/xdma0_user"

int main(int argc, char ** argv)
{
  // Attach hardware
  matmul_t matmul;
  int ret;
  ret = attach_matmul(&matmul);
  if (ret < 0)
  {
    return -ret;
  }

  benchinfo_t bench_info;
  parser_exit_t parser_ret = parse_args(argc, argv, &bench_info);
  if (parser_ret == EXIT_HELP || argc == 0)
  {
    print_usage(argv[0], &matmul);
    return 0;
  }
  else if (parser_ret == EXIT_HW)
  {
    print_hw(&matmul);
    return 0;
  }
  else if (parser_ret != 0)
  {
    dprintf(2, "Parser error.\n");
    return parser_ret;
  }

  // Do benchmark
  int bench_ret = do_benchmark(&matmul, &bench_info);
  if (bench_ret != 0)
  {
    dprintf(2, "Something went wrong...\n");
  }
  else if (!bench_info.dry_run)
  {
    print_results(&bench_info);
  }

  // Cleanup
  ret = detach_matmul(&matmul);
  if (ret < 0)
  {
    return -ret;
  }

  return 0;
  // return bench_ret;
}
