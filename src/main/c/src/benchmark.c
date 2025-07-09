#include <time.h>
#include <gsl/gsl_vector.h>
#include <gsl/gsl_matrix.h>
#include <gsl/gsl_blas.h>

#include "hardware.h"
#include "benchmark.h"
#include "matvec.h"
#include "matmul.h"

int do_benchmark(const matmul_t * matmul, benchinfo_t * bench_info)
{
  // Seed random number generator
  srand(bench_info->seed);

#define CHECK_RET(ret)                          \
  if ((ret) != 0)                               \
  {                                             \
    dprintf(2, "%s: non-zero return (%d)\n",    \
            __FUNCTION__, (ret));               \
  }

#define RUN_CHECK(funcall, status_placeholder)                          \
  (status_placeholder) = funcall;                                       \
  if ((status_placeholder) < 0)                                         \
  {                                                                     \
    dprintf(2, "%s: " #funcall " failed.\n", __FUNCTION__);             \
    return (status_placeholder);                                        \
  }                                                                     \
  else                                                                  \
  {                                                                     \
    dprintf(2, "%s: CTL after `" #funcall "`: 0x%08x\n",                \
            __FUNCTION__, status_placeholder);                          \
  }

  // Allocate vectors and matrix
  gsl_matrix_float * mat = gsl_matrix_float_alloc(M_HEIGHT, M_WIDTH);
  gsl_vector_float * vec = gsl_vector_float_alloc(M_WIDTH);
  gsl_vector_float * hw_result = gsl_vector_float_alloc(M_HEIGHT);
  gsl_vector_float * sw_result = gsl_vector_float_alloc(M_HEIGHT);

  int total_runs = bench_info->n_mat * bench_info->n_vec;

  // Return placeholder
  int ret;
  // Error measurements
  float err;
  float norm;
  // Time measurements
  clock_t start;
  clock_t chrono;
  for (int m = 0; m < bench_info->n_mat; m++)
  {
    // Generate random matrix
    ret = populate_randmat(mat);
    CHECK_RET(ret);

    // Program matrix into hardware
    RUN_CHECK(prog(matmul, mat), ret);
    for (int v = 0; v < bench_info->n_vec; v++)
    {
      dprintf(2, "Sending vector number %d for matrix number %d...\n",
              v + 1, m + 1);
      // Generate random vector
      ret = populate_randvec(vec);
      CHECK_RET(ret);

      // Hardware matrix multiplication
      start = clock();
      RUN_CHECK(hw_matmul(matmul, hw_result, mat, vec, MATMUL_NOPROG), ret);
      chrono = clock() - start;

      // Accumulate chrono
      bench_info->hw_tot_time += chrono;
      if (chrono > bench_info->hw_max_time)
      {
        bench_info->hw_max_time = chrono;
      }

      // Software matrix multiplication
      start = clock();
      ret = sw_matmul(sw_result, mat, vec);
      chrono = clock() - start;
      CHECK_RET(ret);

      // Accumulate chrono
      bench_info->sw_tot_time += chrono;
      if (chrono > bench_info->sw_max_time)
      {
        bench_info->sw_max_time = chrono;
      }

      // Compute euclidian distance between results
      ret = eucl_dist(&err, sw_result, hw_result);
      CHECK_RET(ret);
      dprintf(2, "Error: %f\n", err);

      // Accumulate absolute error
      bench_info->mean_err += err;
      if (err > bench_info->max_err)
      {
        bench_info->max_err = err;
      }

      // Compute norm for relative error
      norm = gsl_blas_snrm2(vec);
      err /= norm;
      // Accumulate relative error
      bench_info->mean_rel_err += err;
      if (err > bench_info->max_rel_err)
      {
        bench_info->max_rel_err = err;
      }
    }
  }
  // Normalize means
  bench_info->mean_err /= (float)total_runs;
  bench_info->mean_rel_err /= (float)total_runs;
  return 0;
}

void print_results(benchinfo_t * bench_info)
{
  int tot_runs = bench_info->n_vec * bench_info->n_mat;
  printf("Mean times: HW %.16lf sec; SW %.16lf sec\n",
         ((double)bench_info->hw_tot_time / (double)tot_runs) / (double)CLOCKS_PER_SEC,
         ((double)bench_info->sw_tot_time / (double)tot_runs) / (double)CLOCKS_PER_SEC);
  printf("Max times: HW %.16lf sec; SW %.16lf sec\n",
         (double)bench_info->hw_max_time / (double)CLOCKS_PER_SEC,
         (double)bench_info->sw_max_time / (double)CLOCKS_PER_SEC);
  printf("Absolute error: MEAN %.16f; MAX %.16f\n",
         bench_info->mean_err, bench_info->max_err);
  printf("Relative error: MEAN %.16f %%; MAX %.16f %%\n",
         bench_info->mean_rel_err * 100.0f,
         bench_info->max_rel_err * 100.0f);
}
