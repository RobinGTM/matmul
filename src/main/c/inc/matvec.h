/* matvec.h -- GSL-based and random generation matrix-vector functions
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
#endif /* __MATVEC_H__ */
