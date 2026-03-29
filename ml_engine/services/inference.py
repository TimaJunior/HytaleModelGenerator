"""
Inference Service for RGBA Voxel GAN.

Підтримує новий RGBA output (4, 64, 64, 64):
- generate_from_image() повертає тензор (4, 64, 64, 64)
- get_colored_voxels() повертає список {"pos": [x,y,z], "color": "#RRGGBB"}
"""

import torch
import torch.nn.functional as F
import numpy as np
import os
import sys
import time
from ml_engine.core.interfaces import IEncoder, IGenerator
from ml_engine.models.gan import ResNetEncoder, VoxelGANGenerator


class ModelInferenceService:
    """
    Full pipeline: Image → Encoder → Latent → Generator → RGBA Voxel Grid.
    """
    def __init__(self, weights_path: str = None, device: str = "cpu"):
        self.device = torch.device(device)
        self.latent_dim = 512  # Оновлено: 256 → 512

        self.encoder: IEncoder = ResNetEncoder(latent_dim=self.latent_dim).to(self.device)
        self.generator: IGenerator = VoxelGANGenerator(latent_dim=self.latent_dim).to(self.device)

        self.encoder.eval()
        self.generator.eval()

        if weights_path and os.path.exists(weights_path):
            self.load_weights(weights_path)

    def load_weights(self, path: str):
        """
        Завантажує ваги. Пріоритет: best.pth над latest.pth.
        Спробує EMA generator якщо доступно.
        """
        actual_path = path
        dir_name = os.path.dirname(path)
        best_path = os.path.join(dir_name, "best.pth")

        if os.path.basename(path) == "latest.pth" and os.path.exists(best_path):
            actual_path = best_path
            sys.stderr.write(f"Found best.pth, prioritizing over latest.pth\n")

        try:
            checkpoint = torch.load(actual_path, map_location=self.device, weights_only=True)
            self.encoder.load_state_dict(checkpoint['encoder'])

            # Віддаємо перевагу EMA генератору (стабільніший)
            if 'ema_generator' in checkpoint:
                self.generator.load_state_dict(checkpoint['ema_generator'])
                sys.stderr.write(f"Using EMA generator weights\n")
            else:
                self.generator.load_state_dict(checkpoint['generator'])

            sys.stderr.write(f"Weights loaded from {actual_path}\n")
        except Exception as e:
            sys.stderr.write(f"WARNING: Failed to load weights: {e}\n")
            raise RuntimeError(f"Corrupt weights file: {actual_path} ({e})")

    def generate_from_image(self, image_tensor: torch.Tensor) -> torch.Tensor:
        """
        Run the generation pipeline.

        Args:
            image_tensor: (1, 3, 256, 256) normalized image tensor
        Returns:
            (4, 64, 64, 64) RGBA voxel tensor (values in [0, 1])
        """
        t0 = time.time()
        with torch.no_grad():
            image_tensor = image_tensor.to(self.device)
            latent = self.encoder(image_tensor)
            voxels = self.generator(latent)
            elapsed = (time.time() - t0) * 1000
            sys.stderr.write(f"[Inference] Generated in {elapsed:.1f}ms\n")
            return voxels.squeeze(0)  # (4, 64, 64, 64)

    def get_colored_voxels(
        self,
        image_tensor: torch.Tensor,
        threshold: float = 0.4,
        max_voxels: int = 4096,
    ) -> list[dict]:
        """
        Запускає inference і повертає список кольорових вокселів.

        Returns:
            [{"pos": [x, y, z], "color": "#RRGGBB"}, ...]
        """
        voxels = self.generate_from_image(image_tensor)  # (4, 64, 64, 64)

        rgb = voxels[:3].cpu().numpy()     # (3, 64, 64, 64) — [0, 1]
        alpha = voxels[3].cpu().numpy()    # (64, 64, 64) — [0, 1]

        # Бінаризація Alpha
        occupied = np.argwhere(alpha >= threshold)

        if len(occupied) == 0:
            sys.stderr.write("Warning: No occupied voxels above threshold\n")
            return []

        # Обмежуємо кількість вокселів (сортуємо за alpha descending)
        if len(occupied) > max_voxels:
            alpha_vals = alpha[occupied[:, 0], occupied[:, 1], occupied[:, 2]]
            top_idx = np.argsort(alpha_vals)[::-1][:max_voxels]
            occupied = occupied[top_idx]

        result = []
        for coord in occupied:
            x, y, z = int(coord[0]), int(coord[1]), int(coord[2])
            r = int(rgb[0, x, y, z] * 255)
            g = int(rgb[1, x, y, z] * 255)
            b = int(rgb[2, x, y, z] * 255)
            hex_color = f"#{r:02X}{g:02X}{b:02X}"
            result.append({"pos": [x, y, z], "color": hex_color})

        return result
