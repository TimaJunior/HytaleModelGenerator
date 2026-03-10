"""
GLB до Dataset Конвертер

Конвертує 3D моделі у форматі GLB в тренувальні дані:
- Voxel grids (32x32x32) у .npy форматі
- Multi-view renders (8 ракурсів) у .png форматі
"""

import os
import sys
import numpy as np
import trimesh
from PIL import Image
import argparse
from pathlib import Path
import math

# Константи
VOXEL_RESOLUTION = 32
NUM_VIEWS = 8  # 4 сторони + 4 ізометричні
IMAGE_SIZE = (256, 256)


def load_glb(path: str) -> trimesh.Trimesh:
    """
    Завантажує GLB файл і повертає об'єднаний mesh.
    
    GLB може містити Scene з кількома об'єктами,
    тому ми об'єднуємо їх в один mesh.
    """
    scene_or_mesh = trimesh.load(path)
    
    if isinstance(scene_or_mesh, trimesh.Scene):
        # Об'єднуємо всі geometry в один mesh
        meshes = [g for g in scene_or_mesh.geometry.values() 
                  if isinstance(g, trimesh.Trimesh)]
        if not meshes:
            raise ValueError(f"No valid meshes found in {path}")
        mesh = trimesh.util.concatenate(meshes)
    else:
        mesh = scene_or_mesh
    
    return mesh


def normalize_mesh(mesh: trimesh.Trimesh) -> trimesh.Trimesh:
    """
    Нормалізує mesh щоб він вміщався в unit cube [-0.5, 0.5].
    Центрує в origin.
    """
    # Копіюємо vertices (trimesh може повертати read-only array)
    vertices = mesh.vertices.copy()
    
    # Центруємо
    centroid = vertices.mean(axis=0)
    vertices -= centroid
    
    # Масштабуємо до unit cube
    extents = vertices.max(axis=0) - vertices.min(axis=0)
    scale = 1.0 / max(extents) if max(extents) > 0 else 1.0
    vertices *= scale * 0.9  # 90% для запасу
    
    mesh.vertices = vertices
    return mesh


def voxelize(mesh: trimesh.Trimesh, resolution: int = VOXEL_RESOLUTION) -> np.ndarray:
    """
    Конвертує mesh у voxel grid.
    
    Returns:
        np.ndarray: Binary voxel grid shape (resolution, resolution, resolution)
    """
    # Voxelization через trimesh
    pitch = 1.0 / resolution  # Розмір одного voxel
    
    # Створюємо voxel representation
    voxels = mesh.voxelized(pitch=pitch)
    
    # Отримуємо dense matrix
    matrix = voxels.matrix.astype(np.uint8)
    
    # Центруємо в grid потрібного розміру
    result = np.zeros((resolution, resolution, resolution), dtype=np.uint8)
    
    # Знаходимо offset для центрування
    shape = matrix.shape
    offsets = [(resolution - s) // 2 for s in shape]
    
    # Копіюємо дані з обрізкою якщо потрібно
    for z in range(min(shape[0], resolution)):
        for y in range(min(shape[1], resolution)):
            for x in range(min(shape[2], resolution)):
                nz = z + offsets[0]
                ny = y + offsets[1]
                nx = x + offsets[2]
                if 0 <= nz < resolution and 0 <= ny < resolution and 0 <= nx < resolution:
                    result[nz, ny, nx] = matrix[z, y, x]
    
    return result


def simple_render(voxels: np.ndarray, angle_x: float = 0, angle_y: float = 0, 
                  output_size: tuple = IMAGE_SIZE) -> Image.Image:
    """
    Простий рендерер voxels з ізометричною проекцією.
    
    Використовує той самий підхід що і synthetic_data.py для консистентності.
    """
    from PIL import ImageDraw
    
    grid_size = voxels.shape[0]
    image = Image.new("RGB", output_size, "black")
    draw = ImageDraw.Draw(image)
    
    scale = output_size[0] // grid_size
    center = output_size[0] // 2
    
    # Простий z-buffer для depth sorting
    depth_buffer = []
    
    # Apply rotation (simple Y-axis rotation for now)
    cos_y = math.cos(math.radians(angle_y))
    sin_y = math.sin(math.radians(angle_y))
    cos_x = math.cos(math.radians(angle_x))
    sin_x = math.sin(math.radians(angle_x))
    
    for z in range(grid_size):
        for y in range(grid_size):
            for x in range(grid_size):
                if voxels[z, y, x] == 1:
                    # Центруємо координати
                    cx = x - grid_size // 2
                    cy = y - grid_size // 2
                    cz = z - grid_size // 2
                    
                    # Rotate around Y axis
                    rx = cx * cos_y - cz * sin_y
                    rz = cx * sin_y + cz * cos_y
                    
                    # Rotate around X axis
                    ry = cy * cos_x - rz * sin_x
                    final_z = cy * sin_x + rz * cos_x
                    
                    # Ізометрична проекція
                    screen_x = int(center + rx * scale)
                    screen_y = int(center - ry * scale)  # Flip Y
                    
                    # Depth-based color
                    depth = (final_z + grid_size) / (2 * grid_size)
                    shade = int(100 + depth * 155)
                    color = (int(shade * 0.5), shade, int(shade * 0.8))
                    
                    depth_buffer.append((final_z, screen_x, screen_y, scale, color))
    
    # Sort by depth (back to front)
    depth_buffer.sort(key=lambda x: x[0])
    
    # Draw
    for _, sx, sy, s, color in depth_buffer:
        draw.rectangle([sx, sy, sx + s - 1, sy + s - 1], fill=color)
    
    return image


def render_views(voxels: np.ndarray, num_views: int = NUM_VIEWS) -> list:
    """
    Рендерить модель з різних ракурсів.
    
    Returns:
        List of PIL.Image objects
    """
    views = []
    
    # Кути для 8 ракурсів
    angles = [
        (0, 0),      # Front
        (0, 90),     # Right
        (0, 180),    # Back
        (0, 270),    # Left
        (30, 45),    # Isometric 1
        (30, 135),   # Isometric 2
        (30, 225),   # Isometric 3
        (30, 315),   # Isometric 4
    ]
    
    for i, (ax, ay) in enumerate(angles[:num_views]):
        img = simple_render(voxels, angle_x=ax, angle_y=ay)
        views.append(img)
    
    return views


def process_glb(glb_path: str, output_dir: str, base_index: int = 500) -> int:
    """
    Обробляє один GLB файл і зберігає результати.
    
    Args:
        glb_path: Шлях до GLB файлу
        output_dir: Базова директорія для збереження (data/)
        base_index: Початковий індекс для нумерації (щоб не перезаписати synthetic)
        
    Returns:
        Кількість згенерованих samples
    """
    print(f"Processing: {glb_path}")
    
    # Створюємо директорії
    images_dir = os.path.join(output_dir, "images")
    voxels_dir = os.path.join(output_dir, "voxels")
    os.makedirs(images_dir, exist_ok=True)
    os.makedirs(voxels_dir, exist_ok=True)
    
    # Завантажуємо та обробляємо
    mesh = load_glb(glb_path)
    mesh = normalize_mesh(mesh)
    voxels = voxelize(mesh)
    
    print(f"  Voxel shape: {voxels.shape}, Active voxels: {np.sum(voxels)}")
    
    if np.sum(voxels) == 0:
        print(f"  Warning: No voxels generated, skipping...")
        return 0
    
    # Рендеримо views
    views = render_views(voxels)
    
    # Зберігаємо кожен view як окремий sample
    model_name = Path(glb_path).stem
    samples_created = 0
    
    for i, view in enumerate(views):
        sample_idx = base_index + i
        
        # Зберігаємо voxels (однакові для всіх views цієї моделі)
        voxel_path = os.path.join(voxels_dir, f"sample_{sample_idx}.npy")
        np.save(voxel_path, voxels)
        
        # Зберігаємо image
        image_path = os.path.join(images_dir, f"sample_{sample_idx}.png")
        view.save(image_path)
        
        samples_created += 1
        
    print(f"  Created {samples_created} samples (indices {base_index}-{base_index + samples_created - 1})")
    
    return samples_created


def main():
    parser = argparse.ArgumentParser(description="Convert GLB models to training data")
    parser.add_argument("--input", "-i", nargs="+", help="GLB file(s) to process")
    parser.add_argument("--input-dir", "-d", help="Directory with GLB files")
    parser.add_argument("--output", "-o", default="data", help="Output directory")
    parser.add_argument("--start-index", type=int, default=500, 
                        help="Starting index (to avoid overwriting synthetic data)")
    parser.add_argument("--test", action="store_true", help="Run self-test")
    
    args = parser.parse_args()
    
    if args.test:
        print("Running self-test...")
        print(f"  trimesh version: {trimesh.__version__}")
        # Створюємо тестовий mesh (конвертуємо primitive у звичайний mesh)
        primitive = trimesh.primitives.Sphere(radius=0.3)
        mesh = trimesh.Trimesh(vertices=primitive.vertices.copy(), 
                               faces=primitive.faces.copy())
        mesh = normalize_mesh(mesh)
        voxels = voxelize(mesh)
        print(f"  Test voxel shape: {voxels.shape}")
        print(f"  Active voxels: {np.sum(voxels)}")
        views = render_views(voxels, num_views=2)
        print(f"  Rendered {len(views)} views")
        print("Self-test passed!")
        return
    
    # Збираємо файли для обробки
    glb_files = []
    
    if args.input:
        glb_files.extend(args.input)
    
    if args.input_dir:
        dir_path = Path(args.input_dir)
        glb_files.extend([str(f) for f in dir_path.glob("*.glb")])
    
    if not glb_files:
        print("No GLB files specified. Use --input or --input-dir")
        sys.exit(1)
    
    print(f"Found {len(glb_files)} GLB file(s) to process")
    
    current_index = args.start_index
    total_samples = 0
    
    for glb_path in glb_files:
        try:
            samples = process_glb(glb_path, args.output, current_index)
            current_index += samples
            total_samples += samples
        except Exception as e:
            print(f"Error processing {glb_path}: {e}")
            continue
    
    print(f"\nTotal: {total_samples} samples created")


if __name__ == "__main__":
    main()
