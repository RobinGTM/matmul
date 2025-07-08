#ifndef __MATVEC_H__
#define __MATVEC_H__

#include <gsl/gsl_vector.h>
#include <gsl/gsl_matrix.h>

// Generate a random vector
void gen_randvec(gsl_vector_float * out);
// Generate a random matrix
void gen_randmat(gsl_matrix_float * out);
// Euclidian distance between two gsl_vector_float
int eucl_dist(float * out, const gsl_vector_float * vec1, const gsl_vector_float * vec2);

#endif /* __MATVEC_H__ */
