"""
OBJ/GLB → RGBA Voxel Grid Converter.

Конвертує 3D mesh файли (OBJ, GLB, STL) у RGBA voxel grid.
Вихід: numpy array shape (grid_size, grid_size, grid_size, 4) — RGBA uint8.
"""

import trimesh
import numpy as np
import os
import sys

GRID_SIZE = 64
PADDING = 2


def _sample_vertex_colors(mesh):
    """Витягує середній колір з vertex colors або face colors."""
    if hasattr(mesh.visual, 'vertex_colors') and mesh.visual.vertex_colors is not None:
        vc = np.array(mesh.visual.vertex_colors)
        if vc.ndim == 2 and vc.shape[1] >= 3:
            return vc[:, :3]  # (N_verts, 3) RGB

    if hasattr(mesh.visual, 'face_colors') and mesh.visual.face_colors is not None:
        fc = np.array(mesh.visual.face_colors)
        if fc.ndim == 2 and fc.shape[1] >= 3:
            return fc[:, :3]

    # Спробувати отримати колір з текстури (якщо є UV mapping)
    try:
        if hasattr(mesh.visual, 'material') and mesh.visual.material is not None:
            mat = mesh.visual.material
            if hasattr(mat, 'baseColorFactor'):
                c = np.array(mat.baseColorFactor[:3]) * 255
                return np.tile(c.astype(np.uint8), (len(mesh.vertices), 1))
            if hasattr(mat, 'diffuse'):
                c = np.array(mat.diffuse[:3])
                if c.max() <= 1.0:
                    c = c * 255
                return np.tile(c.astype(np.uint8), (len(mesh.vertices), 1))
    except Exception:
        pass

    return None


def _assign_colors_to_voxels(mesh, voxel_obj, grid, grid_size):
    """Призначає кольори вокселям на основі найближчих вершин."""
    vertex_colors = _sample_vertex_colors(mesh)

    if vertex_colors is None:
        # Фоллбек: випадковий однотонний колір у стилі Hytale
        hytale_colors = [
            [16, 185, 129],   # зелений
            [59, 130, 246],   # синій
            [239, 68, 68],    # червоний
            [245, 158, 11],   # оранж
            [139, 92, 246],   # фіолетовий
            [30, 41, 59],     # темно-сірий
            [148, 163, 184],  # світло-сірий
        ]
        color = hytale_colors[hash(str(mesh.bounds)) % len(hytale_colors)]
        occupied = grid[:, :, :, 3] > 0
        grid[occupied, 0] = color[0]
        grid[occupied, 1] = color[1]
        grid[occupied, 2] = color[2]
        return grid

    # Отримуємо координати зайнятих вокселів
    occupied_coords = np.argwhere(grid[:, :, :, 3] > 0)
    if len(occupied_coords) == 0:
        return grid

    # Для кожного вокселя знаходимо найближчу вершину меша
    # і присвоюємо її колір
    vertices = np.array(mesh.vertices)

    for coord in occupied_coords:
        # Центр вокселя у просторі меша
        voxel_center = coord.astype(float) + 0.5

        # Знайти найближчу вершину
        dists = np.linalg.norm(vertices - voxel_center, axis=1)
        nearest_idx = np.argmin(dists)

        grid[coord[0], coord[1], coord[2], 0] = vertex_colors[nearest_idx][0]
        grid[coord[0], coord[1], coord[2], 1] = vertex_colors[nearest_idx][1]
        grid[coord[0], coord[1], coord[2], 2] = vertex_colors[nearest_idx][2]

    return grid


def obj_to_voxel(obj_path, grid_size=GRID_SIZE, padding=PADDING):
    """
    Конвертує OBJ/GLB/STL → RGBA voxel grid.

    Returns:
        np.ndarray shape (grid_size, grid_size, grid_size, 4) dtype uint8
        Канали: R, G, B, Alpha (0=порожній, 255=зайнятий)
    """
    try:
        mesh = trimesh.load(obj_path)

        # Якщо це сцена — об'єднати все в один меш
        if isinstance(mesh, trimesh.Scene):
            if len(mesh.geometry) == 0:
                return None
            meshes = [g for g in mesh.geometry.values()
                      if isinstance(g, trimesh.Trimesh)]
            if not meshes:
                return None
            mesh = trimesh.util.concatenate(meshes)

        if not isinstance(mesh, trimesh.Trimesh):
            print(f"Error: Loaded object is not a mesh: {type(mesh)}")
            return None

        # Масштабування в grid з padding
        bounds = mesh.bounds
        size = bounds[1] - bounds[0]
        max_dim = size.max()

        effective_grid = grid_size - 2 * padding
        scale = effective_grid / max_dim if max_dim > 0 else 1

        mesh.apply_translation(-bounds[0])
        mesh.apply_scale(scale)
        mesh.apply_translation([padding, padding, padding])

        # Вокселізація
        voxel_obj = mesh.voxelized(pitch=1.0)
        binary_matrix = voxel_obj.matrix

        # Створюємо RGBA grid
        grid = np.zeros((grid_size, grid_size, grid_size, 4), dtype=np.uint8)

        # Заповнюємо Alpha канал
        s = binary_matrix.shape
        sx = min(s[0], grid_size)
        sy = min(s[1], grid_size)
        sz = min(s[2], grid_size)
        grid[:sx, :sy, :sz, 3] = binary_matrix[:sx, :sy, :sz].astype(np.uint8) * 255

        # Призначаємо кольори
        grid = _assign_colors_to_voxels(mesh, voxel_obj, grid, grid_size)

        return grid

    except Exception as e:
        print(f"Error converting {obj_path}: {e}")
        return None


if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: python obj_to_voxel.py <input.obj> <output.npy>")
        sys.exit(1)

    input_path = sys.argv[1]
    output_path = sys.argv[2]

    grid = obj_to_voxel(input_path)
    if grid is not None:
        os.makedirs(os.path.dirname(output_path) or ".", exist_ok=True)
        np.save(output_path, grid)
        occupied = (grid[:, :, :, 3] > 0).sum()
        print(f"Converted {input_path} → {output_path} ({occupied} voxels, shape {grid.shape})")
