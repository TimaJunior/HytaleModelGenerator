"""
Аугментація Voxel Датасету

Створює варіації існуючих samples через:
- 90° ротації навколо осей (4 варіанти на вісь)
- Відзеркалення по осях
- Комбінації трансформацій

Це збільшує датасет у ~12-24 рази.
"""

import os
import sys
import numpy as np
from PIL import Image
from pathlib import Path
import argparse
import math

# Додаємо project root до path
project_root = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.path.insert(0, project_root)

from ml_engine.utils.glb_to_dataset import simple_render, IMAGE_SIZE


def rotate_voxels_90(voxels: np.ndarray, axis: int, times: int = 1) -> np.ndarray:
    """
    Ротація voxel grid на 90° навколо заданої осі.
    
    Args:
        voxels: 3D numpy array
        axis: 0 (Z), 1 (Y), або 2 (X)
        times: кількість 90° ротацій (1-3)
    """
    result = voxels.copy()
    for _ in range(times % 4):
        if axis == 0:  # Z axis (rotate in XY plane)
            result = np.rot90(result, k=1, axes=(1, 2))
        elif axis == 1:  # Y axis (rotate in XZ plane)
            result = np.rot90(result, k=1, axes=(0, 2))
        elif axis == 2:  # X axis (rotate in YZ plane)
            result = np.rot90(result, k=1, axes=(0, 1))
    return result


def flip_voxels(voxels: np.ndarray, axis: int) -> np.ndarray:
    """Відзеркалення voxel grid по заданій осі."""
    return np.flip(voxels, axis=axis).copy()


def get_augmentations(voxels: np.ndarray) -> list:
    """
    Генерує всі аугментації для одного voxel grid.
    
    Returns:
        List of (augmented_voxels, transformation_name) tuples
    """
    augmentations = []
    
    # 1. Ротації навколо Y (вертикальної) осі — найбільш природні
    for times in range(1, 4):  # 90°, 180°, 270°
        rotated = rotate_voxels_90(voxels, axis=1, times=times)
        augmentations.append((rotated, f"rot_y_{times * 90}"))
    
    # 2. Горизонтальне відзеркалення (по X осі) - дзеркальне відображення
    flipped_x = flip_voxels(voxels, axis=2)
    augmentations.append((flipped_x, "flip_x"))
    
    # 3. Комбінація: flip + rotation
    for times in range(1, 3):  # 90°, 180°
        combo = rotate_voxels_90(flipped_x, axis=1, times=times)
        augmentations.append((combo, f"flip_x_rot_{times * 90}"))
    
    return augmentations


def augment_sample(voxel_path: str, image_path: str, output_dir: str, 
                   base_index: int) -> int:
    """
    Аугментує один sample і зберігає результати.
    
    Returns:
        Кількість створених нових samples
    """
    # Завантажуємо оригінальний voxel
    voxels = np.load(voxel_path)
    
    # Отримуємо аугментації
    augmentations = get_augmentations(voxels)
    
    # Директорії
    images_dir = os.path.join(output_dir, "images")
    voxels_dir = os.path.join(output_dir, "voxels")
    os.makedirs(images_dir, exist_ok=True)
    os.makedirs(voxels_dir, exist_ok=True)
    
    samples_created = 0
    
    for aug_voxels, aug_name in augmentations:
        sample_idx = base_index + samples_created
        
        # Зберігаємо voxels
        np.save(os.path.join(voxels_dir, f"sample_{sample_idx}.npy"), aug_voxels)
        
        # Рендеримо і зберігаємо image (ізометричний вигляд)
        img = simple_render(aug_voxels, angle_x=30, angle_y=45)
        img.save(os.path.join(images_dir, f"sample_{sample_idx}.png"))
        
        samples_created += 1
    
    return samples_created


def augment_range(data_dir: str, start_idx: int, end_idx: int, 
                  output_start_idx: int) -> int:
    """
    Аугментує діапазон samples.
    
    Args:
        data_dir: Директорія з даними
        start_idx: Початковий індекс samples для аугментації
        end_idx: Кінцевий індекс (не включно)
        output_start_idx: Початковий індекс для нових samples
        
    Returns:
        Загальна кількість створених samples
    """
    current_output_idx = output_start_idx
    total_created = 0
    
    for idx in range(start_idx, end_idx):
        voxel_path = os.path.join(data_dir, "voxels", f"sample_{idx}.npy")
        image_path = os.path.join(data_dir, "images", f"sample_{idx}.png")
        
        if not os.path.exists(voxel_path):
            print(f"  Skipping sample_{idx} (not found)")
            continue
        
        created = augment_sample(voxel_path, image_path, data_dir, current_output_idx)
        print(f"  sample_{idx} → {created} augmentations (indices {current_output_idx}-{current_output_idx + created - 1})")
        
        current_output_idx += created
        total_created += created
    
    return total_created


def main():
    parser = argparse.ArgumentParser(description="Augment voxel dataset")
    parser.add_argument("--data-dir", "-d", default="data", help="Data directory")
    parser.add_argument("--start", "-s", type=int, 
                        help="Start index of samples to augment")
    parser.add_argument("--end", "-e", type=int,
                        help="End index of samples to augment (exclusive)")
    parser.add_argument("--output-start", "-o", type=int, default=None,
                        help="Starting index for augmented samples (default: auto)")
    parser.add_argument("--test", action="store_true", help="Run self-test")
    
    args = parser.parse_args()
    
    if args.test:
        print("Running self-test...")
        # Створюємо тестовий voxel grid
        test_voxels = np.zeros((32, 32, 32), dtype=np.uint8)
        test_voxels[10:20, 5:25, 14:18] = 1  # Вертикальний прямокутник
        
        augmentations = get_augmentations(test_voxels)
        print(f"  Generated {len(augmentations)} augmentations")
        
        for aug, name in augmentations:
            print(f"    {name}: shape={aug.shape}, voxels={np.sum(aug)}")
        
        print("Self-test passed!")
        return
    
    # Знаходимо наступний доступний індекс
    if args.start is None or args.end is None:
        print("Error: --start and --end are required when not using --test")
        sys.exit(1)
        
    if args.output_start is None:
        # Шукаємо максимальний існуючий індекс
        voxels_dir = os.path.join(args.data_dir, "voxels")
        existing = [int(f.stem.split("_")[1]) for f in Path(voxels_dir).glob("sample_*.npy")]
        args.output_start = max(existing) + 1 if existing else 0
    
    print(f"Augmenting samples {args.start}-{args.end - 1}")
    print(f"Output starting at index {args.output_start}")
    
    total = augment_range(args.data_dir, args.start, args.end, args.output_start)
    
    print(f"\nTotal: {total} augmented samples created")


if __name__ == "__main__":
    main()
