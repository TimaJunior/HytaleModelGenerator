
import os
import torch
import numpy as np
from PIL import Image
from torch.utils.data import Dataset
from torchvision import transforms

class VoxelDataset(Dataset):
    def __init__(self, data_dir="data", transform=None):
        self.data_dir = data_dir
        self.image_dir = os.path.join(data_dir, "images")
        self.voxel_dir = os.path.join(data_dir, "voxels")
        
        self.image_files = sorted([f for f in os.listdir(self.image_dir) if f.endswith('.png')])
        self.voxel_files = sorted([f for f in os.listdir(self.voxel_dir) if f.endswith('.npy')])
        
        # Verify alignment
        # In a real app we'd check filenames match exactly
        assert len(self.image_files) == len(self.voxel_files), "Mismatch between images and voxels count"

        self.transform = transform or transforms.Compose([
            transforms.Resize((256, 256)),
            transforms.ToTensor(),
            # Normalize to match typical ImageNet stats or just [0,1]
             # transforms.Normalize(mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225]),
        ])

    def __len__(self):
        return len(self.image_files)

    def __getitem__(self, idx):
        img_name = self.image_files[idx]
        vox_name = self.voxel_files[idx]
        
        # Load Image
        img_path = os.path.join(self.image_dir, img_name)
        image = Image.open(img_path).convert("RGB")
        
        if self.transform:
            image = self.transform(image)
            
        # Load Voxel
        vox_path = os.path.join(self.voxel_dir, vox_name)
        # Load numpy array (32, 32, 32)
        voxels = np.load(vox_path).astype(np.float32)
        
        # Convert to Tensor and add channel dim -> (1, 32, 32, 32)
        voxel_tensor = torch.from_numpy(voxels).unsqueeze(0)
        
        return image, voxel_tensor
