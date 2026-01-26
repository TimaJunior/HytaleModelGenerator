
import torch
import torch.nn as nn
from torchvision import models
from ml_engine.core.interfaces import IEncoder, IGenerator, IDiscriminator

class ResNetEncoder(nn.Module, IEncoder):
    """
    Concrete Encoder using ResNet18.
    Encodes (3, 256, 256) -> (Latent_Dim).
    """
    def __init__(self, latent_dim: int = 256):
        super().__init__()
        # Use pretrained resnet for better feature extraction from the start
        resnet = models.resnet18(weights=models.ResNet18_Weights.DEFAULT)
        
        # Remove the last classification layer (fc)
        self.features = nn.Sequential(*list(resnet.children())[:-1])
        
        # Add a custom projection head to match our latent dimension
        self.projection = nn.Linear(resnet.fc.in_features, latent_dim)

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        # x: (B, 3, 256, 256)
        x = self.features(x)
        x = torch.flatten(x, 1)
        x = self.projection(x)
        return x

class VoxelGANGenerator(nn.Module, IGenerator):
    """
    Concrete Generator using 3D Transposed Convolutions.
    Decodes (Latent_Dim) -> (1, 32, 32, 32).
    """
    def __init__(self, latent_dim: int = 256):
        super().__init__()
        self.latent_dim = latent_dim
        
        # Initial dense layer to reshape latent vector into a small 3D cube
        self.fc = nn.Linear(latent_dim, 256 * 2 * 2 * 2)
        
        self.decoder = nn.Sequential(
            # Input: (256, 2, 2, 2)
            nn.ConvTranspose3d(256, 128, kernel_size=4, stride=2, padding=1),
            nn.BatchNorm3d(128),
            nn.ReLU(True),
            # (128, 4, 4, 4)
            
            nn.ConvTranspose3d(128, 64, kernel_size=4, stride=2, padding=1),
            nn.BatchNorm3d(64),
            nn.ReLU(True),
            # (64, 8, 8, 8)
            
            nn.ConvTranspose3d(64, 32, kernel_size=4, stride=2, padding=1),
            nn.BatchNorm3d(32),
            nn.ReLU(True),
            # (32, 16, 16, 16)
            
            nn.ConvTranspose3d(32, 1, kernel_size=4, stride=2, padding=1),
            # Output: (1, 32, 32, 32)
            nn.Sigmoid() # Output probability of voxel existence (0-1)
        )

    def forward(self, z: torch.Tensor) -> torch.Tensor:
        x = self.fc(z)
        x = x.view(-1, 256, 2, 2, 2)
        x = self.decoder(x)
        return x

class VoxelDiscriminator(nn.Module, IDiscriminator):
    """
    Concrete Discriminator using 3D CNN.
    Classifies (1, 32, 32, 32) -> Real/Fake (Score).
    """
    def __init__(self):
        super().__init__()
        self.encoder = nn.Sequential(
            # Input: (1, 32, 32, 32)
            nn.Conv3d(1, 32, kernel_size=4, stride=2, padding=1),
            nn.LeakyReLU(0.2, inplace=True),
            # (32, 16, 16, 16)
            
            nn.Conv3d(32, 64, kernel_size=4, stride=2, padding=1),
            nn.BatchNorm3d(64),
            nn.LeakyReLU(0.2, inplace=True),
            # (64, 8, 8, 8)
            
            nn.Conv3d(64, 128, kernel_size=4, stride=2, padding=1),
            nn.BatchNorm3d(128),
            nn.LeakyReLU(0.2, inplace=True),
            # (128, 4, 4, 4)
            
            nn.Conv3d(128, 1, kernel_size=4, stride=1, padding=0),
            # Output: (1, 1, 1, 1) -> Scalar
            nn.Sigmoid()
        )

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        return self.encoder(x).view(-1, 1)
