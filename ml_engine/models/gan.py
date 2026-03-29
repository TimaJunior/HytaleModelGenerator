"""
Next-Gen Voxel GAN Architecture.

Архітектура:
- ResNetEncoder: зображення (3,256,256) → latent (512)
- VoxelGANGenerator: latent (512) → RGBA voxels (4, 64, 64, 64)
  з Self-Attention 3D, ResBlocks, Progressive upsampling
- ConditionalDiscriminator: PatchGAN 3D + Spectral Norm + LSGAN
"""

import torch
import torch.nn as nn
import torch.nn.functional as F
from torchvision import models
from ml_engine.core.interfaces import IEncoder, IGenerator, IDiscriminator


# ============================================================
# Допоміжні модулі
# ============================================================

class ResBlock3d(nn.Module):
    """3D Residual Block з BatchNorm."""
    def __init__(self, channels: int):
        super().__init__()
        self.block = nn.Sequential(
            nn.Conv3d(channels, channels, 3, padding=1, bias=False),
            nn.BatchNorm3d(channels),
            nn.ReLU(True),
            nn.Conv3d(channels, channels, 3, padding=1, bias=False),
            nn.BatchNorm3d(channels),
        )
        self.relu = nn.ReLU(True)

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        return self.relu(x + self.block(x))


class SelfAttention3d(nn.Module):
    """
    3D Self-Attention Module.
    Дозволяє кожному вокселю «бачити» всі інші вокселі на цьому рівні.
    Використовується для глобальної когерентності форм.
    """
    def __init__(self, channels: int):
        super().__init__()
        mid = max(channels // 8, 1)
        self.query = nn.Conv3d(channels, mid, 1, bias=False)
        self.key = nn.Conv3d(channels, mid, 1, bias=False)
        self.value = nn.Conv3d(channels, channels, 1, bias=False)
        self.gamma = nn.Parameter(torch.zeros(1))

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        B, C, D, H, W = x.shape
        N = D * H * W

        q = self.query(x).view(B, -1, N)                # (B, C//8, N)
        k = self.key(x).view(B, -1, N)                  # (B, C//8, N)
        v = self.value(x).view(B, -1, N)                # (B, C, N)

        # Attention: (B, N, N) = (B, N, C//8) @ (B, C//8, N)
        scale = q.shape[1] ** 0.5
        attn = torch.softmax(
            torch.bmm(q.permute(0, 2, 1), k) / scale, dim=-1
        )                                                # (B, N, N)

        # Output: (B, C, N) = (B, C, N) @ (B, N, N)
        out = torch.bmm(v, attn.permute(0, 2, 1))       # (B, C, N)
        out = out.view(B, C, D, H, W)
        return x + self.gamma * out


def spectral_conv3d(in_ch, out_ch, **kwargs) -> nn.Module:
    """Conv3d з Spectral Normalization для стабільності дискримінатора."""
    return nn.utils.spectral_norm(nn.Conv3d(in_ch, out_ch, **kwargs))


# ============================================================
# Encoder
# ============================================================

class ResNetEncoder(nn.Module, IEncoder):
    """
    Image Encoder: (3, 256, 256) → latent vector (512).
    Використовує ResNet18 з кастомною projection head.
    """
    def __init__(self, latent_dim: int = 512):
        super().__init__()
        resnet = models.resnet18(weights=models.ResNet18_Weights.DEFAULT)
        self.features = nn.Sequential(*list(resnet.children())[:-1])
        self.projection = nn.Sequential(
            nn.Linear(resnet.fc.in_features, 768),
            nn.ReLU(True),
            nn.Dropout(0.2),
            nn.Linear(768, latent_dim),
        )

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        x = self.features(x)
        x = torch.flatten(x, 1)
        return self.projection(x)


# ============================================================
# Generator: 64³ RGBA
# ============================================================

class VoxelGANGenerator(nn.Module, IGenerator):
    """
    RGBA Voxel Generator.
    Latent (512) → RGBA voxels (4, 64, 64, 64).

    Архітектура upsampling:
      512 → FC reshape (512, 2, 2, 2)
      ConvTranspose3d x6 → (4, 64, 64, 64)
    """
    def __init__(self, latent_dim: int = 512):
        super().__init__()
        self.latent_dim = latent_dim

        # Початковий FC шар
        self.fc = nn.Linear(latent_dim, 512 * 2 * 2 * 2)

        self.decoder = nn.Sequential(
            # (512, 2, 2, 2) → (256, 4, 4, 4)
            nn.ConvTranspose3d(512, 256, 4, 2, 1, bias=False),
            nn.BatchNorm3d(256),
            nn.ReLU(True),
            ResBlock3d(256),
            SelfAttention3d(256),  # Attention на малих масштабах для форми

            # (256, 4, 4, 4) → (128, 8, 8, 8)
            nn.ConvTranspose3d(256, 128, 4, 2, 1, bias=False),
            nn.BatchNorm3d(128),
            nn.ReLU(True),
            ResBlock3d(128),

            # (128, 8, 8, 8) → (64, 16, 16, 16)
            nn.ConvTranspose3d(128, 64, 4, 2, 1, bias=False),
            nn.BatchNorm3d(64),
            nn.ReLU(True),
            ResBlock3d(64),
            SelfAttention3d(64),  # Attention для середніх деталей

            # (64, 16, 16, 16) → (32, 32, 32, 32)
            nn.ConvTranspose3d(64, 32, 4, 2, 1, bias=False),
            nn.BatchNorm3d(32),
            nn.ReLU(True),
            ResBlock3d(32),

            # (32, 32, 32, 32) → (16, 64, 64, 64)
            nn.ConvTranspose3d(32, 16, 4, 2, 1, bias=False),
            nn.BatchNorm3d(16),
            nn.ReLU(True),
            ResBlock3d(16),
        )

        # Фінальний шар: (16, 64, 64, 64) → (4, 64, 64, 64)
        self.head = nn.Conv3d(16, 4, 3, padding=1, bias=True)

    def forward(self, z: torch.Tensor) -> torch.Tensor:
        x = self.fc(z)
        x = x.view(-1, 512, 2, 2, 2)
        x = self.decoder(x)
        x = self.head(x)

        # Розбиваємо на RGB та Alpha з різними активаціями
        rgb = torch.sigmoid(x[:, :3])     # [0, 1] — колір
        alpha = torch.sigmoid(x[:, 3:4])  # [0, 1] — існування

        return torch.cat([rgb, alpha], dim=1)  # (B, 4, 64, 64, 64)


# ============================================================
# Discriminator: PatchGAN 3D + SpectralNorm
# ============================================================

class ConditionalDiscriminator(nn.Module, IDiscriminator):
    """
    PatchGAN 3D Discriminator з Spectral Normalization.

    Замість глобального скаляра повертає PatchMap (1, D', H', W'),
    де кожен елемент — real/fake score для патчу.
    Це змушує генератор поліпшувати деталі в usіх частинах моделі.

    Input: voxel (B, 4, 64, 64, 64) + condition volume (B, 512)
    Output: patch scores (B, 1, 4, 4, 4)
    """
    def __init__(self, latent_dim: int = 512):
        super().__init__()

        # Проєкція latent у просторовий об'єм (64³)
        self.condition_proj = nn.Sequential(
            nn.Linear(latent_dim, 64 * 64 * 64),
            nn.LeakyReLU(0.2),
        )

        # PatchGAN encoder (5 каналів: 4 RGBA + 1 condition)
        self.encoder = nn.Sequential(
            # (5, 64, 64, 64) → (64, 32, 32, 32)
            spectral_conv3d(5, 64, kernel_size=4, stride=2, padding=1),
            nn.LeakyReLU(0.2, inplace=True),

            # (64, 32, 32, 32) → (128, 16, 16, 16)
            spectral_conv3d(64, 128, kernel_size=4, stride=2, padding=1),
            nn.GroupNorm(8, 128),
            nn.LeakyReLU(0.2, inplace=True),

            # (128, 16, 16, 16) → (256, 8, 8, 8)
            spectral_conv3d(128, 256, kernel_size=4, stride=2, padding=1),
            nn.GroupNorm(16, 256),
            nn.LeakyReLU(0.2, inplace=True),

            # (256, 8, 8, 8) → (512, 4, 4, 4)
            spectral_conv3d(256, 512, kernel_size=4, stride=2, padding=1),
            nn.GroupNorm(32, 512),
            nn.LeakyReLU(0.2, inplace=True),

            # (512, 4, 4, 4) → (1, 4, 4, 4) PatchMap
            spectral_conv3d(512, 1, kernel_size=3, stride=1, padding=1),
            # Без Sigmoid! LSGAN використовує MSE, не BCE
        )

    def forward(self, x: torch.Tensor, condition: torch.Tensor = None) -> torch.Tensor:
        """
        x: (B, 4, 64, 64, 64)
        condition: (B, 512) або None
        returns: (B, 1, 4, 4, 4) — PatchMap
        """
        if condition is not None:
            cond_vol = self.condition_proj(condition)
            cond_vol = cond_vol.view(-1, 1, 64, 64, 64)
        else:
            cond_vol = torch.zeros(x.shape[0], 1, 64, 64, 64, device=x.device)

        x_in = torch.cat([x, cond_vol], dim=1)  # (B, 5, 64, 64, 64)
        return self.encoder(x_in)  # (B, 1, 4, 4, 4)
