#ifndef __MATMUL_H__
#define __MATMUL_H__

#include <stdint.h>
#include <stddef.h>
#include <gsl/gsl_matrix.h>
#include <gsl/gsl_vector.h>

typedef uint64_t aximm_t;
typedef uint32_t axilctl_t;

typedef struct matmul_struct
{
  axilctl_t * s_axil_ctl;
  int         s_axi_h2c_fd;
  int         s_axi_c2h_fd;
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
int read(const matmul_t * matmul, gsl_vector_float * vec_out);

#endif /* __MATMUL_H__ */
