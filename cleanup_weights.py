
import os
import glob

weights_dir = "ml_engine/weights"
files = glob.glob(os.path.join(weights_dir, "checkpoint_epoch_*.pth"))

# Keep every 100th, and the last 5
to_keep = set()
for f in files:
    try:
        # Extract epoch number from checkpoint_epoch_N.pth
        name = os.path.basename(f)
        epoch = int(name.replace("checkpoint_epoch_", "").replace(".pth", ""))
        if epoch == 1 or epoch % 100 == 0 or epoch >= 770:
            to_keep.add(f)
    except:
        continue

for f in files:
    if f not in to_keep:
        try:
            os.remove(f)
            # print(f"Deleted {f}")
        except Exception as e:
            print(f"Failed to delete {f}: {e}")

print(f"Cleanup finished. Kept {len(to_keep)} checkpoints.")
