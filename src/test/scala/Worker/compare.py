#!/bin/env python
import numpy as np

WID=0

a = np.array([
    pow(-1, (i + 1) % 2) * i
    for i in range(257 - (WID + 1) * 16, 257 - (WID + 1) * 16 + 16)
])

b = 10 * np.array(range(1, 17))
print(np.dot(a, b))
