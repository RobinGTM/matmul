#include <time.h>
#include <string.h>
#include <gsl/gsl_vector.h>
#include <gsl/gsl_matrix.h>
#include <gsl/gsl_blas.h>

#include "hardware.h"
#include "benchmark.h"
#include "matvec.h"
#include "matmul.h"

// Debug
static int print_gsl_matrix_float(FILE *f, const gsl_matrix_float * m)
{
  if (!m)
  {
    GSL_ERROR("Cannot print unallocated matrix.", GSL_EINVAL);
    return -1;
  }
  int status, n = 0;

  for (size_t i = 0; i < m->size1; i++)
  {
    for (size_t j = 0; j < m->size2; j++)
    {
      float coeff = gsl_matrix_float_get(m, i, j);
      unsigned int coeffx;
      memcpy(&coeffx, &coeff, sizeof(float));
      if ((status = fprintf(f, "%f(0x%08x) ", coeff, coeffx)) < 0)
      {
        return -1;
      }
      n += status;
    }

    if ((status = fprintf(f, "\n")) < 0)
    {
      return -1;
    }
    n += status;
  }

  return n;
}

static int print_gsl_vector_float(FILE * f, const gsl_vector_float * v)
{
  if (!v)
  {
    GSL_ERROR("Cannot print unallocated vector.", GSL_EINVAL);
    return -1;
  }
  int n = 0;
  int status = 0;
  for (size_t i = 0; i < v->size; i++)
  {
    float coeff = gsl_vector_float_get(v, i);
    unsigned int coeffx;
    memcpy(&coeffx, &coeff, sizeof(float));
    if (((status = fprintf(f, "%f(0x%08x)\n", coeff, coeffx))) < 0)
    {
      return -1;
    }
    n += status;
  }
  return n;
}

int do_benchmark(const matmul_t * matmul, benchinfo_t * bench_info)
{
  dprintf(2, "Random seed: %d\n", bench_info->seed);

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
  gsl_matrix_float * mat = gsl_matrix_float_alloc(matmul->m_height, matmul->m_width);
  gsl_vector_float * vec = gsl_vector_float_alloc(matmul->m_width);
  gsl_vector_float * hw_result = gsl_vector_float_alloc(matmul->m_height);
  gsl_vector_float * sw_result = gsl_vector_float_alloc(matmul->m_height);

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

    if (bench_info->dry_run)
    {
      printf("--------------- MATRIX NUMBER %d ---------------\n", m);
      print_gsl_matrix_float(stdout, mat);
    }
    else
    {
      // Program matrix into hardware
      RUN_CHECK(prog(matmul, mat), ret);
    }

    for (int v = 0; v < bench_info->n_vec; v++)
    {
      // Generate random vector
      ret = populate_randvec(vec);
      CHECK_RET(ret);

      // Dry run: just print things
      if (bench_info->dry_run)
      {
        printf("-------- VECTOR NUMBER %d (MATRIX %d) --------\n", v, m);
        print_gsl_vector_float(stdout, vec);
      }
      else
      {
        dprintf(2, "Sending vector number %d for matrix number %d...\n",
                v + 1, m + 1);
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

        if (bench_info->print)
        {
          dprintf(2, "HW\tSW\n");
          for (size_t i = 0; i < matmul->m_height; i++)
          {
            float hwcoeff, swcoeff;
            unsigned int hwcoeffx, swcoeffx;
            hwcoeff = gsl_vector_float_get(hw_result, i);
            swcoeff = gsl_vector_float_get(sw_result, i);
            memcpy(&hwcoeffx, &hwcoeff, sizeof(float));
            memcpy(&swcoeffx, &swcoeff, sizeof(float));
            dprintf(2, "%f(0x%x)\t%f(0x%x)\n", hwcoeff, hwcoeffx, swcoeff, swcoeffx);
          }
        }
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
