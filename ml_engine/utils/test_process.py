
import os
import sys
import numpy as np
import trimesh
from pathlib import Path

# Add script directory to path to import helpers
sys.path.append(os.path.dirname(os.path.abspath(__file__)))
from bbmodel_to_voxel import bbmodel_to_voxel

def test():
    model_path = r"c:\Users\Tima\Documents\Projects\hytalemodelgenerator\data\raw_models\extracted\weapons\hytale_sword_sample.bbmodel"
    if not os.path.exists(model_path):
        print(f"File {model_path} does not exist.")
        return
        
    print(f"Testing BBMODEL: {model_path}")
    grid = bbmodel_to_voxel(model_path)
    if grid is not None:
        print(f"  Grid sum: {grid.sum()}")
        print(f"  Grid shape: {grid.shape}")
    else:
        print("  Grid conversion failed.")

if __name__ == "__main__":
    test()
