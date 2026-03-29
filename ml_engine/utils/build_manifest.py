"""
Побудова канонічного Dataset Manifest.

Скануює data/images та data/voxels, парить за base name,
дедуплікує по SHA-256 voxel hash, ділить на train/val по
унікальному voxel asset (без data leakage).

Використання:
    python -m ml_engine.utils.build_manifest --data-dir data
    python -m ml_engine.utils.build_manifest --data-dir data --dry-run
    python -m ml_engine.utils.build_manifest --data-dir data --val-ratio 0.15
"""

import os
import sys
import json
import hashlib
import argparse
import numpy as np
from pathlib import Path
from datetime import datetime, timezone
from collections import defaultdict


def compute_voxel_hash(voxel_path: str) -> str | None:
    """
    Обчислює SHA-256 hash від бінарного вмісту .npy файлу.

    Returns:
        Hex-encoded hash string або None якщо файл не читається.
    """
    try:
        data = np.load(voxel_path)
        return hashlib.sha256(data.tobytes()).hexdigest()
    except Exception:
        return None


def is_voxel_valid(voxel_path: str) -> bool:
    """
    Перевіряє що voxel grid не порожній і має правильну форму.
    Підтримує як RGBA 64³ (64,64,64,4), так і legacy 32³ (32,32,32).
    """
    try:
        data = np.load(voxel_path)
        if data.shape == (64, 64, 64, 4):
            # RGBA format: перевіряємо Alpha канал
            if data[:, :, :, 3].sum() == 0:
                return False
            return True
        elif data.shape == (32, 32, 32):
            # Legacy binary format
            if data.sum() == 0:
                return False
            return True
        else:
            return False
    except Exception:
        return False


def scan_pairs(data_dir: str) -> tuple[list[dict], dict]:
    """
    Скануює data_dir/images та data_dir/voxels, парить за base name.

    Returns:
        (matched_pairs, stats) де matched_pairs — список dicts з полями
        id, image_path, voxel_path; stats — словник зі статистикою.
    """
    images_dir = os.path.join(data_dir, "images")
    voxels_dir = os.path.join(data_dir, "voxels")

    # Збираємо всі файли за base name (без розширення)
    image_bases = {}
    for f in os.listdir(images_dir):
        if f.endswith(".png"):
            base = os.path.splitext(f)[0]
            image_bases[base] = os.path.join("images", f)

    voxel_bases = {}
    for f in os.listdir(voxels_dir):
        if f.endswith(".npy"):
            base = os.path.splitext(f)[0]
            voxel_bases[base] = os.path.join("voxels", f)

    # Знаходимо перетин
    all_image_keys = set(image_bases.keys())
    all_voxel_keys = set(voxel_bases.keys())
    matched_keys = all_image_keys & all_voxel_keys
    orphan_images = all_image_keys - all_voxel_keys
    orphan_voxels = all_voxel_keys - all_image_keys

    stats = {
        "total_images": len(all_image_keys),
        "total_voxels": len(all_voxel_keys),
        "matched_pairs": len(matched_keys),
        "orphan_images": len(orphan_images),
        "orphan_voxels": len(orphan_voxels),
        "orphan_image_ids": sorted(list(orphan_images))[:20],
        "orphan_voxel_ids": sorted(list(orphan_voxels))[:20],
    }

    pairs = []
    for base in sorted(matched_keys):
        pairs.append({
            "id": base,
            "image_path": image_bases[base],
            "voxel_path": voxel_bases[base],
        })

    return pairs, stats


def validate_and_hash(pairs: list[dict], data_dir: str) -> tuple[list[dict], dict]:
    """
    Валідує voxel grids та обчислює hash для кожної пари.

    Фільтрує порожні, биті та невалідні семпли.

    Returns:
        (valid_pairs, validation_stats)
    """
    valid = []
    discarded_empty = 0
    discarded_broken = 0

    for pair in pairs:
        voxel_abs = os.path.join(data_dir, pair["voxel_path"])

        if not is_voxel_valid(voxel_abs):
            # Визначаємо причину
            try:
                data = np.load(voxel_abs)
                if data.sum() == 0:
                    discarded_empty += 1
                else:
                    discarded_broken += 1
            except Exception:
                discarded_broken += 1
            continue

        voxel_hash = compute_voxel_hash(voxel_abs)
        if voxel_hash is None:
            discarded_broken += 1
            continue

        pair["voxel_hash"] = voxel_hash
        valid.append(pair)

    stats = {
        "discarded_empty": discarded_empty,
        "discarded_broken": discarded_broken,
        "valid_pairs": len(valid),
    }

    return valid, stats


def assign_assets(pairs: list[dict]) -> list[dict]:
    """
    Групує семпли по voxel_hash і призначає asset_id.

    Семпли з однаковим voxel_hash — це різні view одного 3D target.
    """
    hash_to_asset = {}
    asset_counter = 0

    for pair in pairs:
        vh = pair["voxel_hash"]
        if vh not in hash_to_asset:
            hash_to_asset[vh] = f"asset_{asset_counter:04d}"
            asset_counter += 1
        pair["asset_id"] = hash_to_asset[vh]

    return pairs


def split_by_asset(pairs: list[dict], val_ratio: float = 0.15,
                   seed: int = 42) -> list[dict]:
    """
    Ділить семпли на train/val по asset_id (не по sample).

    Всі views одного asset потрапляють в один split,
    що виключає data leakage.
    """
    import random
    rng = random.Random(seed)

    # Збираємо унікальні asset_id
    asset_ids = sorted(set(p["asset_id"] for p in pairs))
    rng.shuffle(asset_ids)

    val_count = max(1, int(len(asset_ids) * val_ratio))
    val_assets = set(asset_ids[:val_count])

    for pair in pairs:
        pair["split"] = "val" if pair["asset_id"] in val_assets else "train"
        pair["enabled"] = True

    return pairs


def build_manifest(data_dir: str, val_ratio: float = 0.15,
                   dry_run: bool = False) -> dict:
    """
    Повний pipeline побудови manifest.

    Returns:
        manifest dict
    """
    print(f"📁 Сканування {data_dir}...")
    pairs, scan_stats = scan_pairs(data_dir)
    print(f"   Images: {scan_stats['total_images']}, "
          f"Voxels: {scan_stats['total_voxels']}, "
          f"Matched: {scan_stats['matched_pairs']}")
    print(f"   Orphan images: {scan_stats['orphan_images']}, "
          f"Orphan voxels: {scan_stats['orphan_voxels']}")

    if scan_stats['orphan_images'] > 0:
        print(f"   Orphan image IDs (перші 20): {scan_stats['orphan_image_ids']}")
    if scan_stats['orphan_voxels'] > 0:
        print(f"   Orphan voxel IDs (перші 20): {scan_stats['orphan_voxel_ids']}")

    print(f"\n🔍 Валідація та хешування...")
    pairs, val_stats = validate_and_hash(pairs, data_dir)
    print(f"   Valid: {val_stats['valid_pairs']}, "
          f"Discarded empty: {val_stats['discarded_empty']}, "
          f"Discarded broken: {val_stats['discarded_broken']}")

    print(f"\n🏷️  Призначення asset IDs...")
    pairs = assign_assets(pairs)
    unique_assets = len(set(p["asset_id"] for p in pairs))
    print(f"   Unique voxel assets: {unique_assets}")

    # Статистика дублікатів
    hash_counts = defaultdict(int)
    for p in pairs:
        hash_counts[p["voxel_hash"]] += 1
    duplicated_assets = sum(1 for c in hash_counts.values() if c > 1)
    max_views = max(hash_counts.values()) if hash_counts else 0
    print(f"   Assets з кількома views: {duplicated_assets}, "
          f"Max views per asset: {max_views}")

    print(f"\n✂️  Розподіл train/val (ratio={val_ratio})...")
    pairs = split_by_asset(pairs, val_ratio=val_ratio)
    train_count = sum(1 for p in pairs if p["split"] == "train")
    val_count = sum(1 for p in pairs if p["split"] == "val")
    train_assets = len(set(p["asset_id"] for p in pairs if p["split"] == "train"))
    val_assets = len(set(p["asset_id"] for p in pairs if p["split"] == "val"))
    print(f"   Train: {train_count} samples ({train_assets} assets)")
    print(f"   Val:   {val_count} samples ({val_assets} assets)")

    # Перевірка leakage
    train_hashes = set(p["voxel_hash"] for p in pairs if p["split"] == "train")
    val_hashes = set(p["voxel_hash"] for p in pairs if p["split"] == "val")
    leakage = train_hashes & val_hashes
    if leakage:
        print(f"   ⚠️  LEAKAGE DETECTED: {len(leakage)} shared hashes!")
    else:
        print(f"   ✅ No leakage between train/val splits")

    # Визначаємо формат даних
    rgba_count = sum(1 for p in pairs
                     if np.load(os.path.join(data_dir, p['voxel_path'])).shape == (64, 64, 64, 4))
    is_rgba = rgba_count > len(pairs) // 2
    resolution = 64 if is_rgba else 32
    channels = 4 if is_rgba else 1

    manifest = {
        "version": 2,
        "created_at": datetime.now(timezone.utc).isoformat(),
        "format": {
            "resolution": resolution,
            "channels": channels,
            "dtype": "uint8",
        },
        "stats": {
            "total_samples": len(pairs),
            "train_samples": train_count,
            "val_samples": val_count,
            "unique_assets": unique_assets,
            "discarded_orphans": scan_stats["orphan_images"]
                                 + scan_stats["orphan_voxels"],
            "discarded_empty": val_stats["discarded_empty"],
            "discarded_broken": val_stats["discarded_broken"],
        },
        "samples": pairs,
    }

    if dry_run:
        print(f"\n🔒 DRY RUN — manifest не збережено.")
    else:
        manifest_path = os.path.join(data_dir, "manifest.json")
        with open(manifest_path, "w", encoding="utf-8") as f:
            json.dump(manifest, f, indent=2, ensure_ascii=False)
        print(f"\n💾 Manifest збережено: {manifest_path}")

    return manifest


def main():
    parser = argparse.ArgumentParser(
        description="Побудова канонічного dataset manifest"
    )
    parser.add_argument(
        "--data-dir", "-d", default="data",
        help="Директорія з даними (містить images/ та voxels/)"
    )
    parser.add_argument(
        "--val-ratio", type=float, default=0.15,
        help="Частка validation split (default: 0.15)"
    )
    parser.add_argument(
        "--dry-run", action="store_true",
        help="Лише показати статистику, не зберігати manifest"
    )
    args = parser.parse_args()

    build_manifest(
        data_dir=args.data_dir,
        val_ratio=args.val_ratio,
        dry_run=args.dry_run,
    )


if __name__ == "__main__":
    main()
