"""
Regression tests for CLI contract and serving compatibility.
"""

import os
import json
import subprocess
import pytest
import torch
from ml_engine.services.inference import ModelInferenceService

PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
CLI_PATH = os.path.join(PROJECT_ROOT, "ml_engine", "cli.py")
TEST_IMAGE_PATH = os.path.join(PROJECT_ROOT, "data", "images", "sample_0.png")

class TestServingRegression:
    
    @pytest.fixture
    def inference_service(self):
        """Ініціалізація сервісу без ваг (використовує fallback random initialization)."""
        return ModelInferenceService()

    def test_inference_pipeline_execution(self, inference_service):
        """Перевірка forward pass від image tensor до voxel grid."""
        dummy_input = torch.randn(1, 3, 256, 256)
        output = inference_service.generate_from_image(dummy_input)
        
        # Перевірка контракту форми та значень
        assert output.shape == (1, 1, 32, 32, 32)
        assert output.dtype == torch.float32
        
        # Output is Sigmoid probability, should be in [0, 1]
        assert torch.all(output >= 0.0)
        assert torch.all(output <= 1.0)

    @pytest.mark.skipif(not os.path.exists(TEST_IMAGE_PATH), reason="Test image not found")
    def test_cli_json_contract(self):
        """Перевіряє, що CLI.py повертає JSON сумісний з Hytale frontend."""
        # cli.py expects: python cli.py <image_path> [--threshold=0.5]
        cmd = ["python", CLI_PATH, TEST_IMAGE_PATH]
        result = subprocess.run(cmd, capture_output=True, text=True)
        
        assert result.returncode == 0, f"CLI command failed: {result.stderr}"
        
        try:
            output_data = json.loads(result.stdout)
        except json.JSONDecodeError:
            pytest.fail(f"CLI did not output valid JSON. Output:\n{result.stdout}")
            
        # Contract Verification
        assert "status" in output_data
        assert output_data["status"] == "success"
        assert "model_shape" in output_data
        assert output_data["model_shape"] == [32, 32, 32]
        assert "voxel_count" in output_data
        assert "voxels" in output_data
        assert isinstance(output_data["voxels"], list)
