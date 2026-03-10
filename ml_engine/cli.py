
import sys
import os
import json
import contextlib

# Ensure project root is in path
sys.path.append(os.getcwd())

# Redirect stdout to stderr globally to prevent library noise from imports/init
@contextlib.contextmanager
def strict_stdout():
    original_stdout = sys.stdout
    sys.stdout = sys.stderr
    try:
        yield original_stdout
    finally:
        sys.stdout = original_stdout


def main():
    # All heavy imports happen INSIDE strict_stdout so any library-level
    # print() or stdout noise is redirected to stderr, keeping stdout clean
    # for the single JSON payload we write at the end.
    with strict_stdout() as real_stdout:
        import torch
        import numpy as np
        from PIL import Image
        from torchvision import transforms
        from ml_engine.services.inference import ModelInferenceService

        if len(sys.argv) > 1 and sys.argv[1] == "--train":
            from ml_engine.train import train
            try:
                # Default 10 epochs
                epochs = 10
                # Check for optional epoch argument
                if len(sys.argv) > 2:
                    try:
                        epochs = int(sys.argv[2])
                    except ValueError:
                        real_stdout.write(json.dumps({"error": "Invalid epoch count"}))
                        sys.exit(1)

                train(epochs=epochs)
                real_stdout.write(json.dumps({"status": "success", "message": f"Training completed for {epochs} epochs"}))
            except Exception as e:
                real_stdout.write(json.dumps({"error": str(e)}))
            sys.exit(0)

        if len(sys.argv) < 2:
            real_stdout.write(json.dumps({"error": "No image path provided"}))
            sys.exit(1)

        image_path = sys.argv[1]

        try:
            # Preprocess Image: Pad to Square to preserve aspect ratio
            # This matches the training data generation where voxels act as a 1:1 grid
            def pad_to_square(img, background_color=(0, 0, 0)):
                width, height = img.size
                if width == height:
                    return img
                max_dim = max(width, height)
                new_img = Image.new("RGB", (max_dim, max_dim), background_color)
                # Paste in center
                new_img.paste(img, ((max_dim - width) // 2, (max_dim - height) // 2))
                return new_img

            transform = transforms.Compose([
                transforms.Resize((256, 256)),
                transforms.ToTensor(),
            ])

            image = Image.open(image_path).convert('RGB')
            image = pad_to_square(image)  # Pad first
            input_tensor = transform(image).unsqueeze(0)  # (1, 3, 256, 256)

            # Run Inference
            weights_path = os.path.join("ml_engine", "weights", "latest.pth")
            if not os.path.exists(weights_path):
                weights_path = None

            service = ModelInferenceService(weights_path=weights_path, device="cpu")
            output_voxels = service.generate_from_image(input_tensor)

            # Check stats
            probs = output_voxels.squeeze().numpy()
            sys.stderr.write(f"Voxel Probabilities -> Min: {probs.min():.4f}, Max: {probs.max():.4f}, Mean: {probs.mean():.4f}\n")

            # Custom threshold from args or default 0.5
            threshold = 0.5
            if len(sys.argv) > 2 and sys.argv[2].startswith("--threshold="):
                try:
                    threshold = float(sys.argv[2].split("=")[1])
                except Exception:
                    pass

            sys.stderr.write(f"Using threshold: {threshold}\n")

            active_voxels = (output_voxels > threshold).int().squeeze().numpy()

            # Get coordinates of active voxels
            coords = np.argwhere(active_voxels == 1).tolist()

            result = {
                "status": "success",
                "model_shape": list(active_voxels.shape),
                "voxel_count": len(coords),
                "debug_info": {
                    "min_prob": float(probs.min()),
                    "max_prob": float(probs.max()),
                    "mean_prob": float(probs.mean()),
                    "threshold_used": threshold
                },
                "voxels": coords  # List of [z, y, x]
            }

            real_stdout.write(json.dumps(result))

        except Exception as e:
            real_stdout.write(json.dumps({"error": str(e)}))
            sys.exit(1)


if __name__ == "__main__":
    main()
