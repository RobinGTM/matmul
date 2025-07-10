#include <sys/mman.h>
#include <unistd.h>
#include <fcntl.h>
#include <stdio.h>
#include <gsl/gsl_matrix.h>
#include <gsl/gsl_vector.h>
#include <string.h>
#include <errno.h>
#include <time.h>

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
    matmul->m_width = matmul->s_axil_ctl[WIDTH_REG];
    matmul->m_height = matmul->s_axil_ctl[HEIGHT_REG];
    matmul->saf = (matmul->s_axil_ctl[CTL_REG] >> SAF_BIT) % 2;
    if (!matmul->m_width)
    {
      dprintf(2, "[WARNING]: Could not read width from hardware, "
              "using %d defined in header\n", M_WIDTH);
      matmul->m_width = M_WIDTH;
    }
    if (!matmul->m_height)
    {
      dprintf(2, "[WARNING]: Could not height width from hardware, "
              "using %d defined in header\n", M_HEIGHT);
      matmul->m_height = M_HEIGHT;
    }
    return matmul->s_axil_ctl[CTL_REG];
  }
}

int prog(const matmul_t * matmul, const gsl_matrix_float * mat)
{
  if (mat->size1 != matmul->m_height || mat->size2 != matmul->m_width)
  {
    dprintf(2, "%s: Cannot send matrix with invalid size!\n", __FUNCTION__);
    dprintf(2, "(hardware is %dx%d while matrix is %ldx%ld)\n",
            matmul->m_height, matmul->m_width, mat->size1, mat->size2);
    return -1;
  }
  aximm_t * coeffs = (aximm_t *)malloc(sizeof(aximm_t) * matmul->m_height * matmul->m_width);
  unsigned int idx = 0;
  // Placeholder for matrix coeff
  float curr;
  for (int i = 0; i < matmul->m_height; i++)
  {
    for (int j = 0; j < matmul->m_width; j++)
    {
      curr = gsl_matrix_float_get(mat, i, j);
      memcpy(&coeffs[idx++], &curr, sizeof(float));
    }
  }
  // Write PROG to control register
  matmul->s_axil_ctl[CTL_REG] = CMD_PROG;

  //// DEBUG
  dprintf(2, "%s: CTL after PROG command: 0x%08x\n",
          __FUNCTION__, matmul->s_axil_ctl[CTL_REG]);

  // Write to XDMA host 2 card driver fd
  int written = write(matmul->s_axi_h2c_fd, coeffs,
                      matmul->m_height * matmul->m_width * sizeof(aximm_t));

  // Cleanup
  free(coeffs);

  if (written < 0)
  {
    dprintf(2, "%s: write returned %d (errno %d).\n", __FUNCTION__, written, errno);
    return written;
  }

  return matmul->s_axil_ctl[CTL_REG];
}

int send(const matmul_t * matmul, const gsl_vector_float * vec)
{
  if (vec->size != matmul->m_width)
  {
    dprintf(2, "%s: Cannot send vector with invalid size!\n", __FUNCTION__);
    dprintf(2, "(hardware matrix width is %d while vector has %ld coeffs)\n",
            matmul->m_width, vec->size);
    return -1;
  }
  aximm_t * coeffs = (aximm_t *)malloc(sizeof(aximm_t) * matmul->m_width);
  // Placeholder for vector coeff
  float curr;
  for (int i = 0; i < matmul->m_width; i++)
  {
    curr = gsl_vector_float_get(vec, i);
    memcpy(&coeffs[i], &curr, sizeof(float));
  }

  // Send vector
  int written = write(matmul->s_axi_h2c_fd, coeffs, matmul->m_width * sizeof(aximm_t));

  free(coeffs);

  if (written < 0)
  {
    dprintf(2, "%s: write returned %d (errno %d).\n", __FUNCTION__, written, errno);
    return written;
  }

  return matmul->s_axil_ctl[CTL_REG];
}

int recv(const matmul_t * matmul, gsl_vector_float * vec_out)
{
  if (vec_out->size != matmul->m_height)
  {
    dprintf(2, "%s: Cannot read into vector with invalid size!\n", __FUNCTION__);
    dprintf(2, "(hardware matrix height is %d while vector has %ld coeffs)\n",
            matmul->m_height, vec_out->size);
    return -1;
  }
  aximm_t * out = (aximm_t *)malloc(matmul->m_height * sizeof(aximm_t));

  // Read XDMA driver output
  int readcnt = read(matmul->s_axi_c2h_fd, out, matmul->m_height * sizeof(aximm_t));

  if (readcnt < 0)
  {
    free(out);
    dprintf(2, "%s: read returned %d (errno %d).\n", __FUNCTION__, readcnt, errno);
    return -1;
  }

  // Read output into vector
  float curr;
  for (int i = 0; i < matmul->m_height; i++)
  {
    memcpy(&curr, (aximm_t *)&(out[i]), sizeof(float));
    // Output of matmul is reversed
    gsl_vector_float_set(vec_out, matmul->m_height - 1 - i, curr);
  }

  return matmul->s_axil_ctl[CTL_REG];
}

int detach_matmul(matmul_t * matmul)
{
  if (!matmul)
  {
    dprintf(2, "matmul is not allocated.\n");
    return -1;
  }
  if (matmul->s_axil_ctl)
  {
    if (munmap((void *)matmul->s_axil_ctl, MMAP_LEN) != 0)
    {
      dprintf(2, "munmap failed.\n");
      return -1;
    }
  }
  if (matmul->s_axi_h2c_fd > 0)
  {
    close(matmul->s_axi_h2c_fd);
  }
  if (matmul->s_axi_c2h_fd > 0)
  {
    close(matmul->s_axi_c2h_fd);
  }
  return 0;
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

int hw_matmul(const matmul_t * matmul, gsl_vector_float * vec_out,
              const gsl_matrix_float * mat, const gsl_vector_float * vec,
              int do_prog)
{
  int ret;

  // Program matrix if asked so
  if (do_prog == 1)
  {
    RUN_CHECK(prog(matmul, mat), ret);
  }
  // Send vector
  RUN_CHECK(send(matmul, vec), ret);
  struct timespec slp_req = { .tv_sec = 0, .tv_nsec = 10 * 100};
  struct timespec slp_rem = { .tv_sec = 0, .tv_nsec = 0 };
  nanosleep(&slp_req, &slp_rem);
  // Read output
  RUN_CHECK(recv(matmul, vec_out), ret);
  return matmul->s_axil_ctl[CTL_REG];
}
