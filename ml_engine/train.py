"""
Next-Gen RGBA Voxel GAN Training Script.

Ключові покращення порівняно з v1:
- LSGAN (Least Squares GAN) замість BCE — стабільніший
- RGBA output (4 канали: R,G,B,Alpha) замість binary (1 канал)
- PatchGAN дискримінатор (оцінює локальні ділянки, а не глобально)
- Color Loss (L1 на RGB де є вокселі)
- Perceptual Loss (через 2D max-projections + VGG features)
- AMP (Mixed Precision) — 2× прискорення на GPU
- EMA (Exponential Moving Average) ваг генератора
- CosineAnnealing LR scheduler
- TTUR (Two Time-scale Update Rule): lr_D > lr_G
- Graceful Ctrl+C збереження
"""

import os
import torch
import torch.nn as nn
import torch.nn.functional as F
import torch.optim as optim
from torch.utils.data import DataLoader
from torch.cuda.amp import GradScaler, autocast
from ml_engine.models.gan import ResNetEncoder, VoxelGANGenerator, ConditionalDiscriminator
from ml_engine.utils.dataset_loader import VoxelDataset
import time
import copy
import numpy as np
from torchvision import models


# ============================================================
# Loss Functions
# ============================================================

def dice_loss(pred_alpha: torch.Tensor, target_alpha: torch.Tensor, smooth: float = 1.0):
    """Dice loss на Alpha канал (форма)."""
    p = pred_alpha.contiguous().view(-1)
    t = target_alpha.contiguous().view(-1)
    intersection = (p * t).sum()
    return 1 - (2. * intersection + smooth) / (p.sum() + t.sum() + smooth)


def weighted_bce(pred_alpha: torch.Tensor, target_alpha: torch.Tensor):
    """Weighted BCE на Alpha канал з балансуванням порожніх/зайнятих вокселів."""
    occupied = target_alpha.sum() / (target_alpha.numel() + 1e-6)
    pos_weight = torch.clamp((1 - occupied) / (occupied + 1e-6), max=20.0)
    weight = torch.ones_like(target_alpha)
    weight[target_alpha > 0.5] = pos_weight
    return F.binary_cross_entropy(pred_alpha, target_alpha, weight=weight, reduction='mean')


def voxel_tv_loss(x: torch.Tensor):
    """3D Total Variation Loss (тільки по Alpha / першому каналу)."""
    a = x[:, 3:4]  # Alpha channel
    dh = torch.abs(a[:, :, 1:, :, :] - a[:, :, :-1, :, :]).mean()
    dw = torch.abs(a[:, :, :, 1:, :] - a[:, :, :, :-1, :]).mean()
    dd = torch.abs(a[:, :, :, :, 1:] - a[:, :, :, :, :-1]).mean()
    return dh + dw + dd


def color_loss(fake: torch.Tensor, real: torch.Tensor):
    """
    L1 Color Loss: порівнює RGB лише в тих вокселях, де є реальний об'єкт.
    Це не карає генератор за колір порожніх вокселів.
    """
    mask = (real[:, 3:4] > 0.5).float()  # Alpha маска з real
    fake_rgb = fake[:, :3] * mask
    real_rgb = real[:, :3] * mask
    n_occupied = mask.sum() + 1e-6
    return F.l1_loss(fake_rgb, real_rgb, reduction='sum') / n_occupied


# --- Perceptual Loss ---
class VGGPerceptual(nn.Module):
    """Перцептивна loss через VGG16 features."""
    def __init__(self):
        super().__init__()
        vgg = models.vgg16(weights=models.VGG16_Weights.DEFAULT)
        self.feat = nn.Sequential(*list(vgg.features)[:16])
        for p in self.parameters():
            p.requires_grad = False
        self.eval()

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        return self.feat(x)


def perceptual_3d_loss(fake: torch.Tensor, real: torch.Tensor,
                       vgg: VGGPerceptual) -> torch.Tensor:
    """
    Перцептивна loss через 2D max-projection з 3 ракурсів.
    Порівнює RGB канали через VGG features.
    """
    total = torch.tensor(0.0, device=fake.device)
    fake_rgb = fake[:, :3]   # (B, 3, D, H, W)
    real_rgb = real[:, :3]

    for axis in [2, 3, 4]:
        fake_proj = fake_rgb.max(dim=axis).values  # (B, 3, ?, ?)
        real_proj = real_rgb.max(dim=axis).values

        # Resize до 64x64 для VGG (VGG потребує мінімум 32x32)
        fake_proj = F.interpolate(fake_proj, size=(64, 64), mode='bilinear', align_corners=False)
        real_proj = F.interpolate(real_proj, size=(64, 64), mode='bilinear', align_corners=False)

        with torch.no_grad():
            rf = vgg(real_proj).detach()
        ff = vgg(fake_proj)
        total = total + F.l1_loss(ff, rf)

    return total / 3.0


def lsgan_loss_D(real_out: torch.Tensor, fake_out: torch.Tensor):
    """Least Squares GAN discriminator loss."""
    return ((real_out - 1.0) ** 2).mean() + (fake_out ** 2).mean()


def lsgan_loss_G(fake_out: torch.Tensor):
    """Least Squares GAN generator loss."""
    return ((fake_out - 1.0) ** 2).mean()


# ============================================================
# Validation
# ============================================================

def validate(encoder, generator, val_loader, device):
    """Validation: рахуємо IoU по Alpha каналу."""
    encoder.eval()
    generator.eval()
    total_iou = 0.0
    count = 0

    with torch.no_grad():
        for images, real_voxels in val_loader:
            images = images.to(device)
            real_voxels = real_voxels.to(device)

            latent = encoder(images)
            fake_voxels = generator(latent)

            # IoU по Alpha
            pred_alpha = (fake_voxels[:, 3:4] > 0.5).float()
            real_alpha = (real_voxels[:, 3:4] > 0.5).float() if real_voxels.shape[1] == 4 \
                         else real_voxels  # backward compat

            intersection = (pred_alpha * real_alpha).sum(dim=[1, 2, 3, 4])
            union = ((pred_alpha + real_alpha) > 0).float().sum(dim=[1, 2, 3, 4])
            iou = intersection / (union + 1e-6)
            total_iou += iou.sum().item()
            count += images.size(0)

    return total_iou / max(count, 1)


def generate_qualitative_snapshot(encoder, generator, val_loader, epoch, save_dir, device):
    """Зберігає RGBA voxel snapshot для візуального порівняння."""
    snapshot_dir = os.path.join(save_dir, "snapshots", f"epoch_{epoch}")
    os.makedirs(snapshot_dir, exist_ok=True)

    encoder.eval()
    generator.eval()
    with torch.no_grad():
        images, real_voxels = next(iter(val_loader))
        images = images[:4].to(device)
        real_voxels = real_voxels[:4].to(device)

        latent = encoder(images)
        fake_voxels = generator(latent)

        for i in range(images.size(0)):
            np.save(
                os.path.join(snapshot_dir, f"sample_{i}_fake.npy"),
                fake_voxels[i].cpu().numpy()
            )
            if epoch == 1:
                np.save(
                    os.path.join(save_dir, "snapshots", f"sample_{i}_real.npy"),
                    real_voxels[i].cpu().numpy()
                )


# ============================================================
# Training Loop
# ============================================================

def train(
    data_dir: str = "data",
    epochs: int = 500,
    batch_size: int = 4,
    lr_G: float = 1e-4,
    lr_D: float = 4e-4,
    save_dir: str = "ml_engine/weights",
    resume: bool = True,
    device: str = "cuda" if torch.cuda.is_available() else "cpu",
):
    """
    Тренування RGBA Voxel GAN.

    Args:
        epochs: Цільова кінцева епоха (не кількість, а номер)
        lr_G: Learning rate генератора
        lr_D: Learning rate дискримінатора (TTUR: lr_D > lr_G)
    """
    print(f"🚀 Starting RGBA Voxel GAN Training on {device}")
    print(f"   lr_G={lr_G}, lr_D={lr_D}, batch={batch_size}, target_epochs={epochs}")
    os.makedirs(save_dir, exist_ok=True)
    os.makedirs(os.path.join(save_dir, "snapshots"), exist_ok=True)

    use_amp = (device == "cuda")

    # --- 1. Dataset ---
    train_dataset = VoxelDataset(data_dir=data_dir, split="train", augment=True)
    val_dataset = VoxelDataset(data_dir=data_dir, split="val", augment=False)
    train_loader = DataLoader(train_dataset, batch_size=batch_size, shuffle=True, num_workers=0)
    val_loader = DataLoader(val_dataset, batch_size=batch_size, shuffle=False, num_workers=0)
    print(f"   Train: {len(train_dataset)}, Val: {len(val_dataset)}")

    # --- 2. Models ---
    encoder = ResNetEncoder(latent_dim=512).to(device)
    generator = VoxelGANGenerator(latent_dim=512).to(device)
    discriminator = ConditionalDiscriminator(latent_dim=512).to(device)

    # EMA для генератора
    ema_generator = copy.deepcopy(generator)
    ema_generator.eval()
    ema_decay = 0.999

    # --- 3. Optimizers ---
    optimizerG = optim.Adam(
        list(generator.parameters()) + list(encoder.parameters()),
        lr=lr_G, betas=(0.0, 0.9)
    )
    optimizerD = optim.Adam(
        discriminator.parameters(),
        lr=lr_D, betas=(0.0, 0.9)
    )

    # LR Schedulers
    schedulerG = optim.lr_scheduler.CosineAnnealingWarmRestarts(
        optimizerG, T_0=100, T_mult=2
    )
    schedulerD = optim.lr_scheduler.CosineAnnealingWarmRestarts(
        optimizerD, T_0=100, T_mult=2
    )

    # AMP GradScalers
    scaler_G = GradScaler(enabled=use_amp)
    scaler_D = GradScaler(enabled=use_amp)

    # Perceptual Loss (VGG)
    vgg_perceptual = VGGPerceptual().to(device)

    # --- 4. Resume ---
    start_epoch = 0
    best_iou = 0.0
    latest_path = os.path.join(save_dir, "latest.pth")

    if resume and os.path.exists(latest_path):
        print(f"   Resuming from {latest_path}")
        checkpoint = torch.load(latest_path, map_location=device, weights_only=True)

        # Спробуємо завантажити — якщо архітектура змінилась, починаємо заново
        try:
            encoder.load_state_dict(checkpoint.get('encoder', {}))
            generator.load_state_dict(checkpoint.get('generator', {}))
            if 'discriminator' in checkpoint:
                discriminator.load_state_dict(checkpoint['discriminator'])
            if 'ema_generator' in checkpoint:
                ema_generator.load_state_dict(checkpoint['ema_generator'])
            if 'optimizerG' in checkpoint:
                optimizerG.load_state_dict(checkpoint['optimizerG'])
            if 'optimizerD' in checkpoint:
                optimizerD.load_state_dict(checkpoint['optimizerD'])
            start_epoch = checkpoint.get('epoch', -1) + 1
            best_iou = checkpoint.get('best_iou', 0.0)
            print(f"   ✅ Resumed from epoch {start_epoch}")
        except (RuntimeError, KeyError) as e:
            print(f"   ⚠️  Cannot resume (architecture changed): {e}")
            print("   Starting from scratch...")
            start_epoch = 0

    end_epoch = epochs
    if start_epoch >= end_epoch:
        print(f"   Already at epoch {start_epoch}. Set higher --epochs target.")
        return

    # --- 5. Loss weights ---
    lambda_dice = 8.0
    lambda_bce = 4.0
    lambda_tv = 0.5     # Менше, ніж в v1: не придушувати тонкі деталі
    lambda_color = 6.0
    lambda_perceptual = 1.5
    lambda_adv = 1.0

    # --- 6. Training Loop ---
    start_time = time.time()
    current_epoch = start_epoch

    try:
        for epoch in range(start_epoch, end_epoch):
            current_epoch = epoch
            encoder.train()
            generator.train()
            discriminator.train()

            epoch_loss_G = 0.0
            epoch_loss_D = 0.0
            n_batches = 0

            for i, (images, real_voxels) in enumerate(train_loader):
                bs = images.size(0)
                images = images.to(device)
                real_voxels = real_voxels.to(device)

                # Якщо реальні вокселі binary 32³ — пропускаємо
                if real_voxels.shape[1] != 4:
                    continue

                # ─── Discriminator Step ───
                optimizerD.zero_grad()
                with autocast(enabled=use_amp):
                    latent = encoder(images)
                    fake_voxels = generator(latent)

                    real_score = discriminator(real_voxels, latent.detach())
                    fake_score = discriminator(fake_voxels.detach(), latent.detach())
                    errD = lsgan_loss_D(real_score, fake_score)

                scaler_D.scale(errD).backward()
                scaler_D.step(optimizerD)
                scaler_D.update()

                # ─── Generator Step ───
                optimizerG.zero_grad()
                with autocast(enabled=use_amp):
                    fake_score_G = discriminator(fake_voxels, latent)

                    errG_adv = lsgan_loss_G(fake_score_G) * lambda_adv
                    errG_dice = dice_loss(fake_voxels[:, 3:4], real_voxels[:, 3:4]) * lambda_dice
                    errG_bce = weighted_bce(fake_voxels[:, 3:4], real_voxels[:, 3:4]) * lambda_bce
                    errG_tv = voxel_tv_loss(fake_voxels) * lambda_tv
                    errG_color = color_loss(fake_voxels, real_voxels) * lambda_color
                    errG_perceptual = perceptual_3d_loss(fake_voxels, real_voxels, vgg_perceptual) * lambda_perceptual

                    errG = errG_adv + errG_dice + errG_bce + errG_tv + errG_color + errG_perceptual

                scaler_G.scale(errG).backward()
                torch.nn.utils.clip_grad_norm_(
                    list(generator.parameters()) + list(encoder.parameters()), max_norm=1.0
                )
                scaler_G.step(optimizerG)
                scaler_G.update()

                # ─── EMA Update ───
                with torch.no_grad():
                    for ema_p, gen_p in zip(ema_generator.parameters(), generator.parameters()):
                        ema_p.data.mul_(ema_decay).add_(gen_p.data, alpha=1 - ema_decay)

                epoch_loss_G += errG.item()
                epoch_loss_D += errD.item()
                n_batches += 1

                if i % 20 == 0:
                    print(f"  [{epoch+1}/{end_epoch}][{i}/{len(train_loader)}] "
                          f"D: {errD.item():.3f} | G: {errG.item():.3f} "
                          f"(adv:{errG_adv.item():.3f} dice:{errG_dice.item():.3f} "
                          f"color:{errG_color.item():.3f} percep:{errG_perceptual.item():.3f})")

            # ─── LR Step ───
            schedulerG.step()
            schedulerD.step()

            # ─── Validation ───
            avg_iou = validate(encoder, generator, device=device, val_loader=val_loader)
            avg_G = epoch_loss_G / max(n_batches, 1)
            avg_D = epoch_loss_D / max(n_batches, 1)
            print(f"Epoch [{epoch+1}/{end_epoch}] IoU: {avg_iou:.4f} | "
                  f"G: {avg_G:.4f} | D: {avg_D:.4f}")

            # ─── Qualitative Snapshot ───
            if (epoch + 1) == 1 or (epoch + 1) % 10 == 0:
                generate_qualitative_snapshot(encoder, ema_generator, val_loader, epoch + 1, save_dir, device)

            # ─── Checkpoint ───
            state = {
                'epoch': epoch,
                'encoder': encoder.state_dict(),
                'generator': generator.state_dict(),
                'ema_generator': ema_generator.state_dict(),
                'discriminator': discriminator.state_dict(),
                'optimizerG': optimizerG.state_dict(),
                'optimizerD': optimizerD.state_dict(),
                'best_iou': best_iou,
            }
            torch.save(state, os.path.join(save_dir, "latest.pth"))

            if (epoch + 1) % 25 == 0:
                torch.save(state, os.path.join(save_dir, f"checkpoint_epoch_{epoch+1}.pth"))

            if avg_iou > best_iou:
                best_iou = avg_iou
                state['best_iou'] = best_iou
                torch.save(state, os.path.join(save_dir, "best.pth"))
                print(f"  🏆 New best model! IoU: {best_iou:.4f}")

    except KeyboardInterrupt:
        print(f"\n[!] Training interrupted at epoch {current_epoch + 1}. Saving...")
        state = {
            'epoch': current_epoch,
            'encoder': encoder.state_dict(),
            'generator': generator.state_dict(),
            'ema_generator': ema_generator.state_dict(),
            'discriminator': discriminator.state_dict(),
            'optimizerG': optimizerG.state_dict(),
            'optimizerD': optimizerD.state_dict(),
            'best_iou': best_iou,
        }
        torch.save(state, os.path.join(save_dir, "latest.pth"))
        print(f"  ✅ Saved to latest.pth at epoch {current_epoch + 1}")

    elapsed = time.time() - start_time
    print(f"\n✅ Training done in {elapsed:.1f}s")


if __name__ == "__main__":
    train(epochs=500)
