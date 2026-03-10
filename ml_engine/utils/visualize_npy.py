
import sys
import numpy as np
import matplotlib.pyplot as plt

def visualize_npy(npy_path, output_image):
    grid = np.load(npy_path)
    fig = plt.figure(figsize=(8, 8))
    ax = fig.add_subplot(1, 1, 1, projection='3d')
    
    # Thresholding to binary (should already be binary)
    voxels_binary = grid > 0
    
    colors = np.empty(voxels_binary.shape, dtype=object)
    colors[voxels_binary] = '#10b981'

    ax.voxels(voxels_binary, facecolors=colors, edgecolor='k', linewidth=0.1)
    ax.set_title(f"Voxel Grid: {npy_path}")
    
    ax.view_init(elev=30, azim=45)
    plt.savefig(output_image)
    print(f"Visualization saved to {output_image}")

if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: python visualize_npy.py <input.npy> <output.png>")
        sys.exit(1)
    visualize_npy(sys.argv[1], sys.argv[2])
