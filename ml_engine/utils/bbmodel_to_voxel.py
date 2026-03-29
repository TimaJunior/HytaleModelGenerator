"""
Blockbench (.bbmodel) → RGBA Voxel Grid Converter.

Конвертує Blockbench моделі у RGBA voxel grid.
Вихід: numpy array shape (grid_size, grid_size, grid_size, 4) — RGBA uint8.
"""

import json
import numpy as np
import os
import sys
import re

GRID_SIZE = 64
PADDING = 2


def _parse_hex_color(hex_str):
    """Парсить HEX колір (#RRGGBB або #RGB) → [R, G, B]."""
    if not hex_str or not isinstance(hex_str, str):
        return None
    hex_str = hex_str.lstrip('#')
    if len(hex_str) == 3:
        hex_str = ''.join(c * 2 for c in hex_str)
    if len(hex_str) != 6:
        return None
    try:
        r = int(hex_str[0:2], 16)
        g = int(hex_str[2:4], 16)
        b = int(hex_str[4:6], 16)
        return [r, g, b]
    except ValueError:
        return None


def _extract_element_color(element, textures=None):
    """Витягує колір елемента з faces, color атрибуту, або текстури."""
    # 1. Прямий color атрибут (деякі bbmodel мають)
    if 'color' in element:
        c = _parse_hex_color(str(element['color']))
        if c:
            return c

    # 2. З faces — перший face з color
    faces = element.get('faces', {})
    for face_name, face_data in faces.items():
        if isinstance(face_data, dict):
            if 'color' in face_data:
                c = _parse_hex_color(str(face_data['color']))
                if c:
                    return c
            # texture reference → спробувати знайти колір текстури
            if textures and 'texture' in face_data:
                tex_ref = face_data['texture']
                if isinstance(tex_ref, int) and tex_ref < len(textures):
                    tex = textures[tex_ref]
                    if isinstance(tex, dict) and 'color' in tex:
                        c = _parse_hex_color(str(tex['color']))
                        if c:
                            return c

    return None


def bbmodel_to_voxel(bbmodel_path, grid_size=GRID_SIZE, padding=PADDING):
    """
    Конвертує Blockbench .bbmodel → RGBA voxel grid.

    Returns:
        np.ndarray shape (grid_size, grid_size, grid_size, 4) dtype uint8
        Канали: R, G, B, Alpha (0=порожній, 255=зайнятий)
    """
    try:
        with open(bbmodel_path, 'r', encoding='utf-8') as f:
            data = json.load(f)
    except Exception as e:
        print(f"Error reading {bbmodel_path}: {e}")
        return None

    elements = data.get('elements', [])
    if not elements:
        print(f"No elements found in {bbmodel_path}")
        return None

    textures = data.get('textures', [])

    # Збираємо bounding box
    all_points = []
    for el in elements:
        if 'from' in el and 'to' in el:
            all_points.append(el['from'])
            all_points.append(el['to'])

    if not all_points:
        return None

    all_points = np.array(all_points)
    min_coords = all_points.min(axis=0)
    max_coords = all_points.max(axis=0)

    size = max_coords - min_coords
    max_dim = size.max()

    # RGBA grid
    grid = np.zeros((grid_size, grid_size, grid_size, 4), dtype=np.uint8)

    # Scale factor
    effective_grid = grid_size - 2 * padding
    scale = effective_grid / max_dim if max_dim > 0 else 1

    # Фоллбекні кольори у стилі Hytale
    hytale_palette = [
        [16, 185, 129],   # зелений (emerald)
        [30, 41, 59],     # темний (slate)
        [59, 130, 246],   # синій
        [239, 68, 68],    # червоний
        [245, 158, 11],   # оранж
        [139, 92, 246],   # фіолетовий
        [148, 163, 184],  # сірий
        [251, 191, 36],   # жовтий
    ]

    for idx, el in enumerate(elements):
        if 'from' not in el or 'to' not in el:
            continue

        # Scale coordinates
        start = (np.array(el['from']) - min_coords) * scale + padding
        end = (np.array(el['to']) - min_coords) * scale + padding

        s_idx = np.floor(np.minimum(start, end)).astype(int)
        e_idx = np.ceil(np.maximum(start, end)).astype(int)

        s_idx = np.clip(s_idx, 0, grid_size - 1)
        e_idx = np.clip(e_idx, 0, grid_size)

        # Витягуємо колір елемента
        color = _extract_element_color(el, textures)
        if color is None:
            color = hytale_palette[idx % len(hytale_palette)]

        # Заповнюємо RGBA
        grid[s_idx[0]:e_idx[0], s_idx[1]:e_idx[1], s_idx[2]:e_idx[2], 0] = color[0]
        grid[s_idx[0]:e_idx[0], s_idx[1]:e_idx[1], s_idx[2]:e_idx[2], 1] = color[1]
        grid[s_idx[0]:e_idx[0], s_idx[1]:e_idx[1], s_idx[2]:e_idx[2], 2] = color[2]
        grid[s_idx[0]:e_idx[0], s_idx[1]:e_idx[1], s_idx[2]:e_idx[2], 3] = 255

    return grid


if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: python bbmodel_to_voxel.py <input.bbmodel> <output.npy>")
        sys.exit(1)

    input_path = sys.argv[1]
    output_path = sys.argv[2]

    grid = bbmodel_to_voxel(input_path)
    if grid is not None:
        os.makedirs(os.path.dirname(output_path) or ".", exist_ok=True)
        np.save(output_path, grid)
        occupied = (grid[:, :, :, 3] > 0).sum()
        print(f"Converted {input_path} → {output_path} ({occupied} voxels, shape {grid.shape})")
