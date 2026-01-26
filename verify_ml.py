
import torch
import sys
import os

# Add project root to path so we can import ml_engine
sys.path.append(os.getcwd())

from ml_engine.services.inference import ModelInferenceService
from ml_engine.models.gan import VoxelDiscriminator

def test_architecture():
    print("--- Testing ML Architecture ---")
    
    # 1. Setup Service
    try:
        service = ModelInferenceService(device="cpu")
        print("[Pass] Service Initialized")
    except Exception as e:
        print(f"[Fail] Service Init: {e}")
        return

    # 2. Create Dummy Data (1 image, 3 channels, 256x256)
    dummy_image = torch.randn(1, 3, 256, 256)
    print(f"Input Image Shape: {dummy_image.shape}")

    # 3. Test Generator Pipeline
    try:
        output_voxels = service.generate_from_image(dummy_image)
        print(f"Output Voxel Shape: {output_voxels.shape}")
        
        if output_voxels.shape == (1, 1, 32, 32, 32):
            print("[Pass] Generator Output Shape is Correct (1, 32, 32, 32)")
        else:
            print("[Fail] Generator Output Shape Mismatch")
    except Exception as e:
        print(f"[Fail] Generation Pipeline: {e}")

    # 4. Test Discriminator
    try:
        discriminator = VoxelDiscriminator()
        # Pass the generated voxels to discriminator
        score = discriminator(output_voxels)
        print(f"Discriminator Score: {score.item()}")
        
        if score.shape == (1, 1):
             print("[Pass] Discriminator Output Shape is Correct")
    except Exception as e:
         print(f"[Fail] Discriminator: {e}")

if __name__ == "__main__":
    test_architecture()
