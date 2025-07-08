#include <sys/mman.h>
#include <unistd.h>
#include <stdlib.h>
#include <stdint.h>
#include <stdio.h>
#include <string.h>
#include <fcntl.h>
#include <time.h>

#include "matmul.h"
#include "matvec.h"
#include "hardware.h"

#define SOCK_H2C "/dev/xdma0_h2c_0"
#define SOCK_C2H "/dev/xdma0_c2h_0"
#define MM_CTL   "/dev/xdma0_user"

int main()
{



  int axi_wr_fd = open(SOCK_H2C, O_WRONLY);
  printf("axi_wr_fd: %d\n", axi_wr_fd);
  int axi_rd_fd = open(SOCK_C2H, O_RDONLY);
  printf("axi_rd_fd: %d\n", axi_rd_fd);
  int ctl_fd = open(MM_CTL, O_RDWR);
  if (ctl_fd < 0)
  {
    dprintf(2, "Could not open %s (%d)\n", MM_CTL, ctl_fd);
  }
  uint32_t * s_axil_ctl =
    (uint32_t *)mmap(NULL, 64, PROT_READ | PROT_WRITE, MAP_SHARED, ctl_fd, 0);
  close(ctl_fd);

  uint32_t ctl;

  printf("HxW = %dx%d\n", M_HEIGHT, M_WIDTH);
  ctl = s_axil_ctl[0];
  printf("CTL: 0x%x\n", ctl);

  // Floats are 32-bit but AXI bus is 64-bit
  uint64_t * coeffs = (uint64_t *)malloc(M_WIDTH * M_HEIGHT * sizeof(uint64_t));
  printf("Matrix:\n");

  // for (int i = 0; i < M_WIDTH; i++) {
  //   for (int j = 0; j < M_HEIGHT; j++) {
  //     if (i == 1)
  //     {
  //       float curr = (float)(1.0);
  //       memcpy(&coeffs[j * M_HEIGHT + i], &curr, sizeof(curr));
  //     }
  //     else
  //     {
  //       continue;
  //     }
  //   }
  // }
  for (int i = 0; i < M_WIDTH * M_HEIGHT; i++)
  {
    float curr = (float)((1));
    memcpy(&coeffs[i], &curr, sizeof(curr));
    printf("%.0f (0x%lx), ", curr, coeffs[i]);
  }
  printf("\n");

  uint64_t * vec = (uint64_t *)malloc(M_WIDTH * sizeof(uint64_t));
  printf("Vector:\n");
  for (int i = 0; i < M_HEIGHT; i++)
  {
    float curr = (float)((i + 1) % 2);
    memcpy(&vec[i], &curr, sizeof(curr));
    printf("%.0f (0x%lx), ", curr, vec[i]);
  }
  printf("\n");

  s_axil_ctl[0] = 0x1;
  ctl = s_axil_ctl[0];
  printf("CTL: 0x%x\n", ctl);

  // Send coeffs
  int written;
  written = write(axi_wr_fd, coeffs, M_HEIGHT * M_WIDTH * sizeof(uint64_t));
  printf("Wrote %d bytes from coeffs to axi_wr_fd\n", written);

  ctl = s_axil_ctl[0];
  printf("CTL: 0x%x\n", ctl);

  // Send vector
  written = write(axi_wr_fd, vec, M_HEIGHT * sizeof(uint64_t));
  printf("Wrote %d bytes from vec to axi_wr_fd\n", written);
  ctl = s_axil_ctl[0];
  printf("CTL: 0x%x\n", ctl);

  // ctl = s_axil_ctl[0];
  // int cnt = 0;
  // while ((ctl % 8) != 4)
  // {
  //   cnt += 1;
  //   printf("CTL: 0x%x (%dx)\n", ctl, cnt);
  //   sleep(1);
  // }

  uint64_t * out = (uint64_t *)malloc(M_HEIGHT * sizeof(uint64_t));
  // Read result
  int rdcnt = read(axi_rd_fd, out, M_HEIGHT * sizeof(*out));
  printf("Read %d bytes from matmul\n", rdcnt);

  float out_float[M_HEIGHT];
  for (int i = 0; i < M_HEIGHT; i++)
  {
    memcpy(&(out_float[i]), (uint64_t *)&(out[i]), sizeof(*out_float));
  }

  printf("-----------------------------------------\n");

  for (int i = 0; i < M_HEIGHT; i++)
  {
    printf("%.0f (0x%lx), ", out_float[i], out[i]);
  }
  printf("\n");

  free(coeffs);
  free(vec);
  free(out);
  
  return 0;
}
