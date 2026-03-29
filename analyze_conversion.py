import os
import glob
import json

def analyze():
    data_dir = "data"
    manifest_path = os.path.join(data_dir, "manifest.json")
    
    if not os.path.exists(manifest_path):
        print("Manifest not found!")
        return
        
    with open(manifest_path, "r", encoding="utf-8") as f:
        manifest = json.load(f)
        
    print("=== Статистика Dataset ===")
    print(f"Total Valid Samples (image+voxel pairs): {manifest['stats']['total_samples']}")
    print(f"Unique Voxel Assets: {manifest['stats']['unique_assets']}")
    
    # Check current files
    images = glob.glob(os.path.join(data_dir, "images", "*.png"))
    voxels = glob.glob(os.path.join(data_dir, "voxels", "*.npy"))
    
    print("\n=== Файли на диску ===")
    print(f"Images (.png): {len(images)}")
    print(f"Voxels (.npy): {len(voxels)}")
    
    if len(images) == len(voxels) and len(images) == manifest['stats']['total_samples']:
        print("✅ Усі файли консистентні! Зайвих вокселів чи зображень немає.")
    else:
        print("❌ Присутні неконсистентні файли на диску.")
        
    # Find raw models
    raw_exts = ("*.obj", "*.bbmodel", "*.glb")
    raw_models = []
    for ext in raw_exts:
        raw_models.extend(glob.glob(os.path.join("data", "**", ext), recursive=True))
        
    raw_extracted = [m for m in raw_models if "extracted" in m]
    
    print("\n=== Сирі 3D Моделі ===")
    print(f"Total raw 3D models found: {len(raw_models)}")
    print(f"Total raw 3D models in 'extracted' folder: {len(raw_extracted)}")
    
    avg_views = manifest['stats']['total_samples'] / max(1, manifest['stats']['unique_assets'])
    print(f"\nКонвертовано ~ {manifest['stats']['unique_assets']} унікальних моделей (асетів) у датасет.")
    print(f"В середньому ~ {avg_views:.1f} ракурсів (views) на кожну конвертовану 3D модель.")

if __name__ == "__main__":
    analyze()
