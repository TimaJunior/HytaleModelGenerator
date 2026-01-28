
import torch
import sys
import os

files = ["ml_engine/weights/latest.pth", "ml_engine/weights/checkpoint_epoch_775.pth", "ml_engine/weights/checkpoint_epoch_776.pth"]
for path in files:
    if not os.path.exists(path):
        print(f"File {path} does not exist.")
        continue
    try:
        checkpoint = torch.load(path, map_location="cpu")
        print(f"OK: {path} (Epoch {checkpoint.get('epoch')})")
    except Exception as e:
        print(f"CORRUPT: {path} - {e}")
