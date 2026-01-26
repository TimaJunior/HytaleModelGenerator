
import sys
import os
import json
import torch
import numpy as np
from PIL import Image
from torchvision import transforms

import contextlib

# Ensure project root is in path
sys.path.append(os.getcwd())

# Redirect stdout to stderr globally to prevent library noise
@contextlib.contextmanager
def strict_stdout():
    original_stdout = sys.stdout
    sys.stdout = sys.stderr
    try:
        yield original_stdout
    finally:
        sys.stdout = original_stdout

from ml_engine.services.inference import ModelInferenceService

def main():
    with strict_stdout() as real_stdout:
        if len(sys.argv) > 1 and sys.argv[1] == "--train":
            from ml_engine.train import train
            try:
                train(epochs=10)
                real_stdout.write(json.dumps({"status": "success", "message": "Training completed"}))
            except Exception as e:
                real_stdout.write(json.dumps({"error": str(e)}))
            sys.exit(0)

        if len(sys.argv) < 2:
            real_stdout.write(json.dumps({"error": "No image path provided"}))
            sys.exit(1)

    image_path = sys.argv[1]
    
        try:
            # Preprocess Image
            transform = transforms.Compose([
                transforms.Resize((256, 256)),
                transforms.ToTensor(),
                # Normalization (optional, match training)
            ])
            
            image = Image.open(image_path).convert('RGB')
            input_tensor = transform(image).unsqueeze(0) # (1, 3, 256, 256)

            # Run Inference
            weights_path = os.path.join("ml_engine", "weights", "latest.pth")
            if not os.path.exists(weights_path):
                weights_path = None
                
            service = ModelInferenceService(weights_path=weights_path, device="cpu") 
            output_voxels = service.generate_from_image(input_tensor)
            
            # Convert tensor to list for JSON serialization
            # Squeeze batch dimension and traverse
            # We output a sparse list of active voxels for efficiency
            
            # Simple dense array for now for simplicity in visualization prototype
            # Thresholding at 0.5
            active_voxels = (output_voxels > 0.5).int().squeeze().numpy()
            
            # Get coordinates of active voxels
            coords = np.argwhere(active_voxels == 1).tolist()
            
            result = {
                "status": "success",
                "model_shape": list(active_voxels.shape),
                "voxel_count": len(coords),
                "voxels": coords # List of [z, y, x]
            }
            
            real_stdout.write(json.dumps(result))

        except Exception as e:
            real_stdout.write(json.dumps({"error": str(e)}))
            sys.exit(1)

if __name__ == "__main__":
    main()
