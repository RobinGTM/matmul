#include <stdio.h>
#include <time.h>
#include <gsl/gsl_vector.h>
#include <gsl/gsl_matrix.h>

#include "matmul.h"
#include "matvec.h"
#include "hardware.h"

#define SOCK_H2C "/dev/xdma0_h2c_0"
#define SOCK_C2H "/dev/xdma0_c2h_0"
#define MM_CTL   "/dev/xdma0_user"

int main()
{
  srand(time(NULL));
  // srand(870833247);
  int seed = rand();
  dprintf(2, "Random seed: %d\n", seed);
  srand(seed);

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

  // Create vectors
  gsl_matrix_float * mat = gsl_matrix_float_alloc(M_HEIGHT, M_WIDTH);
  gsl_vector_float * vec = gsl_vector_float_alloc(M_WIDTH);
  gsl_vector_float * hw_result = gsl_vector_float_alloc(M_HEIGHT);
  gsl_vector_float * sw_result = gsl_vector_float_alloc(M_HEIGHT);

  // Generate random mat and vec
  ret = populate_randmat(mat);
  if (ret < 0)
  {
    return -ret;
  }
  ret = populate_randvec(vec);
  if (ret < 0)
  {
    return -ret;
  }
  // gsl_matrix_float_fprintf(stdout, mat, "%f");
  // printf("----------------------------------\n");
  // gsl_vector_float_fprintf(stdout, vec, "%f");
  // printf("----------------------------------\n");

  // Compute hardware matrix multiplication, programming matrix coeffs
  // into matmul
  ret = hw_matmul(&matmul, hw_result, mat, vec, 1);
  if (ret < 0)
  {
    return -ret;
  }

  // Compute software matrix multiplication (GSL-CBLAS)
  ret = sw_matmul(sw_result, mat, vec);
  if (ret < 0)
  {
    return -ret;
  }

  // Compare
  float score;
  ret = eucl_dist(&score, sw_result, hw_result);
  if (ret != 0)
  {
    return -ret;
  }
  else
  {
    printf("Distance between HW and SW vectors: %.10f\n", score);
  }

  return 0;
}
