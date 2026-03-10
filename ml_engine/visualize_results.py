
import os
import torch
import numpy as np
import matplotlib.pyplot as plt
from PIL import Image
from torchvision import transforms
from ml_engine.models.gan import ResNetEncoder, VoxelGANGenerator
import random

def visualize_inference(
    weights_path="ml_engine/weights/latest.pth",
    data_dir="data",
    output_image="visualization_result.png",
    device="cuda" if torch.cuda.is_available() else "cpu",
    threshold=0.5
):
    print(f"Loading models onto {device}...")
    encoder = ResNetEncoder().to(device)
    generator = VoxelGANGenerator().to(device)

    # 1. Load weights
    if not os.path.exists(weights_path):
        print(f"Error: Weights file {weights_path} not found.")
        return

    checkpoint = torch.load(weights_path, map_location=device)
    encoder.load_state_dict(checkpoint['encoder'])
    generator.load_state_dict(checkpoint['generator'])
    
    encoder.eval()
    generator.eval()

    # 2. Pick a random sample from data
    image_dir = os.path.join(data_dir, "images")
    if not os.path.exists(image_dir):
        print(f"Error: Data directory {image_dir} not found.")
        return

    sample_files = [f for f in os.listdir(image_dir) if f.endswith('.png')]
    if not sample_files:
        print("No images found for visualization.")
        return

    sample_name = random.choice(sample_files)
    sample_path = os.path.join(image_dir, sample_name)
    
    # 3. Preprocess image
    image_raw = Image.open(sample_path).convert("RGB")
    transform = transforms.Compose([
        transforms.Resize((256, 256)),
        transforms.ToTensor(),
    ])
    image_tensor = transform(image_raw).unsqueeze(0).to(device)

    # 4. Inference
    with torch.no_grad():
        latent = encoder(image_tensor)
        generated_voxels = generator(latent)
    
    # Remove batch and channel dims -> (32, 32, 32)
    voxels_np = generated_voxels.squeeze().cpu().numpy()
    
    # 5. Visualization
    fig = plt.figure(figsize=(12, 5))

    # Subplot 1: Input Image
    ax1 = fig.add_subplot(1, 2, 1)
    ax1.imshow(image_raw)
    ax1.set_title(f"Input: {sample_name}")
    ax1.axis('off')

    # Subplot 2: Generated Voxels (3D)
    ax2 = fig.add_subplot(1, 2, 2, projection='3d')
    
    # Thresholding to binary
    voxels_binary = voxels_np > threshold
    
    # Create colors based on voxel density or just a single color
    colors = np.empty(voxels_binary.shape, dtype=object)
    colors[voxels_binary] = '#10b981' # Hytale Emerald/Greenish

    ax2.voxels(voxels_binary, facecolors=colors, edgecolor='k', linewidth=0.1)
    ax2.set_title(f"Generated Voxels (thresh={threshold})")
    
    # Improve viewing angle
    ax2.view_init(elev=30, azim=45)

    plt.tight_layout()
    plt.savefig(output_image)
    print(f"Visualization saved to {output_image}")
    plt.show()

if __name__ == "__main__":
    visualize_inference()
