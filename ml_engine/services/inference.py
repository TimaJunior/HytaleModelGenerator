
import torch
import os
import time
from ml_engine.core.interfaces import IEncoder, IGenerator
from ml_engine.models.gan import ResNetEncoder, VoxelGANGenerator

class ModelInferenceService:
    """
    Service to handle the full pipeline: Image -> Encoder -> Latent -> Generator -> 3D Voxel.
    """
    def __init__(self, weights_path: str = None, device: str = "cpu"):
        self.device = torch.device(device)
        self.latent_dim = 256
        
        # Initialize models
        self.encoder: IEncoder = ResNetEncoder(latent_dim=self.latent_dim).to(self.device)
        self.generator: IGenerator = VoxelGANGenerator(latent_dim=self.latent_dim).to(self.device)
        
        self.encoder.eval()
        self.generator.eval()

        if weights_path and os.path.exists(weights_path):
            self.load_weights(weights_path)
    
    def load_weights(self, path: str):
        """
        Load pretrained weights for both encoder and generator.
        Expects a dict: {'encoder': state_dict, 'generator': state_dict}
        """
        checkpoint = torch.load(path, map_location=self.device)
        self.encoder.load_state_dict(checkpoint['encoder'])
        self.generator.load_state_dict(checkpoint['generator'])
        import sys
        sys.stderr.write(f"Weights loaded from {path}\n")

    def generate_from_image(self, image_tensor: torch.Tensor) -> torch.Tensor:
        """
        Run the generation pipeline.
        :param image_tensor: (1, 3, 256, 256) Normalized image
        :return: (1, 32, 32, 32) Voxel grid (Probability map)
        """
        start_time = time.time()
        
        with torch.no_grad():
            image_tensor = image_tensor.to(self.device)
            
            # 1. Encode image to latent vector
            latent_vector = self.encoder(image_tensor)
            
            # 2. Generate voxels from latent vector
            voxels = self.generator(latent_vector)
            
            # 3. Thresholding (Optional)
            # voxels = (voxels > 0.5).float()
            
            end_time = time.time()
            duration_ms = (end_time - start_time) * 1000
            import sys
            sys.stderr.write(f"[Inference] Generated voxel grid in {duration_ms:.2f}ms\n")
            
            return voxels
