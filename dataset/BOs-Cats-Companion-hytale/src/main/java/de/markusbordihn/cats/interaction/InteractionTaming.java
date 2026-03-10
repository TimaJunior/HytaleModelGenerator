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

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.permissions.PermissionHolder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.systems.RoleChangeSystem;
import de.markusbordihn.cats.Constants;
import de.markusbordihn.cats.component.CatTamingProgressComponent;
import de.markusbordihn.cats.data.CatType;
import de.markusbordihn.cats.inventory.InventoryHelper;
import de.markusbordihn.cats.manager.CatsManager;
import de.markusbordihn.cats.manager.CatsNamesManager;
import de.markusbordihn.cats.permission.PermissionManager;
import java.util.UUID;
import java.util.logging.Level;

public class InteractionTaming {
  private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
  private static final long FEEDING_COOLDOWN_MS = 5000;

  public static boolean handle(
      Ref<EntityStore> entityRef,
      Role role,
      Store<EntityStore> store,
      Player player,
      ItemStack heldItem) {
    String itemName = heldItem != null ? heldItem.getItemId() : null;
    InteractionLogger.logInteraction(
        "WILD CAT: Taming Attempt", entityRef, role, store, player, itemName);

    CatTamingProgressComponent progressComponent =
        store.getComponent(entityRef, CatTamingProgressComponent.getComponentType());
    if (progressComponent == null) {
      progressComponent = new CatTamingProgressComponent();
      store.putComponent(
          entityRef, CatTamingProgressComponent.getComponentType(), progressComponent);
      LOGGER.at(Level.FINE).log(
          "Initialized taming progress: requires %d fish", progressComponent.getRequiredProgress());
    }

    if (!progressComponent.canFeedNow(FEEDING_COOLDOWN_MS)) {
      role.getStateSupport().setState(entityRef, "Rejection", "Default", store);
      if (player != null) {
        player.sendMessage(
            Message.translation("cats.interactions.taming.too_soon")
                .param(
                    "seconds",
                    String.valueOf(
                        (progressComponent.getLastFedTimestamp()
                                + FEEDING_COOLDOWN_MS
                                - System.currentTimeMillis())
                            / 1000))
                .color(Constants.COLOR_WARNING));
      }
      return true;
    }

    progressComponent.incrementProgress();
    store.putComponent(entityRef, CatTamingProgressComponent.getComponentType(), progressComponent);

    LOGGER.at(Level.FINE).log(
        "Taming progress: %d/%d",
        progressComponent.getCurrentProgress(), progressComponent.getRequiredProgress());

    if (progressComponent.isReadyToTame()) {
      handleSuccessfulTaming(entityRef, role, store, player, heldItem);
      store.removeComponent(entityRef, CatTamingProgressComponent.getComponentType());
    } else {
      handleProgressFeedback(
          entityRef,
          role,
          store,
          player,
          heldItem,
          progressComponent.getCurrentProgress(),
          progressComponent.getRequiredProgress());
    }

    return false;
  }

  private static void handleProgressFeedback(
      Ref<EntityStore> entityRef,
      Role role,
      Store<EntityStore> store,
      Player player,
      ItemStack heldItem,
      long currentProgress,
      long requiredProgress) {

    role.getStateSupport().setState(entityRef, "Feeding", "Default", store);

    if (player != null) {
      player.sendMessage(
          Message.translation("cats.interactions.taming.progress")
              .param("current", String.valueOf(currentProgress))
              .param("required", String.valueOf(requiredProgress))
              .color(Constants.COLOR_INFO));
      if (currentProgress == 1) {
        player.sendMessage(
            Message.translation("cats.interactions.taming.gaining_trust")
                .color(Constants.COLOR_SUCCESS));
      } else if (currentProgress >= requiredProgress - 1) {
        player.sendMessage(
            Message.translation("cats.interactions.taming.almost_there")
                .color(Constants.COLOR_SUCCESS));
      } else {
        player.sendMessage(
            Message.translation("cats.interactions.taming.likes_food")
                .color(Constants.COLOR_SUCCESS));
      }
    }

    InventoryHelper.consumeActiveHotbarItem(player, heldItem);

    LOGGER.at(Level.FINE).log(
        "Cat fed with %s - progress: %d/%d",
        heldItem != null ? heldItem.getItemId() : null, currentProgress, requiredProgress);
  }

  private static void handleSuccessfulTaming(
      Ref<EntityStore> entityRef,
      Role role,
      Store<EntityStore> store,
      Player player,
      ItemStack heldItem) {
    String itemName = heldItem != null ? heldItem.getItemId() : null;
    if (player == null) {
      LOGGER.at(Level.WARNING).log("Cannot tame cat - player is null");
      return;
    }

    // Get player entity ref from the interaction target (non-deprecated path)
    Ref<EntityStore> playerEntityRef = role.getStateSupport().getInteractionIterationTarget();
    if (playerEntityRef == null || !playerEntityRef.isValid()) {
      LOGGER.at(Level.WARNING).log("Cannot tame cat - player entity ref is null");
      return;
    }

    // Get PlayerRef as an ECS component (replaces deprecated Player.getPlayerRef())
    PlayerRef playerRef = store.getComponent(playerEntityRef, PlayerRef.getComponentType());
    if (playerRef == null) {
      LOGGER.at(Level.WARNING).log("Cannot tame cat - PlayerRef component is null");
      return;
    }

    UUID playerUUID = playerRef.getUuid();
    String username = playerRef.getUsername();
    if (playerUUID == null || username == null) {
      LOGGER.at(Level.WARNING).log("Cannot tame cat - UUID or username not found");
      return;
    }

    CatsManager catsManager = CatsManager.getInstance();
    if (catsManager == null) {
      LOGGER.at(Level.WARNING).log("Cannot tame cat - CatsManager is null");
      player.sendMessage(
          Message.translation("cats.interactions.taming.error_system")
              .color(Constants.COLOR_ERROR));
      return;
    }

    int currentCatCount = catsManager.getCatCountByOwner(playerUUID, store);
    int catLimit = getCatLimit(player);
    if (catLimit >= 0 && currentCatCount >= catLimit) {
      player.sendMessage(
          Message.translation("cats.interactions.taming.limit_reached")
              .param("current", String.valueOf(currentCatCount))
              .param("limit", String.valueOf(catLimit))
              .color(Constants.COLOR_ERROR));
      return;
    }

    UUIDComponent catUuidComponent =
        store.getComponent(entityRef, UUIDComponent.getComponentType());
    UUID catUuid = catUuidComponent != null ? catUuidComponent.getUuid() : null;
    if (catUuid == null) {
      LOGGER.at(Level.WARNING).log("Cannot tame cat - cat UUID not found");
      player.sendMessage(
          Message.translation("cats.interactions.taming.error_no_uuid")
              .color(Constants.COLOR_ERROR));
      return;
    }

    String catName = CatsNamesManager.getRandomName();
    catsManager.assignOwner(entityRef, playerUUID, username, catName, "Taming", store);

    // Change role from Wild to Tamed
    NPCEntity npcEntity = store.getComponent(entityRef, NPCEntity.getComponentType());
    if (npcEntity != null) {
      try {
        Role currentRole = npcEntity.getRole();
        if (currentRole == null) {
          LOGGER.at(Level.WARNING).log("Failed to request role change: currentRole is null");
        } else {
          String tamedRoleName = CatType.fromRoleName(currentRole.getRoleName()).getTamedRoleName();
          if (tamedRoleName.isEmpty()) {
            LOGGER.at(Level.WARNING).log(
                "Failed to get tamed role name for CatType %s",
                CatType.fromRoleName(currentRole.getRoleName()));
          } else if (NPCPlugin.get().getIndex(tamedRoleName) < 0) {
            LOGGER.at(Level.WARNING).log("Failed to find role index for %s", tamedRoleName);
          } else {
            RoleChangeSystem.requestRoleChange(
                entityRef,
                currentRole,
                NPCPlugin.get().getIndex(tamedRoleName),
                true,
                null,
                null,
                store);

            // Clear spawn configuration to prevent the engine's despawn system.
            npcEntity.setSpawnConfiguration(Integer.MIN_VALUE);
            npcEntity.updateSpawnTrackingState(false);

            LOGGER.at(Level.INFO).log(
                "Cat role change requested from %s to %s (spawn tracking disabled)",
                currentRole.getRoleName(), tamedRoleName);
          }
        }
      } catch (Exception e) {
        LOGGER.at(Level.SEVERE).log("Failed to change cat role", e);
      }
    }

    player.sendMessage(
        Message.translation("cats.interactions.taming.success")
            .param("item", itemName)
            .param("catName", catName)
            .color(Constants.COLOR_SUCCESS));
    player.sendMessage(
        Message.translation("cats.interactions.taming.companion").color(Constants.COLOR_WARNING));
    player.sendMessage(
        Message.translation("cats.interactions.taming.help")
            .param("catName", catName)
            .color(Constants.COLOR_INFO));

    if (catLimit >= 0) {
      player.sendMessage(
          Message.translation("cats.interactions.taming.count")
              .param("current", String.valueOf(currentCatCount + 1))
              .param("limit", String.valueOf(catLimit))
              .color(Constants.COLOR_GRAY));
    }

    InventoryHelper.consumeActiveHotbarItem(player, heldItem);

    LOGGER.at(Level.INFO).log(
        "Cat successfully tamed by player %s with item %s (cats: %d/%d)",
        username, itemName, currentCatCount + 1, catLimit);
  }

  private static int getCatLimit(Player player) {
    if (!(player instanceof PermissionHolder permissionHolder)) {
      return Constants.DEFAULT_CAT_LIMIT;
    }
    return PermissionManager.getCatLimit(permissionHolder);
  }
}
