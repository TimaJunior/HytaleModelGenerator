import os
import glob
import numpy as np
import matplotlib.pyplot as plt

def visualize_latest_snapshot():
    snapshot_dir = "ml_engine/weights/snapshots"
    if not os.path.exists(snapshot_dir):
        print("Немає папки зі snapshots.")
        return

    # Find the latest epoch folder
    epoch_dirs = glob.glob(os.path.join(snapshot_dir, "epoch_*"))
    if not len(epoch_dirs):
        print("Не знайдено збережених епох.")
        return
        
    latest_dir = sorted(epoch_dirs, key=lambda x: int(x.split("_")[-1]))[-1]
    
    # Get fake and real samples
    fake_files = glob.glob(os.path.join(latest_dir, "*_fake.npy"))
    if not fake_files:
        print(f"Немає fake вокселів у {latest_dir}")
        return
        
    # Real ones are stored in the root of snapshots dir
    
    num_samples = len(fake_files)
    fig, axes = plt.subplots(num_samples, 2, figsize=(8, 4 * num_samples))
    
    if num_samples == 1:
        axes = np.array([axes])
        
    for i, fake_path in enumerate(sorted(fake_files)):
        fake_voxel = np.load(fake_path)
        
        # Try to find corresponding real voxel
        sample_idx = fake_path.split("_")[-2] # e.g. "sample_0_fake.npy" -> "0"
        real_path = os.path.join(snapshot_dir, f"sample_{sample_idx}_real.npy")
        
        has_real = os.path.exists(real_path)
        if has_real:
            real_voxel = np.load(real_path)

        # Plot max projections
        axes[i, 0].imshow(np.max(fake_voxel, axis=1), cmap='gray', vmin=0, vmax=1)
        axes[i, 0].set_title(f"Gen (Y-Proj) {os.path.basename(fake_path)}")
        axes[i, 0].axis("off")
        
        if has_real:
            axes[i, 1].imshow(np.max(real_voxel, axis=1), cmap='gray', vmin=0, vmax=1)
            axes[i, 1].set_title(f"Target (Y-Proj)")
        else:
            axes[i, 1].set_title("Target: Not found")
        axes[i, 1].axis("off")

    plt.tight_layout()
    output_png = "latest_snapshot.png"
    plt.savefig(output_png)
    print(f"✅ Результати епохи {os.path.basename(latest_dir)} збережено у {output_png}")

if __name__ == "__main__":
    visualize_latest_snapshot()
