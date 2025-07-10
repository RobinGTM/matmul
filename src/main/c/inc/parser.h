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
