/* parser.h -- A simple parser's header
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
#ifndef __PARSER_H__
#define __PARSER_H__

#include "matmul.h"
#include "benchmark.h"

typedef enum parser_exit
{
  EXIT_OK,
  EXIT_ERR,
  EXIT_HELP,
  EXIT_HW
} parser_exit_t;

// Print matmul size
void print_hw(matmul_t * matmul);
// Print help string
void print_usage(char * name, matmul_t * matmul);
// Parse command line
parser_exit_t parse_args(int argc, char ** argv, benchinfo_t * bench_info);
//// DEBUG
void print_benchinfo(benchinfo_t * b);

#endif /* __PARSER_H__ */
