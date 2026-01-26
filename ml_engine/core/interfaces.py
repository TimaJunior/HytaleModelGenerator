from abc import ABC, abstractmethod
import torch

class IEncoder(ABC):
    """
    Interface for 2D Encoder (SRP: Feature Extraction).
    """
    @abstractmethod
    def forward(self, x: torch.Tensor) -> torch.Tensor:
        """
        Encodes a 2D image into a latent vector.
        :param x: Input image tensor (B, C, H, W)
        :return: Latent vector (B, Latent_Dim)
        """
        pass

class IGenerator(ABC):
    """
    Interface for 3D Generator (SRP: Voxel Generation).
    """
    @abstractmethod
    def forward(self, z: torch.Tensor) -> torch.Tensor:
        """
        Generates a 3D voxel grid from a latent vector.
        :param z: Latent vector (B, Latent_Dim)
        :return: Voxel grid (B, 1, D, H, W)
        """
        pass

class IDiscriminator(ABC):
    """
    Interface for 3D Discriminator (SRP: Real/Fake Classification).
    """
    @abstractmethod
    def forward(self, x: torch.Tensor) -> torch.Tensor:
        """
        Classifies a 3D model as real or fake.
        :param x: Voxel grid (B, 1, D, H, W)
        :return: Probability score (B, 1) or Logits
        """
        pass
