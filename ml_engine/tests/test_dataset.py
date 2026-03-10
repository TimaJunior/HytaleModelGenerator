"""
Тести для dataset loader та manifest integrity.
"""

import os
import json
import pytest
import numpy as np

# Визначаємо project root
PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
DATA_DIR = os.path.join(PROJECT_ROOT, "data")
MANIFEST_PATH = os.path.join(DATA_DIR, "manifest.json")


# ─────────────────────────────────────────────
# Manifest Integrity Tests
# ─────────────────────────────────────────────

class TestManifestIntegrity:
    """Перевірка цілісності manifest.json."""

    @pytest.fixture(autouse=True)
    def load_manifest(self):
        assert os.path.exists(MANIFEST_PATH), "manifest.json не знайдено"
        with open(MANIFEST_PATH, "r", encoding="utf-8") as f:
            self.manifest = json.load(f)
        self.samples = self.manifest["samples"]

    def test_manifest_has_required_fields(self):
        """Manifest має всі обов'язкові поля."""
        assert "version" in self.manifest
        assert "stats" in self.manifest
        assert "samples" in self.manifest

    def test_samples_have_required_fields(self):
        """Кожен sample має повний набір полів."""
        required = {"id", "image_path", "voxel_path", "voxel_hash",
                     "asset_id", "split", "enabled"}
        for s in self.samples:
            missing = required - set(s.keys())
            assert not missing, f"Sample {s.get('id','?')} missing: {missing}"

    def test_no_orphan_pairs(self):
        """Кожен image_path та voxel_path існує на диску."""
        for s in self.samples:
            img = os.path.join(DATA_DIR, s["image_path"])
            vox = os.path.join(DATA_DIR, s["voxel_path"])
            assert os.path.exists(img), f"Image not found: {img}"
            assert os.path.exists(vox), f"Voxel not found: {vox}"

    def test_no_empty_voxels(self):
        """Жоден enabled sample не має порожній voxel grid."""
        for s in self.samples:
            if not s.get("enabled", True):
                continue
            vox = os.path.join(DATA_DIR, s["voxel_path"])
            data = np.load(vox)
            assert data.sum() > 0, f"Empty voxel: {s['id']}"

    def test_no_leakage_between_splits(self):
        """Жоден voxel_hash не з'являється і в train, і в val."""
        train_hashes = {s["voxel_hash"] for s in self.samples
                        if s["split"] == "train"}
        val_hashes = {s["voxel_hash"] for s in self.samples
                      if s["split"] == "val"}
        leakage = train_hashes & val_hashes
        assert len(leakage) == 0, f"Leakage: {len(leakage)} shared hashes"

    def test_both_splits_have_samples(self):
        """Обидва splits мають хоча б один sample."""
        train = [s for s in self.samples if s["split"] == "train"]
        val = [s for s in self.samples if s["split"] == "val"]
        assert len(train) > 0, "Train split порожній"
        assert len(val) > 0, "Val split порожній"

    def test_stats_match_actual_counts(self):
        """Stats у manifest відповідають фактичній кількості."""
        stats = self.manifest["stats"]
        assert stats["total_samples"] == len(self.samples)
        train_count = sum(1 for s in self.samples if s["split"] == "train")
        val_count = sum(1 for s in self.samples if s["split"] == "val")
        assert stats["train_samples"] == train_count
        assert stats["val_samples"] == val_count


# ─────────────────────────────────────────────
# Dataset Loader Tests
# ─────────────────────────────────────────────

class TestVoxelDataset:
    """Smoke tests для VoxelDataset loader."""

    def test_train_split_loads(self):
        """Train split ініціалізується та має samples."""
        from ml_engine.utils.dataset_loader import VoxelDataset
        ds = VoxelDataset(data_dir=DATA_DIR, split="train")
        assert len(ds) > 0, "Train dataset порожній"
        assert ds.mode == "manifest"

    def test_val_split_loads(self):
        """Val split ініціалізується та має samples."""
        from ml_engine.utils.dataset_loader import VoxelDataset
        ds = VoxelDataset(data_dir=DATA_DIR, split="val")
        assert len(ds) > 0, "Val dataset порожній"
        assert ds.mode == "manifest"

    def test_getitem_returns_correct_shapes(self):
        """__getitem__ повертає правильні shapes."""
        from ml_engine.utils.dataset_loader import VoxelDataset
        ds = VoxelDataset(data_dir=DATA_DIR, split="train")
        image, voxel = ds[0]
        assert image.shape == (3, 256, 256), f"Image shape: {image.shape}"
        assert voxel.shape == (1, 32, 32, 32), f"Voxel shape: {voxel.shape}"

    def test_train_val_no_overlap(self):
        """Train і val не перетинаються по sample id."""
        from ml_engine.utils.dataset_loader import VoxelDataset
        train_ds = VoxelDataset(data_dir=DATA_DIR, split="train")
        val_ds = VoxelDataset(data_dir=DATA_DIR, split="val")
        train_ids = set(train_ds.sample_ids)
        val_ids = set(val_ds.sample_ids)
        overlap = train_ids & val_ids
        assert len(overlap) == 0, f"Overlap: {overlap}"

    def test_all_split_loads_everything(self):
        """Без split завантажуються всі enabled samples."""
        from ml_engine.utils.dataset_loader import VoxelDataset
        ds_all = VoxelDataset(data_dir=DATA_DIR)
        ds_train = VoxelDataset(data_dir=DATA_DIR, split="train")
        ds_val = VoxelDataset(data_dir=DATA_DIR, split="val")
        assert len(ds_all) == len(ds_train) + len(ds_val)
