"""
Manifest-based Voxel Dataset Loader.

Підтримує як нові RGBA 64³ семпли (64,64,64,4) → tensor (4,64,64,64),
так і legacy binary 32³ (32,32,32) → tensor (1,32,32,32) для зворотньої сумісності.
"""

import os
import json
import torch
import numpy as np
from PIL import Image
from torch.utils.data import Dataset
from torchvision import transforms
import random


def _random_flip_rgba(voxel_np: np.ndarray) -> np.ndarray:
    """Випадкове дзеркалення по осі X."""
    if random.random() < 0.5:
        return voxel_np[::-1, :, :, :].copy()
    return voxel_np


def _random_color_jitter(voxel_np: np.ndarray, strength: float = 0.1) -> np.ndarray:
    """Невелике зашумлення RGB каналів для data augmentation."""
    rgb = voxel_np[:, :, :, :3].astype(np.float32)
    noise = np.random.uniform(-strength, strength, (1, 1, 1, 3)) * 255
    rgb = np.clip(rgb + noise, 0, 255).astype(np.uint8)
    result = voxel_np.copy()
    result[:, :, :, :3] = rgb
    return result


class VoxelDataset(Dataset):
    """
    Dataset для завантаження image-voxel пар з manifest.

    Args:
        data_dir: Директорія з даними (містить images/, voxels/, manifest.json)
        split: "train" або "val"
        manifest_path: Явний шлях до manifest
        transform: Трансформації для зображень
        augment: Чи застосовувати data augmentation (тільки при train)
    """

    def __init__(
        self,
        data_dir: str = "data",
        split: str | None = None,
        manifest_path: str | None = None,
        transform=None,
        augment: bool = True,
    ):
        self.data_dir = data_dir
        self.augment = augment and (split == "train")

        self.transform = transform or transforms.Compose([
            transforms.Resize((256, 256)),
            transforms.ColorJitter(brightness=0.2, contrast=0.2, saturation=0.2),
            transforms.ToTensor(),
            transforms.Normalize(mean=[0.5, 0.5, 0.5], std=[0.5, 0.5, 0.5]),
        ])

        if manifest_path is None:
            manifest_path = os.path.join(data_dir, "manifest.json")

        if os.path.exists(manifest_path):
            self._load_from_manifest(manifest_path, split)
        else:
            self._load_legacy(data_dir)

    def _load_from_manifest(self, manifest_path: str, split: str | None):
        with open(manifest_path, "r", encoding="utf-8") as f:
            manifest = json.load(f)

        # Читаємо формат з маніфесту (version 2 має "format" поле)
        fmt = manifest.get("format", {})
        self.resolution = fmt.get("resolution", 32)
        self.channels = fmt.get("channels", 1)

        samples = manifest["samples"]
        if split is not None:
            samples = [s for s in samples
                       if s["split"] == split and s.get("enabled", True)]
        else:
            samples = [s for s in samples if s.get("enabled", True)]

        self.samples = samples
        self._mode = "manifest"

    def _load_legacy(self, data_dir: str):
        """Legacy mode: парить за sorted filename."""
        image_dir = os.path.join(data_dir, "images")
        voxel_dir = os.path.join(data_dir, "voxels")

        image_files = sorted([f for f in os.listdir(image_dir) if f.endswith(".png")])
        voxel_files = sorted([f for f in os.listdir(voxel_dir) if f.endswith(".npy")])

        image_bases = {os.path.splitext(f)[0]: f for f in image_files}
        voxel_bases = {os.path.splitext(f)[0]: f for f in voxel_files}
        matched = sorted(set(image_bases.keys()) & set(voxel_bases.keys()))

        self.samples = [
            {
                "id": base,
                "image_path": os.path.join("images", image_bases[base]),
                "voxel_path": os.path.join("voxels", voxel_bases[base]),
            }
            for base in matched
        ]
        self._mode = "legacy"
        self.resolution = 32
        self.channels = 1

    def __len__(self) -> int:
        return len(self.samples)

    def __getitem__(self, idx: int) -> tuple[torch.Tensor, torch.Tensor]:
        sample = self.samples[idx]

        # --- Зображення ---
        img_path = os.path.join(self.data_dir, sample["image_path"])
        image = Image.open(img_path).convert("RGB")
        if self.transform:
            image = self.transform(image)

        # --- Voxels ---
        vox_path = os.path.join(self.data_dir, sample["voxel_path"])
        voxels_np = np.load(vox_path)

        if voxels_np.ndim == 4 and voxels_np.shape[3] == 4:
            # RGBA 64³ format: (D, H, W, 4) → нормалізація → (4, D, H, W)
            if self.augment:
                voxels_np = _random_flip_rgba(voxels_np)
                voxels_np = _random_color_jitter(voxels_np)

            voxels_f = voxels_np.astype(np.float32) / 255.0
            # (D, H, W, 4) → (4, D, H, W)
            voxel_tensor = torch.from_numpy(voxels_f).permute(3, 0, 1, 2)

        elif voxels_np.ndim == 3:
            # Legacy binary 32³: (D, H, W) → (1, D, H, W)
            voxel_tensor = torch.from_numpy(voxels_np.astype(np.float32)).unsqueeze(0)

        else:
            # Невідомий формат — повертаємо порожній тензор
            voxel_tensor = torch.zeros(4, 64, 64, 64, dtype=torch.float32)

        return image, voxel_tensor

    @property
    def mode(self) -> str:
        return self._mode

    @property
    def sample_ids(self) -> list[str]:
        return [s["id"] for s in self.samples]
