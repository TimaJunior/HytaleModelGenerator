
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
    resume=True, # Auto-resume by default
    device="cuda" if torch.cuda.is_available() else "cpu"
):
    print(f"Starting Training on {device}...")
    os.makedirs(save_dir, exist_ok=True)

    # 1. Dataset & Loader
    dataset = VoxelDataset(data_dir=data_dir)
    dataloader = DataLoader(dataset, batch_size=batch_size, shuffle=True, num_workers=0)

    # 2. Initialize Models
    encoder = ResNetEncoder().to(device)
    generator = VoxelGANGenerator().to(device)
    discriminator = VoxelDiscriminator().to(device)
    
    # 3. Optimizers
    optimizerG = optim.Adam(list(generator.parameters()) + list(encoder.parameters()), lr=lr, betas=(beta1, 0.999))
    optimizerD = optim.Adam(discriminator.parameters(), lr=lr, betas=(beta1, 0.999))
    
    start_epoch = 0
    
    # Resume logic
    latest_path = os.path.join(save_dir, "latest.pth")
    if resume and os.path.exists(latest_path):
        print(f"Resuming from {latest_path}...")
        checkpoint = torch.load(latest_path, map_location=device)
        
        encoder.load_state_dict(checkpoint['encoder'])
        generator.load_state_dict(checkpoint['generator'])
        # Optional: Load discriminator if it exists (for training continuation)
        if 'discriminator' in checkpoint:
            discriminator.load_state_dict(checkpoint['discriminator'])
        if 'optimizerG' in checkpoint:
            optimizerG.load_state_dict(checkpoint['optimizerG'])
        if 'optimizerD' in checkpoint:
            optimizerD.load_state_dict(checkpoint['optimizerD'])
        if 'epoch' in checkpoint:
            start_epoch = checkpoint['epoch'] + 1
            
        print(f"Resumed from epoch {start_epoch}")
    else:
        # Initialize weights only if not resuming
        generator.apply(weights_init)
        discriminator.apply(weights_init)
    
    # 4. Loss Functions
    criterion_gan = nn.BCELoss()
    criterion_l1 = nn.L1Loss() 
    
    lambda_l1 = 100.0 

    print(f"Loaded {len(dataset)} samples.")

    # 5. Training Loop
    start_time = time.time()
    
    # Allow extending training: run 'epochs' MORE epochs, or run UNTIL 'epochs'?
    # Usually "run for N epochs" is easier for user interaction.
    # Let's say we run for 'epochs' additional epochs.
    # end_epoch is now the absolute target epoch
    end_epoch = epochs
    
    if start_epoch >= end_epoch:
        print(f"Target epoch {end_epoch} already reached or exceeded (current: {start_epoch}). Nothing to do.")
        return

    for epoch in range(start_epoch, end_epoch):
        for i, (images, real_voxels) in enumerate(dataloader):
            bs = images.size(0)
            
            real_voxels = real_voxels.to(device)
            images = images.to(device)
            
            #Labels
            real_label = torch.ones(bs, 1, device=device)
            fake_label = torch.zeros(bs, 1, device=device)
            
            # ... (Standard training steps same as before) ...
            
            # Match original indentation/logic for steps
            # ---------------------
            #  Train Discriminator
            # ---------------------
            optimizerD.zero_grad()
            output_real = discriminator(real_voxels)
            errD_real = criterion_gan(output_real, real_label)
            
            latent = encoder(images)
            fake_voxels = generator(latent)
            output_fake = discriminator(fake_voxels.detach())
            errD_fake = criterion_gan(output_fake, fake_label)
            errD = (errD_real + errD_fake) / 2
            errD.backward()
            optimizerD.step()
            
            # -----------------
            #  Train Generator
            # -----------------
            optimizerG.zero_grad()
            output_fake_for_G = discriminator(fake_voxels)
            errG_gan = criterion_gan(output_fake_for_G, real_label)
            errG_l1 = criterion_l1(fake_voxels, real_voxels) * lambda_l1
            errG = errG_gan + errG_l1
            errG.backward()
            optimizerG.step()
            
            if i % 10 == 0:
                print(f"[{epoch+1}/{end_epoch}][{i}/{len(dataloader)}] "
                      f"Loss_D: {errD.item():.4f} "
                      f"Loss_G: {errG.item():.4f}")
                      
        # Save Checkpoint with full state
        state = {
            'epoch': epoch,
            'encoder': encoder.state_dict(),
            'generator': generator.state_dict(),
            'discriminator': discriminator.state_dict(),
            'optimizerG': optimizerG.state_dict(),
            'optimizerD': optimizerD.state_dict()
        }
        
        # Save numbered checkpoint only every 50 epochs to save space
        if (epoch + 1) % 50 == 0:
            torch.save(state, os.path.join(save_dir, f"checkpoint_epoch_{epoch+1}.pth"))
        
        # Always update latest.pth
        torch.save(state, os.path.join(save_dir, "latest.pth"))
        
    print(f"Training finished in {time.time() - start_time:.2f}s")

if __name__ == "__main__":
    # Test run
    train(epochs=2)
