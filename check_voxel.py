import numpy as np
import os

path = "data/voxels/sample_0.npy"
if os.path.exists(path):
    data = np.load(path)
    print(f"File: {path}")
    print(f"Shape: {data.shape}")
    print(f"Sum: {data.sum()}")
    print(f"Unique values: {np.unique(data)}")
else:
    print(f"File {path} not found.")
