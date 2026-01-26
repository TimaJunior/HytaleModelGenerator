
import os
import numpy as np
import random
from PIL import Image, ImageDraw
import json

def ensure_dirs(base_path="data"):
    os.makedirs(os.path.join(base_path, "images"), exist_ok=True)
    os.makedirs(os.path.join(base_path, "voxels"), exist_ok=True)

def generate_random_shape(grid_size=32):
    """Generates a random 3D voxel shape (sphere, cube, or noise cloud)."""
    voxels = np.zeros((grid_size, grid_size, grid_size), dtype=np.uint8)
    shape_type = random.choice(["sphere", "cube", "cloud"])
    
    center = grid_size // 2
    
    if shape_type == "sphere":
        radius = random.randint(4, grid_size // 2 - 2)
        y, x, z = np.ogrid[:grid_size, :grid_size, :grid_size]
        dist_from_center = np.sqrt((x - center)**2 + (y - center)**2 + (z - center)**2)
        voxels[dist_from_center <= radius] = 1
        
    elif shape_type == "cube":
        size = random.randint(4, grid_size // 2)
        start = center - size // 2
        end = start + size
        voxels[start:end, start:end, start:end] = 1
        
    elif shape_type == "cloud":
        # Random scattered points
        num_points = random.randint(50, 200)
        for _ in range(num_points):
            x, y, z = random.randint(0, grid_size-1), random.randint(0, grid_size-1), random.randint(0, grid_size-1)
            voxels[x, y, z] = 1
            
            # small clusters
            if random.random() > 0.5:
                if x+1 < grid_size: voxels[x+1, y, z] = 1
                if y+1 < grid_size: voxels[x, y+1, z] = 1
                if z+1 < grid_size: voxels[x, y, z+1] = 1
                
    return voxels

def simple_render(voxels, grid_size=32, output_size=(256, 256)):
    """
    Very simple orthographic projection renderer.
    Maps 3D voxels to a 2D image by checking depth.
    """
    image = Image.new("RGB", output_size, "black")
    draw = ImageDraw.Draw(image)
    
    # Scale voxel grid to image size
    scale = output_size[0] // grid_size
    
    # Iterate voxels and draw 'front' faces
    # Z-buffer approach (painters algorithm, back to front)
    
    # Simple isometric-ish look: 
    # Just project x,y directly for now (front view) for simplicity of MVP
    # Ideally should be isometric, but front-view is enough for "shape completion" task
    
    for z in range(grid_size):
        for y in range(grid_size):
            for x in range(grid_size):
                if voxels[z, y, x] == 1:
                    # Draw a rectangle
                    # In 3D: z is depth, y is height, x is width
                    # Screen: x -> x, y -> y (inverted normally)
                    
                    # Add some "depth" color shading based on Z
                    shade = 100 + (z * 155 // grid_size)
                    color = (0, shade, 0) # Greenish
                    
                    # Project
                    px = x * scale
                    py = (grid_size - 1 - y) * scale # Flip Y for image coords
                    
                    draw.rectangle([px, py, px + scale - 1, py + scale - 1], fill=color)
                    
    return image

def generate_dataset(num_samples=100, output_dir="data"):
    print(f"Generating {num_samples} synthetic samples...")
    ensure_dirs(output_dir)
    
    for i in range(num_samples):
        voxels = generate_random_shape()
        image = simple_render(voxels)
        
        # Save Voxel Data (as simple compressed numpy or JSON)
        # Using numpy .npy for efficiency in ML, but JSON for debugging if needed
        # We'll use .npy for training speed
        np.save(os.path.join(output_dir, "voxels", f"sample_{i}.npy"), voxels)
        
        # Save Image
        image.save(os.path.join(output_dir, "images", f"sample_{i}.png"))
        
        if i % 50 == 0:
            print(f"Generated {i}/{num_samples}")

    print("Done!")

if __name__ == "__main__":
    generate_dataset(num_samples=500)
