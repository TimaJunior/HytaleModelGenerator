
import os
import sys
import numpy as np
import json

def bbmodel_to_voxel(bbmodel_path, grid_size=32, padding=2):
    try:
        with open(bbmodel_path, 'r', encoding='utf-8') as f:
            data = json.load(f)
    except Exception as e:
        print(f"Error reading {bbmodel_path}: {e}")
        return None
    
    elements = data.get('elements', [])
    if not elements:
        print(f"No elements found in {bbmodel_path}")
        return None
    
    all_points = []
    for el in elements:
        if 'from' in el and 'to' in el:
            all_points.append(el['from'])
            all_points.append(el['to'])
    
    if not all_points:
        return None
        
    all_points = np.array(all_points)
    min_coords = all_points.min(axis=0)
    max_coords = all_points.max(axis=0)
    size = max_coords - min_coords
    max_dim = size.max()
    grid = np.zeros((grid_size, grid_size, grid_size), dtype=np.uint8)
    effective_grid_size = grid_size - 2 * padding
    scale = effective_grid_size / max_dim if max_dim > 0 else 1
    
    for el in elements:
        if 'from' not in el or 'to' not in el:
            continue
        start = (np.array(el['from']) - min_coords) * scale + padding
        end = (np.array(el['to']) - min_coords) * scale + padding
        s_idx = np.floor(np.minimum(start, end)).astype(int)
        e_idx = np.ceil(np.maximum(start, end)).astype(int)
        s_idx = np.clip(s_idx, 0, grid_size - 1)
        e_idx = np.clip(e_idx, 0, grid_size - 1)
        grid[s_idx[0]:e_idx[0]+1, s_idx[1]:e_idx[1]+1, s_idx[2]:e_idx[2]+1] = 1
    return grid

def test():
    model_path = r"c:\Users\Tima\Documents\Projects\hytalemodelgenerator\data\raw_models\extracted\weapons\hytale_sword_sample.bbmodel"
    print(f"Testing BBMODEL: {model_path}")
    grid = bbmodel_to_voxel(model_path)
    if grid is not None:
        print(f"  Grid sum: {grid.sum()}")
    else:
        print("  Failed.")

if __name__ == "__main__":
    test()
