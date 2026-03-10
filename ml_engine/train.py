import os
import torch
import torch.nn as nn
import torch.nn.functional as F
import torch.optim as optim
from torch.utils.data import DataLoader
from ml_engine.models.gan import ResNetEncoder, VoxelGANGenerator, ConditionalDiscriminator
from ml_engine.utils.dataset_loader import VoxelDataset
import time
import numpy as np

def weights_init(m):
    classname = m.__class__.__name__
    if classname.find('Conv') != -1:
        nn.init.normal_(m.weight.data, 0.0, 0.02)
    elif classname.find('BatchNorm') != -1:
        nn.init.normal_(m.weight.data, 1.0, 0.02)
        nn.init.constant_(m.bias.data, 0)

def dice_loss(pred, target, smooth=1.0):
    """Dice loss dla sparse binary voxels."""
    pred_flat = pred.contiguous().view(-1)
    target_flat = target.contiguous().view(-1)
    intersection = (pred_flat * target_flat).sum()
    return 1 - (2. * intersection + smooth) / (pred_flat.sum() + target_flat.sum() + smooth)

def voxel_bce_loss(pred, target):
    """Weighted BCE: occupied voxels мають більшу вагу."""
    # Зважаючи на те, що більшість вокселей пусті (0)
    occupied_ratio = target.sum() / (target.numel() + 1e-6)
    pos_weight = (1.0 - occupied_ratio) / (occupied_ratio + 1e-6)
    pos_weight = torch.clamp(pos_weight, max=20.0)
    
    # Використовуємо F.binary_cross_entropy з manual weighting
    # Оскільки weight в F.binary_cross_entropy це per-element weight, ми створюємо tensor:
    weight = torch.ones_like(target)
    weight[target > 0.5] = pos_weight
    
    return F.binary_cross_entropy(pred, target, weight=weight, reduction='mean')

def generate_qualitative_snapshot(encoder, generator, val_loader, epoch, save_dir, device):
    """Зберігає один батч передбачень для візуального порівняння."""
    snapshot_dir = os.path.join(save_dir, "snapshots", f"epoch_{epoch}")
    os.makedirs(snapshot_dir, exist_ok=True)
    
    encoder.eval()
    generator.eval()
    
    with torch.no_grad():
        # Беремо лише перший батч (або перші 4 семпли)
        images, real_voxels = next(iter(val_loader))
        images = images[:4].to(device)
        real_voxels = real_voxels[:4].to(device)
        
        latent = encoder(images)
        fake_voxels = generator(latent)
        
        for i in range(images.size(0)):
            # Save generated voxel
            fake_np = fake_voxels[i, 0].cpu().numpy()
            np.save(os.path.join(snapshot_dir, f"sample_{i}_fake.npy"), fake_np)
            
            # Save real voxel for reference (only once ideally, but simple enough to overwrite)
            if epoch == 1:
                real_np = real_voxels[i, 0].cpu().numpy()
                np.save(os.path.join(save_dir, "snapshots", f"sample_{i}_real.npy"), real_np)

def validate(encoder, generator, val_loader, device):
    """Validation loop для оцінки IoU."""
    encoder.eval()
    generator.eval()
    
    val_metrics = {"iou": 0.0, "count": 0}
    with torch.no_grad():
        for images, real_voxels in val_loader:
            images = images.to(device)
            real_voxels = real_voxels.to(device)
            
            latent = encoder(images)
            fake_voxels = generator(latent)
            
            # Розрахунок IoU
            pred_binary = (fake_voxels > 0.5).float()
            intersection = (pred_binary * real_voxels).sum(dim=[1,2,3,4])
            union = ((pred_binary + real_voxels) > 0).float().sum(dim=[1,2,3,4])
            
            iou = intersection / (union + 1e-6)
            val_metrics["iou"] += iou.sum().item()
            val_metrics["count"] += images.size(0)
            
    avg_iou = val_metrics["iou"] / val_metrics["count"]
    return avg_iou

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
    # Lower defaults for finetuning on real data
    lr = 0.00005 
    print(f"Starting Training on {device} with LR={lr}...")
    os.makedirs(save_dir, exist_ok=True)
    os.makedirs(os.path.join(save_dir, "snapshots"), exist_ok=True)

    # 1. Dataset & Loaders
    train_dataset = VoxelDataset(data_dir=data_dir, split="train")
    val_dataset = VoxelDataset(data_dir=data_dir, split="val")
    
    train_loader = DataLoader(train_dataset, batch_size=batch_size, shuffle=True, num_workers=0)
    val_loader = DataLoader(val_dataset, batch_size=batch_size, shuffle=False, num_workers=0)

    # 2. Initialize Models
    encoder = ResNetEncoder().to(device)
    generator = VoxelGANGenerator().to(device)
    discriminator = ConditionalDiscriminator().to(device) # NEW
    
    # 3. Optimizers
    optimizerG = optim.Adam(list(generator.parameters()) + list(encoder.parameters()), lr=lr, betas=(beta1, 0.999))
    optimizerD = optim.Adam(discriminator.parameters(), lr=lr, betas=(beta1, 0.999))
    
    start_epoch = 0
    best_iou = 0.0
    
    # Resume logic
    latest_path = os.path.join(save_dir, "latest.pth")
    if resume and os.path.exists(latest_path):
        print(f"Resuming from {latest_path}...")
        checkpoint = torch.load(latest_path, map_location=device, weights_only=True)
        
        try:
            encoder.load_state_dict(checkpoint['encoder'])
            generator.load_state_dict(checkpoint['generator'])
            if 'discriminator' in checkpoint:
                discriminator.load_state_dict(checkpoint['discriminator'])
            if 'optimizerG' in checkpoint:
                optimizerG.load_state_dict(checkpoint['optimizerG'])
            if 'optimizerD' in checkpoint:
                optimizerD.load_state_dict(checkpoint['optimizerD'])
            if 'epoch' in checkpoint:
                start_epoch = checkpoint['epoch'] + 1
            if 'best_iou' in checkpoint:
                best_iou = checkpoint['best_iou']
            print(f"Resumed from epoch {start_epoch}")
        except RuntimeError as e:
            print(f"Failed to resume (likely architecture mismatch): {e}")
            print("Initializing from scratch.")
            generator.apply(weights_init)
            discriminator.apply(weights_init)
            start_epoch = 0
    else:
        # Initialize weights only if not resuming
        generator.apply(weights_init)
        discriminator.apply(weights_init)
    
    # 4. Loss Functions Config
    criterion_gan = nn.BCELoss()
    lambda_adv = 1.0
    lambda_dice = 10.0
    lambda_bce = 5.0

    print(f"Loaded {len(train_dataset)} train samples, {len(val_dataset)} val samples.")

    # 5. Training Loop
    start_time = time.time()
    
    end_epoch = start_epoch + epochs
    
    for epoch in range(start_epoch, end_epoch):
        encoder.train()
        generator.train()
        discriminator.train()
        
        epoch_loss_G = 0.0
        epoch_loss_D = 0.0
        
        for i, (images, real_voxels) in enumerate(train_loader):
            bs = images.size(0)
            
            real_voxels = real_voxels.to(device)
            images = images.to(device)
            
            #Labels
            real_label = torch.ones(bs, 1, device=device)
            fake_label = torch.zeros(bs, 1, device=device)
            
            # ---------------------
            #  Train Discriminator
            # ---------------------
            optimizerD.zero_grad()
            latent = encoder(images)
            
            # D with condition
            output_real = discriminator(real_voxels, latent.detach())
            errD_real = criterion_gan(output_real, real_label)
            
            fake_voxels = generator(latent)
            output_fake = discriminator(fake_voxels.detach(), latent.detach())
            errD_fake = criterion_gan(output_fake, fake_label)
            
            errD = (errD_real + errD_fake) / 2
            errD.backward()
            optimizerD.step()
            epoch_loss_D += errD.item()
            
            # -----------------
            #  Train Generator
            # -----------------
            optimizerG.zero_grad()
            # G wants D to output real
            output_fake_for_G = discriminator(fake_voxels, latent)
            
            errG_adv = criterion_gan(output_fake_for_G, real_label) * lambda_adv
            errG_dice = dice_loss(fake_voxels, real_voxels) * lambda_dice
            errG_wbce = voxel_bce_loss(fake_voxels, real_voxels) * lambda_bce
            
            errG = errG_adv + errG_dice + errG_wbce
            errG.backward()
            optimizerG.step()
            epoch_loss_G += errG.item()
            
            if i % 10 == 0:
                print(f"[{epoch+1}/{end_epoch}][{i}/{len(train_loader)}] "
                      f"Loss_D: {errD.item():.4f} "
                      f"Loss_G: {errG.item():.4f} (Adv: {errG_adv.item():.4f}, Dice: {errG_dice.item():.4f}, WBCE: {errG_wbce.item():.4f})")
                      
        # --- End of Epoch Validation & Checkpoints ---
        avg_iou = validate(encoder, generator, val_loader, device)
        print(f"Epoch [{epoch+1}/{end_epoch}] Validation IoU: {avg_iou:.4f}")
        
        # Qualitative Snapshot
        if (epoch + 1) == 1 or (epoch + 1) % 5 == 0:
            generate_qualitative_snapshot(encoder, generator, val_loader, epoch + 1, save_dir, device)
        
        # Save Checkpoint with full state
        state = {
            'epoch': epoch,
            'encoder': encoder.state_dict(),
            'generator': generator.state_dict(),
            'discriminator': discriminator.state_dict(),
            'optimizerG': optimizerG.state_dict(),
            'optimizerD': optimizerD.state_dict(),
            'best_iou': best_iou
        }
        
        # Always update latest.pth
        torch.save(state, os.path.join(save_dir, "latest.pth"))
        
        # Save numbered checkpoint only every 10 epochs
        if (epoch + 1) % 10 == 0:
            torch.save(state, os.path.join(save_dir, f"checkpoint_epoch_{epoch+1}.pth"))
            
        # Save BEST
        if avg_iou > best_iou:
            best_iou = avg_iou
            state['best_iou'] = best_iou
            torch.save(state, os.path.join(save_dir, "best.pth"))
            print(f"👉 New best model saved with IoU: {best_iou:.4f}")
        
    print(f"Training finished in {time.time() - start_time:.2f}s")

if __name__ == "__main__":
    # Smoke run
    train(epochs=1)
