#ifndef __MATMUL_H__
#define __MATMUL_H__

#include <stdint.h>
#include <stddef.h>
#include <gsl/gsl_matrix.h>
#include <gsl/gsl_vector.h>

#define MATMUL_PROG   1
#define MATMUL_NOPROG 0

typedef uint64_t aximm_t;
typedef uint32_t axilctl_t;

typedef struct matmul_struct
{
  axilctl_t *  s_axil_ctl;
  int          s_axi_h2c_fd;
  int          s_axi_c2h_fd;
  unsigned int m_width;
  unsigned int m_height;
  char         saf;
} matmul_t;

// Attach XDMA driver devices to matmul struct
int attach_matmul(matmul_t * matmul);
// Attach XDMA driver devices to matmul struct
int detach_matmul(matmul_t * matmul);
// Program matrix to 
int prog(const matmul_t * matmul, const gsl_matrix_float * mat);
// Send input vector
int send(const matmul_t * matmul, const gsl_vector_float * vec);
// Read output into vector
int recv(const matmul_t * matmul, gsl_vector_float * vec_out);
// Perform hardware matrix-vector-multiplication
// If do_prog == 1, program matrix into matmul before multiplying
int hw_matmul(const matmul_t * matmul, gsl_vector_float * vec_out,
              const gsl_matrix_float * mat, const gsl_vector_float * vec,
              int do_prog);

#endif /* __MATMUL_H__ */
