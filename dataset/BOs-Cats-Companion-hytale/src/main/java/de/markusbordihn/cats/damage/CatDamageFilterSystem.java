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

package de.markusbordihn.cats.damage;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemGroupDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.AllLegacyLivingEntityTypesQuery;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.markusbordihn.cats.component.CatOwnerComponent;
import de.markusbordihn.cats.config.ProtectionConfig;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CatDamageFilterSystem extends DamageEventSystem {

  private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

  @Override
  @Nullable
  public SystemGroup<EntityStore> getGroup() {
    DamageModule damageModule = DamageModule.get();
    return damageModule != null ? damageModule.getFilterDamageGroup() : null;
  }

  @Override
  @Nonnull
  public Query<EntityStore> getQuery() {
    return AllLegacyLivingEntityTypesQuery.INSTANCE;
  }

  @Override
  @Nonnull
  public Set<Dependency<EntityStore>> getDependencies() {
    DamageModule damageModule = DamageModule.get();
    if (damageModule == null) {
      return Set.of();
    }
    SystemGroup<EntityStore> gatherGroup = damageModule.getGatherDamageGroup();
    if (gatherGroup == null) {
      return Set.of();
    }
    return Set.of(new SystemGroupDependency<>(Order.AFTER, gatherGroup));
  }

  @Override
  public void handle(
      int index,
      @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
      @Nonnull Store<EntityStore> store,
      @Nonnull CommandBuffer<EntityStore> commandBuffer,
      @Nonnull Damage damage) {

    if (damage.isCancelled()) {
      return;
    }

    Ref<EntityStore> targetRef = archetypeChunk.getReferenceTo(index);
    if (targetRef == null || !targetRef.isValid()) {
      return;
    }

    CatOwnerComponent ownerComponent =
        store.getComponent(targetRef, CatOwnerComponent.getComponentType());
    if (ownerComponent == null || !ownerComponent.hasOwner()) {
      return;
    }

    UUID ownerUuid = ownerComponent.getOwnerUUID();
    Damage.Source source = damage.getSource();

    switch (source) {
      case Damage.ProjectileSource projectileSource -> {
        if (!ProtectionConfig.DAMAGE_FROM_PROJECTILES) {
          damage.setCancelled(true);
          LOGGER.at(Level.FINE).log("Blocked projectile damage to tamed cat");
          return;
        }

        Ref<EntityStore> shooterRef = projectileSource.getRef();
        if (shooterRef != null && shooterRef.isValid()) {
          if (isPlayerEntity(shooterRef, store)) {
            UUID shooterUuid = getEntityUuid(shooterRef, store);
            if (shooterUuid != null && shooterUuid.equals(ownerUuid)) {
              if (!ProtectionConfig.DAMAGE_FROM_OWNER) {
                damage.setCancelled(true);
                LOGGER.at(Level.FINE).log("Blocked owner's projectile damage to tamed cat");
              }
            } else {
              if (!ProtectionConfig.DAMAGE_FROM_PLAYERS) {
                damage.setCancelled(true);
                LOGGER.at(Level.FINE).log("Blocked player's projectile damage to tamed cat");
              }
            }
          }
        }
      }
      case Damage.EntitySource entitySource -> {
        Ref<EntityStore> attackerRef = entitySource.getRef();
        if (attackerRef != null && attackerRef.isValid()) {
          if (isPlayerEntity(attackerRef, store)) {
            UUID attackerUuid = getEntityUuid(attackerRef, store);
            if (attackerUuid != null && attackerUuid.equals(ownerUuid)) {
              if (!ProtectionConfig.DAMAGE_FROM_OWNER) {
                damage.setCancelled(true);
                LOGGER.at(Level.FINE).log("Blocked owner damage to tamed cat");
              }
            } else {
              if (!ProtectionConfig.DAMAGE_FROM_PLAYERS) {
                damage.setCancelled(true);
                LOGGER.at(Level.FINE).log("Blocked player damage to tamed cat");
              }
            }
          } else {
            if (!ProtectionConfig.DAMAGE_FROM_MOBS) {
              damage.setCancelled(true);
              LOGGER.at(Level.FINE).log("Blocked mob damage to tamed cat");
            }
          }
        }
      }
      case Damage.EnvironmentSource environmentSource -> {
        if (!ProtectionConfig.DAMAGE_FROM_ENVIRONMENT) {
          damage.setCancelled(true);
          LOGGER.at(Level.FINE).log("Blocked environment damage to tamed cat");
        }
      }
      case Damage.CommandSource commandSource -> {}
      default -> {
        if (!ProtectionConfig.DAMAGE_FROM_MOBS) {
          damage.setCancelled(true);
          LOGGER.at(Level.FINE).log("Blocked unknown source damage to tamed cat");
        }
      }
    }
  }

  private boolean isPlayerEntity(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
    Player player = store.getComponent(ref, Player.getComponentType());
    return player != null;
  }

  @Nullable
  private UUID getEntityUuid(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
    UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
    return uuidComponent != null ? uuidComponent.getUuid() : null;
  }
}
