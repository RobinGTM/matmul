#include <unistd.h>
#include <stdint.h>
#include <stdio.h>
#include <string.h>
#include <fcntl.h>
#include <time.h>

#include "matmul.h"

int main()
{
  int axi_wr_fd = open(SOCK_H2C, O_WRONLY);
  printf("axi_wr_fd: %d\n", axi_wr_fd);
  int axi_rd_fd = open(SOCK_C2H, O_RDONLY);
  printf("axi_rd_fd: %d\n", axi_rd_fd);

  // Floats are 32-bit but AXI bus is 64-bit
  uint64_t vec[M_HEIGHT];

  for (int i = 0; i < M_HEIGHT; i++)
  {
    float curr = (float)(i + 1.0f);
    memcpy(&vec[i], &curr, sizeof(curr));
    printf("%f, ", curr);
  }
  printf("\n");

  uint64_t out[M_HEIGHT];

  write(axi_wr_fd, vec, M_HEIGHT * sizeof(uint64_t));

  sleep(1);

  read(axi_rd_fd, out, M_HEIGHT * sizeof(uint64_t));

  printf("-----------------------------------------\n");

  for (int i = 0; i < M_HEIGHT; i++)
  {
    printf("%f (0x%x), ", out[i], out[i]);
  }
  printf("\n");
  
  return 0;
}
