"""
Colored RGBA Voxel Data Processor.

Конвертує 3D моделі (.obj, .bbmodel) у RGBA voxel грід (64³)
та рендерить 8 кольорових зображень-видів для тренування.
"""

import os
import sys
import numpy as np
from PIL import Image, ImageDraw
import math
from pathlib import Path

sys.path.append(os.path.dirname(os.path.abspath(__file__)))
from obj_to_voxel import obj_to_voxel
from bbmodel_to_voxel import bbmodel_to_voxel

VOXEL_RESOLUTION = 64
NUM_VIEWS = 8
IMAGE_SIZE = (256, 256)


def render_rgba_voxels(voxels_rgba: np.ndarray, angle_x: float = 0, angle_y: float = 0,
                       output_size: tuple = IMAGE_SIZE) -> Image.Image:
    """
    Рендерить RGBA voxel grid у кольорове зображення.
    voxels_rgba shape: (D, H, W, 4) — R,G,B,A
    """
    grid_size = voxels_rgba.shape[0]
    image = Image.new("RGBA", output_size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)

    scale = output_size[0] / grid_size
    center = output_size[0] // 2

    cos_y = math.cos(math.radians(angle_y))
    sin_y = math.sin(math.radians(angle_y))
    cos_x = math.cos(math.radians(angle_x))
    sin_x = math.sin(math.radians(angle_x))

    depth_buffer = []

    for z in range(grid_size):
        for y in range(grid_size):
            for x in range(grid_size):
                if voxels_rgba[x, y, z, 3] < 128:
                    continue

                r = int(voxels_rgba[x, y, z, 0])
                g = int(voxels_rgba[x, y, z, 1])
                b = int(voxels_rgba[x, y, z, 2])

                cx_v = x - grid_size // 2
                cy_v = y - grid_size // 2
                cz_v = z - grid_size // 2

                # Y-axis rotation
                rx = cx_v * cos_y - cz_v * sin_y
                rz = cx_v * sin_y + cz_v * cos_y

                # X-axis rotation
                ry = cy_v * cos_x - rz * sin_x
                final_z = cy_v * sin_x + rz * cos_x

                screen_x = int(center + rx * scale)
                screen_y = int(center - ry * scale)

                # Shading: depth-based lighting
                depth = (final_z + grid_size) / (2 * grid_size)
                shade = 0.5 + depth * 0.5  # [0.5, 1.0]

                r_s = int(r * shade)
                g_s = int(g * shade)
                b_s = int(b * shade)

                depth_buffer.append((final_z, screen_x, screen_y, scale, (r_s, g_s, b_s, 255)))

    depth_buffer.sort(key=lambda v: v[0])

    for _, sx, sy, s, color in depth_buffer:
        s_px = max(1, int(s))
        draw.rectangle([sx, sy, sx + s_px - 1, sy + s_px - 1], fill=color)

    return image.convert("RGB")


def render_views(voxels_rgba: np.ndarray):
    """Рендерить 8 видів моделі."""
    angles = [
        (0, 0), (0, 90), (0, 180), (0, 270),
        (30, 45), (30, 135), (30, 225), (30, 315)
    ]
    return [render_rgba_voxels(voxels_rgba, ax, ay) for ax, ay in angles]


def process_file(file_path, output_dir, start_index):
    """Конвертує один файл і зберігає пари (image, voxel)."""
    file_path = Path(file_path)
    print(f"  Processing: {file_path.name}")

    voxels = None
    if file_path.suffix.lower() == '.obj':
        voxels = obj_to_voxel(str(file_path))
    elif file_path.suffix.lower() == '.bbmodel':
        voxels = bbmodel_to_voxel(str(file_path))

    if voxels is None or voxels[:, :, :, 3].sum() == 0:
        print(f"    Warning: No voxels for {file_path.name}")
        return 0

    views = render_views(voxels)

    images_dir = os.path.join(output_dir, "images")
    voxels_dir = os.path.join(output_dir, "voxels")
    os.makedirs(images_dir, exist_ok=True)
    os.makedirs(voxels_dir, exist_ok=True)

    stem = file_path.stem[:40].replace(" ", "_")

    for i, view in enumerate(views):
        idx = start_index + i
        voxel_filename = f"{stem}_v{i}.npy"
        image_filename = f"{stem}_v{i}.png"
        np.save(os.path.join(voxels_dir, voxel_filename), voxels)
        view.save(os.path.join(images_dir, image_filename))

    return len(views)


def process_synthetic_voxels(output_dir, synth_voxels_dir):
    """
    Для синтетичних вокселів (вже готових .npy RGBA) рендерить зображення
    та зберігає пари.
    """
    images_dir = os.path.join(output_dir, "images")
    voxels_dir = os.path.join(output_dir, "voxels")
    os.makedirs(images_dir, exist_ok=True)
    os.makedirs(voxels_dir, exist_ok=True)

    npy_files = list(Path(synth_voxels_dir).glob("synth_*.npy"))
    print(f"Processing {len(npy_files)} synthetic voxel files...")

    count = 0
    for npy_path in npy_files:
        voxels = np.load(str(npy_path))
        if voxels.shape != (64, 64, 64, 4):
            continue

        stem = npy_path.stem
        views = render_views(voxels)
        for i, view in enumerate(views):
            voxel_fn = f"{stem}_v{i}.npy"
            image_fn = f"{stem}_v{i}.png"
            np.save(os.path.join(voxels_dir, voxel_fn), voxels)
            view.save(os.path.join(images_dir, image_fn))
            count += 1

    print(f"  Rendered {count} synthetic pairs.")
    return count


def main():
    data_root = Path("c:/Users/Tima/Documents/Projects/hytalemodelgenerator")
    output_dir = data_root / "data"

    # 1. Спочатку генеруємо синтетичні моделі
    print("=" * 40)
    print("Step 1: Generating synthetic RGBA voxels...")
    from generate_synthetic import generate_dataset
    generate_dataset(str(output_dir), count_per_type=80)

    # Рендеримо синтетичні
    synth_voxels = str(output_dir / "voxels")
    process_synthetic_voxels(str(output_dir), synth_voxels)

    # 2. Конвертуємо реальні 3D моделі
    print("\nStep 2: Converting real 3D models...")
    model_files = []
    search_dirs = [data_root / "data/raw_models", data_root / "dataset"]
    for s_dir in search_dirs:
        if not s_dir.exists():
            continue
        model_files.extend(s_dir.glob("**/*.obj"))
        model_files.extend(s_dir.glob("**/*.bbmodel"))

    unique_files = {f.name: f for f in model_files}
    print(f"  Found {len(unique_files)} unique real models.")

    total = 0
    current_index = 0
    for name, f_path in unique_files.items():
        try:
            count = process_file(f_path, str(output_dir), current_index)
            current_index += count
            total += count
        except Exception as e:
            print(f"  Error: {f_path.name}: {e}")

    print(f"\n✅ Done! Total pairs: {total}")
    print("Now run: python -m ml_engine.utils.build_manifest")


if __name__ == "__main__":
    main()
