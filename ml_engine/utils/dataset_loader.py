"""
Manifest-based Voxel Dataset Loader.

Завантажує image-voxel пари з manifest.json замість count-based
логіки. Підтримує train/val split.
"""

import os
import json
import torch
import numpy as np
from PIL import Image
from torch.utils.data import Dataset
from torchvision import transforms


class VoxelDataset(Dataset):
    """
    Dataset для завантаження image-voxel пар з manifest.

    Args:
        data_dir: Директорія з даними (містить images/, voxels/, manifest.json)
        split: "train" або "val" (потребує manifest.json)
        manifest_path: Явний шлях до manifest (за замовчуванням data_dir/manifest.json)
        transform: Трансформації для зображень
    """

    def __init__(
        self,
        data_dir: str = "data",
        split: str | None = None,
        manifest_path: str | None = None,
        transform=None,
    ):
        self.data_dir = data_dir

        self.transform = transform or transforms.Compose([
            transforms.Resize((256, 256)),
            transforms.ToTensor(),
        ])

        # Визначаємо шлях до manifest
        if manifest_path is None:
            manifest_path = os.path.join(data_dir, "manifest.json")

        if os.path.exists(manifest_path):
            self._load_from_manifest(manifest_path, split)
        else:
            # Fallback: legacy count-based mode (для backward compat)
            self._load_legacy(data_dir)

    def _load_from_manifest(self, manifest_path: str, split: str | None):
        """Завантажує пари з manifest.json."""
        with open(manifest_path, "r", encoding="utf-8") as f:
            manifest = json.load(f)

        samples = manifest["samples"]

        # Фільтруємо по split та enabled
        if split is not None:
            samples = [s for s in samples
                       if s["split"] == split and s.get("enabled", True)]
        else:
            samples = [s for s in samples if s.get("enabled", True)]

        self.samples = samples
        self._mode = "manifest"

    def _load_legacy(self, data_dir: str):
        """
        Legacy mode: парить за sorted filename.
        Використовується лише якщо manifest.json не знайдено.
        """
        image_dir = os.path.join(data_dir, "images")
        voxel_dir = os.path.join(data_dir, "voxels")

        image_files = sorted(
            [f for f in os.listdir(image_dir) if f.endswith(".png")]
        )
        voxel_files = sorted(
            [f for f in os.listdir(voxel_dir) if f.endswith(".npy")]
        )

        # У legacy mode парим за base name, не за index
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

    def __len__(self) -> int:
        return len(self.samples)

    def __getitem__(self, idx: int) -> tuple[torch.Tensor, torch.Tensor]:
        sample = self.samples[idx]

        # Завантажуємо image
        img_path = os.path.join(self.data_dir, sample["image_path"])
        image = Image.open(img_path).convert("RGB")
        if self.transform:
            image = self.transform(image)

        # Завантажуємо voxel
        vox_path = os.path.join(self.data_dir, sample["voxel_path"])
        voxels = np.load(vox_path).astype(np.float32)
        voxel_tensor = torch.from_numpy(voxels).unsqueeze(0)  # (1, 32, 32, 32)

        return image, voxel_tensor

    @property
    def mode(self) -> str:
        """Повертає режим роботи: 'manifest' або 'legacy'."""
        return self._mode

    @property
    def sample_ids(self) -> list[str]:
        """Повертає список ID всіх семплів."""
        return [s["id"] for s in self.samples]
