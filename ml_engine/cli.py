"""
CLI for RGBA Voxel GAN Inference.

JSON Output Contract (v2):
{
  "status": "success",
  "model_shape": [64, 64, 64],
  "voxel_count": 1234,
  "voxels": [
    {"pos": [10, 20, 5], "color": "#10B981"},
    ...
  ]
}
"""

import sys
import os
import json
import contextlib

sys.path.append(os.getcwd())


@contextlib.contextmanager
def strict_stdout():
    """Перенаправляє stdout → stderr щоб не забруднювати JSON output."""
    orig = sys.stdout
    sys.stdout = sys.stderr
    try:
        yield orig
    finally:
        sys.stdout = orig


def main():
    with strict_stdout() as real_stdout:
        import torch
        from PIL import Image
        from torchvision import transforms
        from ml_engine.services.inference import ModelInferenceService

        # --- Train mode ---
        if len(sys.argv) > 1 and sys.argv[1] == "--train":
            from ml_engine.train import train
            try:
                epochs = 500
                if len(sys.argv) > 2:
                    try:
                        epochs = int(sys.argv[2])
                    except ValueError:
                        real_stdout.write(json.dumps({"error": "Invalid epoch count"}))
                        sys.exit(1)
                train(epochs=epochs)
                real_stdout.write(json.dumps({
                    "status": "success",
                    "message": f"Training completed, target_epochs={epochs}"
                }))
            except Exception as e:
                real_stdout.write(json.dumps({"error": str(e)}))
            sys.exit(0)

        # --- Inference mode ---
        if len(sys.argv) < 2:
            real_stdout.write(json.dumps({"error": "No image path provided"}))
            sys.exit(1)

        image_path = sys.argv[1]

        try:
            # Threshold
            threshold = 0.4
            for arg in sys.argv[2:]:
                if arg.startswith("--threshold="):
                    try:
                        threshold = float(arg.split("=")[1])
                    except Exception:
                        pass

            # Preprocessing
            def pad_to_square(img):
                w, h = img.size
                if w == h:
                    return img
                m = max(w, h)
                new_img = Image.new("RGB", (m, m), (0, 0, 0))
                new_img.paste(img, ((m - w) // 2, (m - h) // 2))
                return new_img

            transform = transforms.Compose([
                transforms.Resize((256, 256)),
                transforms.ToTensor(),
                transforms.Normalize(mean=[0.5, 0.5, 0.5], std=[0.5, 0.5, 0.5]),
            ])

            image = Image.open(image_path).convert("RGB")
            image = pad_to_square(image)
            input_tensor = transform(image).unsqueeze(0)  # (1, 3, 256, 256)

            # Load model
            weights_path = os.path.join("ml_engine", "weights", "latest.pth")
            weights_path = weights_path if os.path.exists(weights_path) else None

            service = ModelInferenceService(weights_path=weights_path, device="cpu")

            # Get colored voxels
            voxels = service.get_colored_voxels(
                input_tensor,
                threshold=threshold,
                max_voxels=4096,
            )

            sys.stderr.write(f"Threshold={threshold}, voxel_count={len(voxels)}\n")

            result = {
                "status": "success",
                "model_shape": [64, 64, 64],
                "voxel_count": len(voxels),
                "voxels": voxels,
            }
            real_stdout.write(json.dumps(result))

        except Exception as e:
            real_stdout.write(json.dumps({"error": str(e)}))
            sys.exit(1)


if __name__ == "__main__":
    main()
