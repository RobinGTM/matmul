#include <stdio.h>
#include <stdlib.h>
#include <stddef.h>
#include <gsl/gsl_vector.h>
#include <gsl/gsl_matrix.h>
#include <gsl/gsl_blas.h>

#include "matvec.h"

int populate_randvec(gsl_vector_float * out)
{
  if (!out)
  {
    GSL_ERROR("out vector is not allocated.", GSL_EINVAL);
    return -1;
  }
  for (int i = 0; i < out->size; i++)
  {
    int rand1 = rand();
    int rand2;
    // Don't let rand2 be 0
    do { rand2 = rand(); } while (rand2 == 0);
    // Random sign (coin flip)
    // int sign = 2 * (int)(rand() <= RAND_MAX / 2) - 1;
    // Generate random coeff
    // float coeff = (float)sign * (float)rand1 / (float)rand2;
    float coeff = (float)rand1 / (float)rand2;
    // Place it in vec
    gsl_vector_float_set(out, i, coeff);
  }
  return 0;
}

int populate_randmat(gsl_matrix_float * out)
{
  if (!out)
  {
    GSL_ERROR("out matrix is not allocated.", GSL_EINVAL);
    return -1;
  }
  for (int i = 0; i < out->size1; i++)
  {
    for (int j = 0; j < out->size2; j++)
    {
      int rand1 = rand();
      int rand2;
      // Don't let rand2 be 0
      do { rand2 = rand(); } while (rand2 == 0);
      // Random sign (coin flip)
      // int sign = 2 * (int)(rand() <= RAND_MAX / 2) - 1;
      // Generate random coeff
      // float coeff = (float)sign * (float)rand1 / (float)rand2;
      float coeff = (float)rand1 / (float)rand2;
      // Place it in vec
      gsl_matrix_float_set(out, i, j, coeff);
    }
  }
  return 0;
}

int eucl_dist(float * out, const gsl_vector_float * vec1, const gsl_vector_float * vec2)
{
  if (!vec1 || !vec2)
  {
    GSL_ERROR("Both input vectors must be allocated.", GSL_EINVAL);
    return -1;
  }
  else if (vec1->size != vec2->size)
  {
    GSL_ERROR("vec1 and vec2 must have the same size.", GSL_EINVAL);
    return -1;
  }
  else
  {
    gsl_vector_float * vec1_cpy = gsl_vector_float_alloc(vec1->size);
    gsl_vector_float_memcpy(vec1_cpy, vec1);
    gsl_vector_float_sub(vec1_cpy, vec2);
    *out = gsl_blas_snrm2(vec1_cpy);
    return 0;
  }
}

int sw_matmul(gsl_vector_float * out,
              const gsl_matrix_float * mat, const gsl_vector_float * vec)
{
  if (!out || !mat || !vec)
  {
    GSL_ERROR("At least one of the arguments is not allocated", GSL_EINVAL);
    return -1;
  }
  else if (vec->size != mat->size2)
  {
    GSL_ERROR("Input vector size and matrix width are different", GSL_EINVAL);
    return -1;
  }
  else if (out->size != mat->size1)
  {
    GSL_ERROR("Output vector size and matrix height are different", GSL_EINVAL);
  }
  else
  {
    // BLAS sgemv computes y = α op(A)x + βy
    int ret = gsl_blas_sgemv(CblasNoTrans, 1.0f, mat, vec, 0.0, out);
    return ret;
  }
}

// int main()
// {
//   gsl_vector_float * vec  = gsl_vector_float_alloc(16);
//   gsl_vector_float * vec2 = gsl_vector_float_alloc(16);
//   gsl_matrix_float * mat  = gsl_matrix_float_alloc(16, 16);

//   gsl_vector_float * out  = gsl_vector_float_alloc(16);

//   srand(2308555);

//   gen_randvec(vec);
//   gen_randvec(vec2);
//   gen_randmat(mat);

//   // print_gsl_matrix_float(stdout, mat);
//   gsl_vector_float_fprintf(stdout, vec, "%f");
//   printf("----------------------------------\n");
//   gsl_vector_float_fprintf(stdout, vec2, "%f");

//   int ret = gsl_blas_sgemv(CblasNoTrans, 1.0f, mat, vec, 1.0, out);

//   printf("----------------------------------\n");
//   // gsl_vector_float_fprintf(stdout, out, "%f");

//   float dist;
//   ret = eucl_dist(&dist, vec, vec2);

//   printf("Distance: %f\n", dist);

//   return 0;
// }
