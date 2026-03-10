
import os
import sys
import numpy as np
import trimesh
from PIL import Image, ImageDraw
import math
from pathlib import Path

# Add script directory to path to import helpers
sys.path.append(os.path.dirname(os.path.abspath(__file__)))
from obj_to_voxel import obj_to_voxel
from bbmodel_to_voxel import bbmodel_to_voxel

# Constants
VOXEL_RESOLUTION = 32
NUM_VIEWS = 8
IMAGE_SIZE = (256, 256)

def simple_render(voxels: np.ndarray, angle_x: float = 0, angle_y: float = 0, 
                  output_size: tuple = IMAGE_SIZE) -> Image.Image:
    grid_size = voxels.shape[0]
    image = Image.new("RGB", output_size, "black")
    draw = ImageDraw.Draw(image)
    
    scale = output_size[0] // grid_size
    center = output_size[0] // 2
    
    depth_buffer = []
    
    cos_y = math.cos(math.radians(angle_y))
    sin_y = math.sin(math.radians(angle_y))
    cos_x = math.cos(math.radians(angle_x))
    sin_x = math.sin(math.radians(angle_x))
    
    for z in range(grid_size):
        for y in range(grid_size):
            for x in range(grid_size):
                if voxels[x, y, z] == 1: # Adjusting axes if needed to match BBMODEL/OBJ
                    cx = x - grid_size // 2
                    cy = y - grid_size // 2
                    cz = z - grid_size // 2
                    
                    rx = cx * cos_y - cz * sin_y
                    rz = cx * sin_y + cz * cos_y
                    
                    ry = cy * cos_x - rz * sin_x
                    final_z = cy * sin_x + rz * cos_x
                    
                    screen_x = int(center + rx * scale)
                    screen_y = int(center - ry * scale)
                    
                    depth = (final_z + grid_size) / (2 * grid_size)
                    shade = int(100 + depth * 155)
                    color = (int(shade * 0.5), shade, int(shade * 0.8))
                    
                    depth_buffer.append((final_z, screen_x, screen_y, scale, color))
    
    depth_buffer.sort(key=lambda x: x[0])
    
    for _, sx, sy, s, color in depth_buffer:
        draw.rectangle([sx, sy, sx + s, sy + s], fill=color)
    
    return image

def render_views(voxels: np.ndarray):
    views = []
    angles = [
        (0, 0), (0, 90), (0, 180), (0, 270),
        (30, 45), (30, 135), (30, 225), (30, 315)
    ]
    for ax, ay in angles:
        views.append(simple_render(voxels, angle_x=ax, angle_y=ay))
    return views

def process_file(file_path, output_dir, start_index):
    file_path = Path(file_path)
    print(f"Processing: {file_path.name}")
    
    voxels = None
    if file_path.suffix.lower() == '.obj':
        voxels = obj_to_voxel(str(file_path))
    elif file_path.suffix.lower() == '.bbmodel':
        voxels = bbmodel_to_voxel(str(file_path))
    
    if voxels is None or np.sum(voxels) == 0:
        print(f"  Warning: No voxels for {file_path}")
        return 0
        
    views = render_views(voxels)
    
    images_dir = os.path.join(output_dir, "images")
    voxels_dir = os.path.join(output_dir, "voxels")
    
    for i, view in enumerate(views):
        idx = start_index + i
        np.save(os.path.join(voxels_dir, f"real_{idx}.npy"), voxels)
        view.save(os.path.join(images_dir, f"real_{idx}.png"))
        
    return len(views)

def main():
    data_root = Path("c:/Users/Tima/Documents/Projects/hytalemodelgenerator")
    output_dir = data_root / "data"
    
    # Scanning
    model_files = []
    search_dirs = [data_root / "data/raw_models", data_root / "dataset"]
    
    for s_dir in search_dirs:
        if not s_dir.exists(): continue
        model_files.extend(list(s_dir.glob("**/*.obj")))
        model_files.extend(list(s_dir.glob("**/*.bbmodel")))
        
    print(f"Found {len(model_files)} total model files.")
    
    # Unique files only (avoid duplicates in dataset)
    unique_files = {}
    for f in model_files:
        if f.name not in unique_files:
            unique_files[f.name] = f
            
    print(f"Found {len(unique_files)} unique models.")
    
    total_samples = 0
    current_index = 0
    
    for name, f_path in unique_files.items():
        try:
            count = process_file(f_path, output_dir, current_index)
            current_index += count
            total_samples += count
        except Exception as e:
            print(f"Error processing {f_path}: {e}")
            
    print(f"Done! Created {total_samples} samples from {len(unique_files)} models.")

if __name__ == "__main__":
    main()
