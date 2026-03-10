
import trimesh
import numpy as np
import os
import sys

def obj_to_voxel(obj_path, grid_size=32, padding=2):
    try:
        mesh = trimesh.load(obj_path)
        # If it's a scene, take the first geometry
        if isinstance(mesh, trimesh.Scene):
            if len(mesh.geometry) == 0:
                return None
            mesh = trimesh.util.concatenate([g for g in mesh.geometry.values() if isinstance(g, trimesh.Trimesh)])
        
        # Ensure it's a mesh
        if not isinstance(mesh, trimesh.Trimesh):
            print(f"Error: Loaded object is not a mesh: {type(mesh)}")
            return None

        # Scale to fit in grid
        bounds = mesh.bounds
        size = bounds[1] - bounds[0]
        max_dim = size.max()
        
        effective_grid_size = grid_size - 2 * padding
        scale = effective_grid_size / max_dim if max_dim > 0 else 1
        
        # Move to origin and scale
        mesh.apply_translation(-bounds[0])
        mesh.apply_scale(scale)
        # Center in padding
        mesh.apply_translation([padding, padding, padding])
        
        # Voxelize
        # pitch is the size of each voxel
        voxel_grid = mesh.voxelized(pitch=1.0).matrix
        
        # Ensure it fits in 32x32x32
        final_grid = np.zeros((grid_size, grid_size, grid_size), dtype=np.uint8)
        
        # Get shape of voxelized matrix
        s = voxel_grid.shape
        # Clip if it's larger for some reason
        sx, sy, sz = min(s[0], grid_size), min(s[1], grid_size), min(s[2], grid_size)
        final_grid[:sx, :sy, :sz] = voxel_grid[:sx, :sy, :sz]
        
        return final_grid
    except Exception as e:
        print(f"Error converting {obj_path}: {e}")
        return None

if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: python obj_to_voxel.py <input.obj> <output.npy>")
        sys.exit(1)
        
    input_path = sys.argv[1]
    output_path = sys.argv[2]
    
    grid = obj_to_voxel(input_path)
    if grid is not None:
        os.makedirs(os.path.dirname(output_path), exist_ok=True)
        np.save(output_path, grid)
        print(f"Successfully converted {input_path} to {output_path} ({grid.sum()} voxels)")
