
import json
import numpy as np
import os
import sys

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
    
    # Collect all points to find the bounding box
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
    
    # Size of the model
    size = max_coords - min_coords
    max_dim = size.max()
    
    # Voxel grid
    grid = np.zeros((grid_size, grid_size, grid_size), dtype=np.uint8)
    
    # Scale factor
    effective_grid_size = grid_size - 2 * padding
    scale = effective_grid_size / max_dim if max_dim > 0 else 1
    
    for el in elements:
        if 'from' not in el or 'to' not in el:
            continue
            
        # Scale coordinates
        # Blockbench coordinates: [x, y, z]
        start = (np.array(el['from']) - min_coords) * scale + padding
        end = (np.array(el['to']) - min_coords) * scale + padding
        
        # Round to integers
        s_idx = np.floor(np.minimum(start, end)).astype(int)
        e_idx = np.ceil(np.maximum(start, end)).astype(int)
        
        # Ensure within bounds
        s_idx = np.clip(s_idx, 0, grid_size - 1)
        e_idx = np.clip(e_idx, 0, grid_size - 1)
        
        # Fill the box in the grid
        # grid[x, y, z]
        grid[s_idx[0]:e_idx[0]+1, s_idx[1]:e_idx[1]+1, s_idx[2]:e_idx[2]+1] = 1
                    
    return grid

if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: python bbmodel_to_voxel.py <input.bbmodel> <output.npy>")
        sys.exit(1)
        
    input_path = sys.argv[1]
    output_path = sys.argv[2]
    
    grid = bbmodel_to_voxel(input_path)
    if grid is not None:
        # Create directory if it doesn't exist
        os.makedirs(os.path.dirname(output_path), exist_ok=True)
        np.save(output_path, grid)
        print(f"Successfully converted {input_path} to {output_path} ({grid.sum()} voxels)")
