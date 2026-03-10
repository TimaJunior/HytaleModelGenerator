/*
 * Copyright 2026 Markus Bordihn
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.markusbordihn.cats.interaction;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.EntityUtils;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.LivingEntity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.logging.Level;
import javax.annotation.Nonnull;

public class UseCatCarrierInteraction extends SimpleInteraction {

  @Nonnull
  public static final BuilderCodec<UseCatCarrierInteraction> CODEC =
      BuilderCodec.builder(
              UseCatCarrierInteraction.class,
              UseCatCarrierInteraction::new,
              SimpleInteraction.CODEC)
          .appendInherited(
              new KeyedCodec<>("FullIcon", Codec.STRING),
              (o, v) -> o.fullIcon = v,
              o -> o.fullIcon,
              (o, p) -> o.fullIcon = p.fullIcon)
          .add()
          .build();

  private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
  protected String fullIcon;

  public UseCatCarrierInteraction() {}

  @Override
  protected void tick0(
      boolean firstRun,
      float time,
      @Nonnull InteractionType type,
      @Nonnull InteractionContext context,
      @Nonnull CooldownHandler cooldownHandler) {

    if (!firstRun) {
      return;
    }

    ItemStack item = context.getHeldItem();
    if (item == null) {
      context.getState().state = InteractionState.Failed;
      super.tick0(firstRun, time, type, context, cooldownHandler);
      return;
    }

    // Only handle release, if carrier is empty, fail (capture is handled by NPC actions)
    if (!CatCarrierInteraction.hasStoredCat(item)) {
      context.getState().state = InteractionState.Failed;
      super.tick0(firstRun, time, type, context, cooldownHandler);
      return;
    }

    Ref<EntityStore> ref = context.getEntity();
    var commandBuffer = context.getCommandBuffer();
    if (commandBuffer == null) {
      context.getState().state = InteractionState.Failed;
      super.tick0(firstRun, time, type, context, cooldownHandler);
      return;
    }

    if (!(EntityUtils.getEntity(ref, commandBuffer) instanceof LivingEntity livingEntity)) {
      context.getState().state = InteractionState.Failed;
      super.tick0(firstRun, time, type, context, cooldownHandler);
      return;
    }

    Player player = commandBuffer.getComponent(ref, Player.getComponentType());
    if (player == null) {
      context.getState().state = InteractionState.Failed;
      super.tick0(firstRun, time, type, context, cooldownHandler);
      return;
    }

    Inventory inventory = livingEntity.getInventory();
    ItemStack heldItem = inventory.getActiveHotbarItem();
    if (heldItem == null || !CatCarrierInteraction.hasStoredCat(heldItem)) {
      context.getState().state = InteractionState.Failed;
      super.tick0(firstRun, time, type, context, cooldownHandler);
      return;
    }

    // Determine target position from target block (if clicking on a block)
    final Vector3d targetPos;
    BlockPosition targetBlock = context.getTargetBlock();
    if (targetBlock != null) {
      targetPos = new Vector3d(targetBlock.x, targetBlock.y, targetBlock.z);
      LOGGER.at(Level.INFO).log(
          "UseCatCarrier: releasing at target block (%d, %d, %d)",
          targetBlock.x, targetBlock.y, targetBlock.z);
    } else {
      targetPos = null;
      LOGGER.at(Level.INFO).log("UseCatCarrier: releasing near player (no target block)");
    }

    commandBuffer.run(
        deferredStore -> {
          boolean released =
              CatCarrierInteraction.handleRelease(deferredStore, player, heldItem, targetPos);
          if (released) {
            LOGGER.at(Level.INFO).log("UseCatCarrier: release successful");
          } else {
            LOGGER.at(Level.INFO).log("UseCatCarrier: release failed");
          }
        });

    context.getState().state = InteractionState.Finished;

    super.tick0(firstRun, time, type, context, cooldownHandler);
  }

  @Override
  protected void simulateTick0(
      boolean firstRun,
      float time,
      @Nonnull InteractionType type,
      @Nonnull InteractionContext context,
      @Nonnull CooldownHandler cooldownHandler) {
    // No client-side simulation needed
  }
}
