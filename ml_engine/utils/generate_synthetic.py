"""
Procedural Synthetic RGBA Voxel Model Generator.

Генерує процедурні 3D моделі (мечі, сокири, дерева, блоки, посохи)
з кольором у форматі RGBA (64, 64, 64, 4).
Це дозволяє швидко збільшити датасет без ручної роботи.
"""

import numpy as np
import random
import os

GRID_SIZE = 64


def _fill_box(grid, x1, y1, z1, x2, y2, z2, color):
    """Заповнити прямокутний паралелепіпед кольором."""
    x1, x2 = max(0, min(x1, x2)), min(GRID_SIZE, max(x1, x2))
    y1, y2 = max(0, min(y1, y2)), min(GRID_SIZE, max(y1, y2))
    z1, z2 = max(0, min(z1, z2)), min(GRID_SIZE, max(z1, z2))
    grid[x1:x2, y1:y2, z1:z2, 0] = color[0]
    grid[x1:x2, y1:y2, z1:z2, 1] = color[1]
    grid[x1:x2, y1:y2, z1:z2, 2] = color[2]
    grid[x1:x2, y1:y2, z1:z2, 3] = 255


def _fill_sphere(grid, cx, cy, cz, radius, color):
    """Заповнити сферу кольором."""
    for x in range(max(0, cx - radius), min(GRID_SIZE, cx + radius + 1)):
        for y in range(max(0, cy - radius), min(GRID_SIZE, cy + radius + 1)):
            for z in range(max(0, cz - radius), min(GRID_SIZE, cz + radius + 1)):
                if (x - cx)**2 + (y - cy)**2 + (z - cz)**2 <= radius**2:
                    grid[x, y, z, 0] = color[0]
                    grid[x, y, z, 1] = color[1]
                    grid[x, y, z, 2] = color[2]
                    grid[x, y, z, 3] = 255


def _fill_cylinder(grid, cx, cz, y_start, y_end, radius, color):
    """Заповнити вертикальний циліндр."""
    for x in range(max(0, cx - radius), min(GRID_SIZE, cx + radius + 1)):
        for z in range(max(0, cz - radius), min(GRID_SIZE, cz + radius + 1)):
            if (x - cx)**2 + (z - cz)**2 <= radius**2:
                y1 = max(0, min(y_start, y_end))
                y2 = min(GRID_SIZE, max(y_start, y_end))
                grid[x, y1:y2, z, 0] = color[0]
                grid[x, y1:y2, z, 1] = color[1]
                grid[x, y1:y2, z, 2] = color[2]
                grid[x, y1:y2, z, 3] = 255


# ============ ГЕНЕРАТОРИ МОДЕЛЕЙ ============

def generate_sword():
    """Генерує процедурний меч з руків'ям і лезом."""
    grid = np.zeros((GRID_SIZE, GRID_SIZE, GRID_SIZE, 4), dtype=np.uint8)
    cx, cz = 32, 32

    # Варіації кольорів
    blade_colors = [
        [192, 203, 215],  # сріблястий
        [147, 197, 253],  # блакитний
        [252, 211, 77],   # золотий
        [248, 113, 113],  # рожевий
    ]
    handle_colors = [
        [30, 41, 59],     # темний
        [120, 53, 15],    # коричневий
        [55, 48, 163],    # індиго
    ]
    guard_colors = [
        [251, 191, 36],   # золотий
        [192, 203, 215],  # сріблястий
        [16, 185, 129],   # зелений
    ]

    blade_c = random.choice(blade_colors)
    handle_c = random.choice(handle_colors)
    guard_c = random.choice(guard_colors)

    blade_len = random.randint(22, 35)
    blade_w = random.randint(1, 3)

    # Руків'я (внизу)
    _fill_box(grid, cx - 1, 4, cz - 1, cx + 1, 18, cz + 1, handle_c)

    # Гарда
    guard_w = random.randint(3, 6)
    _fill_box(grid, cx - guard_w, 17, cz - 1, cx + guard_w, 20, cz + 1, guard_c)

    # Лезо
    _fill_box(grid, cx - blade_w, 19, cz, cx + blade_w, 19 + blade_len, cz + 1, blade_c)

    # Вістря (трикутне)
    tip_y = 19 + blade_len
    for i in range(blade_w):
        _fill_box(grid, cx - blade_w + i + 1, tip_y + i, cz, cx + blade_w - i, tip_y + i + 1, cz + 1, blade_c)

    return grid


def generate_axe():
    """Генерує процедурну сокиру."""
    grid = np.zeros((GRID_SIZE, GRID_SIZE, GRID_SIZE, 4), dtype=np.uint8)
    cx, cz = 32, 32

    handle_c = [random.randint(80, 140), random.randint(40, 80), random.randint(10, 40)]
    head_c = [random.randint(150, 220), random.randint(170, 230), random.randint(180, 240)]

    handle_len = random.randint(25, 38)

    # Ручка
    _fill_box(grid, cx - 1, 4, cz - 1, cx + 1, 4 + handle_len, cz + 1, handle_c)

    # Головка сокири
    head_y = 4 + handle_len - 6
    head_h = random.randint(8, 14)
    head_d = random.randint(5, 8)
    _fill_box(grid, cx + 1, head_y, cz - head_d // 2, cx + 1 + head_h, head_y + 8, cz + head_d // 2, head_c)

    return grid


def generate_staff():
    """Генерує процедурний магічний посох (схожий на референс)."""
    grid = np.zeros((GRID_SIZE, GRID_SIZE, GRID_SIZE, 4), dtype=np.uint8)
    cx, cz = 32, 32

    staff_c = [30, 41, 59]  # темний
    crystal_colors = [
        [16, 185, 129],   # зелений
        [59, 130, 246],   # синій
        [239, 68, 68],    # червоний
        [168, 85, 247],   # фіолетовий
        [251, 191, 36],   # жовтий
    ]
    crystal_c = random.choice(crystal_colors)
    rune_c = [c // 2 for c in crystal_c]

    # Ручка (довга, тонка)
    _fill_box(grid, cx - 1, 4, cz - 1, cx + 1, 42, cz + 1, staff_c)

    # Руни на ручці (невеликі декоративні елементи)
    for ry in range(8, 25, 5):
        _fill_box(grid, cx - 2, ry, cz - 1, cx - 1, ry + 2, cz + 1, rune_c)
        _fill_box(grid, cx + 1, ry, cz - 1, cx + 2, ry + 2, cz + 1, rune_c)

    # Навершя (більш складна конструкція)
    # Розширення до навершя
    _fill_box(grid, cx - 2, 40, cz - 2, cx + 2, 44, cz + 2, staff_c)

    # Бічні «роги» навершя
    _fill_box(grid, cx - 4, 44, cz - 1, cx - 2, 50, cz + 1, staff_c)
    _fill_box(grid, cx + 2, 44, cz - 1, cx + 4, 50, cz + 1, staff_c)

    # Кристал (сфера в центрі навершя)
    _fill_sphere(grid, cx, 48, cz, random.randint(3, 5), crystal_c)

    # Світіння навколо кристала (менш яскравий відтінок)
    glow_c = [min(255, c + 60) for c in crystal_c]
    _fill_sphere(grid, cx, 48, cz, 6, glow_c)
    _fill_sphere(grid, cx, 48, cz, random.randint(3, 5), crystal_c)

    return grid


def generate_tree():
    """Генерує процедурне дерево."""
    grid = np.zeros((GRID_SIZE, GRID_SIZE, GRID_SIZE, 4), dtype=np.uint8)
    cx, cz = 32, 32

    trunk_c = [random.randint(100, 150), random.randint(60, 90), random.randint(20, 50)]
    leaf_c = [random.randint(20, 80), random.randint(140, 220), random.randint(30, 100)]

    # Стовбур
    trunk_h = random.randint(18, 30)
    _fill_cylinder(grid, cx, cz, 4, 4 + trunk_h, random.randint(2, 4), trunk_c)

    # Крона (кілька перекритих сфер)
    canopy_y = 4 + trunk_h
    canopy_r = random.randint(8, 14)
    _fill_sphere(grid, cx, canopy_y, cz, canopy_r, leaf_c)

    # Додаткові «кетки» для натуральності
    for _ in range(random.randint(2, 4)):
        dx = random.randint(-5, 5)
        dy = random.randint(-3, 5)
        dz = random.randint(-5, 5)
        r = random.randint(4, 8)
        _fill_sphere(grid, cx + dx, canopy_y + dy, cz + dz, r, leaf_c)

    return grid


def generate_block():
    """Генерує простий декоративний блок / скриню."""
    grid = np.zeros((GRID_SIZE, GRID_SIZE, GRID_SIZE, 4), dtype=np.uint8)

    body_colors = [
        [120, 53, 15],    # коричневий (скриня)
        [148, 163, 184],  # сірий (камінь)
        [59, 130, 246],   # синій (магічний)
    ]
    accent_colors = [
        [251, 191, 36],   # золотий
        [16, 185, 129],   # зелений
        [239, 68, 68],    # червоний
    ]

    body_c = random.choice(body_colors)
    accent_c = random.choice(accent_colors)

    size = random.randint(12, 22)
    offset = (GRID_SIZE - size) // 2

    # Основне тіло
    _fill_box(grid, offset, offset, offset, offset + size, offset + size, offset + size, body_c)

    # Декоративна смуга
    stripe_w = 2
    mid = offset + size // 2
    _fill_box(grid, offset, mid - stripe_w, offset - 1, offset + size, mid + stripe_w, offset + size + 1, accent_c)

    # Замочок / деталь
    _fill_box(grid, mid - 1, mid - 2, offset - 1, mid + 1, mid + 2, offset, accent_c)

    return grid


def generate_shield():
    """Генерує процедурний щит."""
    grid = np.zeros((GRID_SIZE, GRID_SIZE, GRID_SIZE, 4), dtype=np.uint8)
    cx, cz = 32, 32

    frame_c = [random.randint(140, 200), random.randint(150, 210), random.randint(160, 220)]
    fill_c = [random.randint(20, 80), random.randint(20, 80), random.randint(100, 200)]
    emblem_c = [251, 191, 36]

    h = random.randint(18, 28)
    w = random.randint(14, 22)
    half_h = h // 2
    half_w = w // 2

    # Рамка
    _fill_box(grid, cx - half_w, 32 - half_h, cz, cx + half_w, 32 + half_h, cz + 3, frame_c)
    # Заповнення
    _fill_box(grid, cx - half_w + 2, 32 - half_h + 2, cz, cx + half_w - 2, 32 + half_h - 2, cz + 2, fill_c)
    # Емблема по центру
    _fill_box(grid, cx - 2, 30, cz - 1, cx + 2, 34, cz + 4, emblem_c)

    return grid


# ============ ГЕНЕРАЦІЯ ПАКЕТУ ============

GENERATORS = {
    'sword': generate_sword,
    'axe': generate_axe,
    'staff': generate_staff,
    'tree': generate_tree,
    'block': generate_block,
    'shield': generate_shield,
}


def generate_dataset(output_dir, count_per_type=50):
    """Генерує пакет синтетичних RGBA вокселів."""
    voxels_dir = os.path.join(output_dir, "voxels")
    os.makedirs(voxels_dir, exist_ok=True)

    total = 0
    for name, gen_fn in GENERATORS.items():
        print(f"Generating {count_per_type}x {name}...")
        for i in range(count_per_type):
            grid = gen_fn()
            occupied = (grid[:, :, :, 3] > 0).sum()
            if occupied < 10:
                continue

            filename = f"synth_{name}_{i:04d}.npy"
            np.save(os.path.join(voxels_dir, filename), grid)
            total += 1

    print(f"✅ Згенеровано {total} синтетичних RGBA voxel моделей у {voxels_dir}")
    return total


if __name__ == "__main__":
    output_dir = os.path.join(os.path.dirname(__file__), "..", "..", "data")
    count = 80  # 80 × 6 типів = 480 моделей
    generate_dataset(output_dir, count_per_type=count)
