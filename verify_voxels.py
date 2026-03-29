import os
import json
import numpy as np
import matplotlib.pyplot as plt
from PIL import Image
import random

def visualize_samples(num_samples=5):
    data_dir = "data"
    manifest_path = os.path.join(data_dir, "manifest.json")
    
    with open(manifest_path, "r", encoding="utf-8") as f:
        manifest = json.load(f)
        
    samples = manifest["samples"]
    selected = random.sample(samples, min(num_samples, len(samples)))
    
    fig, axes = plt.subplots(num_samples, 4, figsize=(16, 4 * num_samples))
    if num_samples == 1:
        axes = [axes]
        
    for i, sample in enumerate(selected):
        # Load image
        img_path = os.path.join(data_dir, sample["image_path"])
        img = Image.open(img_path).convert("RGB")
        
        # Load voxel
        voxel_path = os.path.join(data_dir, sample["voxel_path"])
        voxel = np.load(voxel_path) # expected shape: (32, 32, 32)
        
        if voxel.ndim == 4:
            voxel = voxel.squeeze(0) # handle (1, 32, 32, 32)
            
        # Plot Image
        axes[i][0].imshow(img)
        axes[i][0].set_title(f"Image\n{sample['id']}")
        axes[i][0].axis("off")
        
        # Plot Voxel Projections
        axes[i][1].imshow(np.max(voxel, axis=0), cmap='gray')
        axes[i][1].set_title("Voxel Max-Proj X")
        axes[i][1].axis("off")
        
        axes[i][2].imshow(np.max(voxel, axis=1), cmap='gray')
        axes[i][2].set_title("Voxel Max-Proj Y")
        axes[i][2].axis("off")
        
        axes[i][3].imshow(np.max(voxel, axis=2), cmap='gray')
        axes[i][3].set_title("Voxel Max-Proj Z")
        axes[i][3].axis("off")

    plt.tight_layout()
    output_path = "voxel_verification.png"
    plt.savefig(output_path)
    print(f"✅ Збережено візуалізацію: {output_path}")

if __name__ == "__main__":
    visualize_samples()
