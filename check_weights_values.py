import torch
import numpy as np

def check_weights(path):
    print(f"Checking {path}...")
    try:
        ckpt = torch.load(path, map_location="cpu")
        g = ckpt['generator']
        
        # Check final layer (index 9 in Sequential)
        bias = g['decoder.9.bias']
        weight = g['decoder.9.weight']
        
        print(f"Final Bias (index 9): Mean={bias.mean().item():.6f}, Std={bias.std().item():.6f}")
        print(f"Final Weight (index 9): Mean={weight.mean().item():.6f}, Std={weight.std().item():.6f}")
        
        # Check fc layer
        fc_w = g['fc.weight']
        print(f"FC Weight: Mean={fc_w.mean().item():.6f}, Std={fc_w.std().item():.6f}")
        
        # Check if they are all very close to 0
        if torch.allclose(bias, torch.zeros_like(bias), atol=1e-5):
            print("WARNING: Final Bias is almost zero!")
        if torch.allclose(weight, torch.zeros_like(weight), atol=1e-5):
            print("WARNING: Final Weight is almost zero!")
            
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    check_weights("ml_engine/weights/latest.pth")
    check_weights("ml_engine/weights/checkpoint_epoch_2200.pth")
