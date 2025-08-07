/* benchmark.h -- Benchmarking functions
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
#ifndef __BENCHMARK_H__
#define __BENCHMARK_H__

#include "matmul.h"

// Benchmark information struct
typedef struct benchinfo_struct
{
  char dry_run;
  char print;
  int n_mat;
  int n_vec;
  int seed;
  // Maximum absolute error
  float max_err;
  // Mean absolute error
  float mean_err;
  // Maximum relative error (relative to vector's euclidian norm)
  float max_rel_err;
  // Mean relative error
  float mean_rel_err;
  // Times
  clock_t hw_max_time;
  clock_t hw_tot_time;
  clock_t sw_max_time;
  clock_t sw_tot_time;
} benchinfo_t;

// Benchmark
int do_benchmark(const matmul_t * matmul, benchinfo_t * bench_info);
// Print benchmark results
void print_results(benchinfo_t * bench_info);

#endif /* __BENCHMARK_H__ */
