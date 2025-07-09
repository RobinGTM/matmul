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
  benchinfo_t bench_info;
  parser_exit_t parser_ret = parse_args(argc, argv, &bench_info);
  if (parser_ret == EXIT_HELP || argc == 0)
  {
    print_usage(argv[0]);
    return 0;
  }
  else if (parser_ret == EXIT_HW)
  {
    print_hw();
    return 0;
  }
  else if (parser_ret != 0)
  {
    dprintf(2, "Parser error.\n");
    return parser_ret;
  }

  // print_benchinfo(&bench_info);
  // return 0;

  // Attach hardware
  matmul_t matmul;
  int ret;
  ret = attach_matmul(&matmul);
  if (ret < 0)
  {
    return -ret;
  }
  else
  {
    dprintf(2, "CTL: 0x%x\n", ret);
  }

  // Do benchmark
  int bench_ret = do_benchmark(&matmul, &bench_info);
  if (bench_ret != 0)
  {
    dprintf(2, "Something went wrong...\n");
  }
  else
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
