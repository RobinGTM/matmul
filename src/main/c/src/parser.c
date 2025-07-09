#include <stdio.h>
#include <time.h>
#include <string.h>
#include <getopt.h>
#include <stdlib.h>

#include "parser.h"
#include "benchmark.h"
#include "hardware.h"

#define DFL_NMAT 1
#define DFL_NVEC 1

void print_hw(void)
{
#ifdef HARDFLOAT
  printf("%dx%d_hardfloat\n", M_HEIGHT, M_WIDTH);
#else
  printf("%dx%d_saf\n", M_HEIGHT, M_WIDTH);
#endif /* HARDFLOAT */
}

void print_usage(char * name)
{
  printf("Usage: %s", name);
  if (strlen(name) > 48) printf(" \\\n");
  else { for (int i = 0; i < (51 - strlen(name)); i++) { printf(" "); }; printf("\\\n"); }
  printf("       [-m/--n-matrices <n_matrices>] [-s/--seed <seed>]  \\\n");
  printf("       [-n/--n-vectors <n_vecs>] [-w/--hardware]          \\\n");
  printf("       [-h/--help]\n");
  for (int i = 0; i < strlen(name) + strlen("Usage: "); i++) { printf("-"); }; printf("\n");
  printf("  -m <n_matrices>: generate <n_matrices> random float matrices (default %d)\n",
         DFL_NMAT);
  printf("  -n <n_vecs>:     generate <n_vecs> random vectors for each matrix (default %d)\n",
         DFL_NVEC);
  printf("  -s <seed>:       use <seed> to seed random generator (default: time(NULL))\n");
  printf("  -w:              print hardware information and exit\n");
  printf("  -h:              print this help and exit\n");
  for (int i = 0; i < strlen(name) + strlen("Usage: "); i++) { printf("-"); }; printf("\n");
  printf("HARDWARE: ");
  print_hw();
}

static void init_benchinfo(benchinfo_t * bench_info)
{
  bench_info->n_mat = DFL_NMAT;
  bench_info->n_vec = DFL_NVEC;
  bench_info->seed = time(NULL);
  bench_info->max_err = 0.0f;
  bench_info->mean_err = 0.0f;
  bench_info->max_rel_err = 0.0f;
  bench_info->mean_rel_err = 0.0f;
  bench_info->hw_max_time = 0;
  bench_info->hw_tot_time = 0;
  bench_info->sw_max_time = 0;
  bench_info->sw_tot_time = 0;
}

parser_exit_t parse_args(int argc, char ** argv, benchinfo_t * bench_info)
{
  init_benchinfo(bench_info);
  static const char * OPTSTRING = "-n:m:s:wh";
  static struct option const LONGOPTS[] =
  {
    {"n-matrices", required_argument, NULL, 'm'},
    {"n-vectors", required_argument, NULL, 'n'},
    {"seed", required_argument, NULL, 's'},
    {"hardware", no_argument, NULL, 'w'},
    {"help", no_argument, NULL, 'h'},
  };

  int c;
  while ((c = getopt_long(argc, argv, OPTSTRING, LONGOPTS, NULL)) != -1)
  {
    switch(c)
    {
    case 'n':
      bench_info->n_vec = atoi(optarg);
      if (!bench_info->n_vec || bench_info->n_vec < 0)
      {
        dprintf(2, "%s: Invalid value: n-matrices must be a strictly positive int, ",
                __FUNCTION__);
        dprintf(2, "got \"%s\".\n", optarg);
        memset(bench_info, '\0', sizeof(*bench_info));
        return EXIT_ERR;
      }
      break;
    case 'm':
      bench_info->n_mat = atoi(optarg);
      if (!bench_info->n_mat || bench_info->n_mat < 0)
      {
        dprintf(2, "%s: Invalid value: n-vectors must be a strictly positive int, ",
                __FUNCTION__);
        dprintf(2, "got \"%s\".\n", optarg);
        memset(bench_info, '\0', sizeof(*bench_info));
        return EXIT_ERR;
      }
      break;
    case 's':
      bench_info->seed = atoi(optarg);
      if (!bench_info->seed)
      {
        dprintf(2, "%s: Invalid value: seed must be an int, got %s.\n",
                __FUNCTION__, optarg);
        memset(bench_info, '\0', sizeof(*bench_info));
        return EXIT_ERR;
      }
      break;
    case 'h':
      return EXIT_HELP;
      break;
    case 'w':
      return EXIT_HW;
      break;
    default:
      return EXIT_ERR;
    }
  }
  return EXIT_OK;
}

void print_benchinfo(benchinfo_t * b)
{
  dprintf(2, "benchinfo at 0x%p:\n", (void *)b);
  dprintf(2, "  n_mat:\t%d\n", b->n_mat);
  dprintf(2, "  n_vec:\t%d\n", b->n_vec);
  dprintf(2, "  seed: \t%d\n", b->seed);
  dprintf(2, "  max_err:\t%f\n", b->max_err);
  dprintf(2, "  mean_err:\t%f\n", b->mean_err);
  dprintf(2, "  max_rel_err:\t%f\n", b->max_rel_err);
  dprintf(2, "  mean_rel_err:\t%f\n", b->mean_rel_err);
}
