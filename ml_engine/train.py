
import os
import torch
import torch.nn as nn
import torch.optim as optim
from torch.utils.data import DataLoader
from ml_engine.models.gan import ResNetEncoder, VoxelGANGenerator, VoxelDiscriminator
from ml_engine.utils.dataset_loader import VoxelDataset
import time

def weights_init(m):
    classname = m.__class__.__name__
    if classname.find('Conv') != -1:
        nn.init.normal_(m.weight.data, 0.0, 0.02)
    elif classname.find('BatchNorm') != -1:
        nn.init.normal_(m.weight.data, 1.0, 0.02)
        nn.init.constant_(m.bias.data, 0)

def train(
    data_dir="data",
    epochs=10,
    batch_size=8,
    lr=0.0002,
    beta1=0.5,
    save_dir="ml_engine/weights",
    device="cuda" if torch.cuda.is_available() else "cpu"
):
    print(f"Starting Training on {device}...")
    os.makedirs(save_dir, exist_ok=True)

    # 1. Dataset & Loader
    dataset = VoxelDataset(data_dir=data_dir)
    dataloader = DataLoader(dataset, batch_size=batch_size, shuffle=True, num_workers=0) # workers=0 for windows compat

    # 2. Initialize Models
    encoder = ResNetEncoder().to(device)
    generator = VoxelGANGenerator().to(device)
    discriminator = VoxelDiscriminator().to(device)
    
    # Initialize weights
    generator.apply(weights_init)
    discriminator.apply(weights_init)
    
    # 3. Optimizers
    # Encoder usually trained with Generator
    optimizerG = optim.Adam(list(generator.parameters()) + list(encoder.parameters()), lr=lr, betas=(beta1, 0.999))
    optimizerD = optim.Adam(discriminator.parameters(), lr=lr, betas=(beta1, 0.999))
    
    # 4. Loss Functions
    criterion_gan = nn.BCELoss()
    criterion_l1 = nn.L1Loss() # For reconstruction consistency
    
    lambda_l1 = 100.0 # Weight for reconstruction loss

    print(f"Loaded {len(dataset)} samples.")

    # 5. Training Loop
    start_time = time.time()
    
    for epoch in range(epochs):
        for i, (images, real_voxels) in enumerate(dataloader):
            bs = images.size(0)
            
            real_voxels = real_voxels.to(device) # (B, 1, 32, 32, 32)
            images = images.to(device)
            
            #Labels
            real_label = torch.ones(bs, 1, device=device)
            fake_label = torch.zeros(bs, 1, device=device)
            
            # ---------------------
            #  Train Discriminator
            # ---------------------
            optimizerD.zero_grad()
            
            # Real
            output_real = discriminator(real_voxels)
            errD_real = criterion_gan(output_real, real_label)
            
            # Fake
            latent = encoder(images)
            fake_voxels = generator(latent)
            output_fake = discriminator(fake_voxels.detach()) # Detach to stop G gradient
            errD_fake = criterion_gan(output_fake, fake_label)
            
            errD = (errD_real + errD_fake) / 2
            errD.backward()
            optimizerD.step()
            
            # -----------------
            #  Train Generator
            # -----------------
            optimizerG.zero_grad()
            
            # GAN Loss (fool D)
            output_fake_for_G = discriminator(fake_voxels)
            errG_gan = criterion_gan(output_fake_for_G, real_label)
            
            # Reconstruction Loss (L1)
            errG_l1 = criterion_l1(fake_voxels, real_voxels) * lambda_l1
            
            errG = errG_gan + errG_l1
            errG.backward()
            optimizerG.step()
            
            # Log
            if i % 10 == 0:
                print(f"[{epoch+1}/{epochs}][{i}/{len(dataloader)}] "
                      f"Loss_D: {errD.item():.4f} "
                      f"Loss_G: {errG.item():.4f}")
                      
        # Save Checkpoint
        torch.save({
            'encoder': encoder.state_dict(),
            'generator': generator.state_dict(),
            'discriminator': discriminator.state_dict()
        }, os.path.join(save_dir, f"checkpoint_epoch_{epoch+1}.pth"))
        
        # Save 'latest.pth' for easy inference
        torch.save({
            'encoder': encoder.state_dict(),
            'generator': generator.state_dict()
        }, os.path.join(save_dir, "latest.pth"))
        
    print(f"Training finished in {time.time() - start_time:.2f}s")

if __name__ == "__main__":
    # Test run
    train(epochs=2)
