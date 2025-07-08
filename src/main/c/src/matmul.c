#include <sys/mman.h>
#include <fcntl.h>
#include <stdio.h>
#include <gsl/gsl_matrix.h>
#include <gsl/gsl_vector.h>
#include <string.h>
#include <errno.h>

#include "hardware.h"
#include "matmul.h"

#define XDMA_H2C "/dev/xdma0_h2c_0"
#define XDMA_C2H "/dev/xdma0_c2h_0"
#define XDMA_CTL "/dev/xdma0_user"
#define MMAP_LEN 64

static inline int try_open(const char * filename, mode_t mode)
{
  int fd = open(filename, mode);
  if (fd < 0)
  {
    dprintf(2, "try_open: Could not open file: \"%s\" (%d)\n", filename, fd);
    exit(fd);
  }
  else
  {
    return fd;
  }
}

int attach_matmul(matmul_t * matmul)
{
  matmul->s_axi_h2c_fd = try_open(XDMA_H2C, O_WRONLY);
  matmul->s_axi_c2h_fd = try_open(XDMA_C2H, O_RDONLY);
  int ctl_fd = try_open(XDMA_CTL, O_RDWR);
  matmul->s_axil_ctl =
    (axilctl_t *)mmap(NULL, MMAP_LEN, PROT_READ | PROT_WRITE, MAP_SHARED, ctl_fd, 0);
  close(ctl_fd);
  if (matmul->s_axil_ctl == (void *)-1)
  {
    dprintf(2, "attach_matmul: Failed to MMAP control device \"%s\" (errno %d)\n",
            XDMA_CTL, errno);
    return -1;
  }
  else
  {
    return matmul->s_axil_ctl[CTL_REG];
  }
}

int prog(const matmul_t * matmul, const gsl_matrix_float * mat)
{
  if (mat->size1 != M_HEIGHT || mat->size2 != M_WIDTH)
  {
    dprintf(2, "%s: Cannot send matrix with invalid size!\n", __FUNCTION__);
    dprintf(2, "(hardware is %dx%d while matrix is %dx%d)\n",
            M_HEIGHT, M_WIDTH, mat->size1, mat->size2);
    return -1;
  }
  aximm_t * coeffs = (aximm_t *)malloc(sizeof(aximm_t) * M_HEIGHT * M_WIDTH);
  unsigned int idx = 0;
  for (int i = 0; i < M_HEIGHT; i++)
  {
    for (int j = 0; j < M_WIDTH; j++)
    {
      memcpy(&coeffs[idx++], &gsl_matrix_get(mat, i, j), sizeof(float));
    }
  }
  s_axil_ctl[0] = CMD_PROG;

  //// DEBUG
  for (int i = 0; i < M_HEIGHT * M_WIDTH; i++)
  {
    printf("%f\n", coeffs[i]);
  }
  //// END

  // Write to XDMA host 2 card driver fd
  int written = write(matmul->s_axi_h2c_fd, coeffs, M_HEIGHT * M_WIDTH * sizeof(aximm_t));

  free(coeffs);

  if (written < 0)
  {
    dprintf(2, "%s: write returned %d (errno %d).\n", __FUNCTION__, written, errno);
    return written;
  }

  return s_axil_ctl[CTL_REG];
}

int send(const matmul_t * matmul, const gsl_vector_float * vec)
{
  if (vec->size != M_WIDTH)
  {
    dprintf(2, "%s: Cannot send vector with invalid size!\n", __FUNCTION__);
    dprintf(2, "(hardware matrix width is %d while vector has %d coeffs)\n",
            M_WIDTH, vec->size);
    return -1;
  }
  aximm_t * coeffs = (aximm_t *)malloc(sizeof(aximm_t) * M_WIDTH);
  for (int i = 0; i < M_WIDTH; i++)
  {
    memcpy(&coeffs[i], &gsl_matrix_get(vec, i), sizeof(float));
  }

  //// DEBUG
  for (int i = 0; i < M_WIDTH; i++)
  {
    printf("%f\n", coeffs[i]);
  }
  //// END

  // Send vector
  int written = write(matmul->s_axi_h2c_fd, coeffs, M_WIDTH * sizeof(aximm_t));

  free(coeffs);

  if (written < 0)
  {
    dprintf(2, "%s: write returned %d (errno %d).\n", __FUNCTION__, written, errno);
    return written;
  }

  return s_axil_ctl[CTL_REG];
}

int read(const matmul_t * matmul, gsl_vector_float * vec_out)
{
  if (vec_out->size != M_HEIGHT)
  {
    dprintf(2, "%s: Cannot read into vector with invalid size!\n", __FUNCTION__);
    dprintf(2, "(hardware matrix height is %d while vector has %d coeffs)\n",
            M_HEIGHT, vec_out->size);
    return -1;
  }
  aximm_t * out = (aximm_t *)malloc(M_HEIGHT * sizeof(aximm_t));

  // Read XDMA driver output
  int readcnt = read(matmul->s_axi_c2h_fd, out, M_HEIGHT * sizeof(aximm_t));

  if (readcnt < 0)
  {
    free(out);
    dprintf(2, "%s: read returned %d (errno %d).\n", __FUNCTION__, readcnt, errno);
    return -1;
  }

  // Read output into vector
  float curr;
  for (int i = 0; i < M_HEIGHT; i++)
  {
    memcpy(&curr, (aximm_t *)&(out[i]), sizeof(float));
    // Output of matmul is reversed
    gsl_vector_float_set(vec_out, M_HEIGHT - 1 - i, curr);
  }

  return matmul->s_axil_ctl[CTL_REG];
}
