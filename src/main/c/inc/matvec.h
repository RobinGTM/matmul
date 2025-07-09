#ifndef __MATVEC_H__
#define __MATVEC_H__

#include <gsl/gsl_vector.h>
#include <gsl/gsl_matrix.h>

// Generate a random vector
int populate_randvec(gsl_vector_float * out);
// Generate a random matrix
int populate_randmat(gsl_matrix_float * out);
// Euclidian distance between two gsl_vector_float
int eucl_dist(float * out, const gsl_vector_float * vec1, const gsl_vector_float * vec2);
// Software matrix-vector multiplication
int sw_matmul(gsl_vector_float * out,
              const gsl_matrix_float * mat, const gsl_vector_float * vec);
// Debugging
int print_gsl_matrix_float(FILE *f, const gsl_matrix_float *m);

#endif /* __MATVEC_H__ */
