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

import com.hypixel.hytale.codec.codecs.UUIDBinaryCodec;
import com.hypixel.hytale.codec.codecs.simple.StringCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.metadata.CapturedNPCMetadata;
import com.hypixel.hytale.server.npc.role.Role;
import de.markusbordihn.cats.Constants;
import de.markusbordihn.cats.component.CatOwnerComponent;
import de.markusbordihn.cats.component.CatStateComponent;
import de.markusbordihn.cats.data.CatDataEntry;
import de.markusbordihn.cats.data.CatType;
import de.markusbordihn.cats.manager.CatsManager;
import it.unimi.dsi.fastutil.Pair;
import java.util.UUID;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CatCarrierInteraction {

  private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

  private static final UUIDBinaryCodec UUID_CODEC = new UUIDBinaryCodec();
  private static final StringCodec STRING_CODEC = new StringCodec();

  private static final String META_CAT_UUID = "CatUUID";
  private static final String META_CAT_NAME = "CatName";
  private static final String FULL_CARRIER_ICON = "Icons/ItemsGenerated/Cat_Carrier_Full.png";

  private CatCarrierInteraction() {}

  public static boolean handleCapture(
      @Nonnull Ref<EntityStore> entityRef,
      @Nonnull Role role,
      @Nonnull Store<EntityStore> store,
      @Nonnull Player player,
      @Nonnull ItemStack heldItem) {

    InteractionLogger.logInteraction(
        "CAT CARRIER CAPTURE", entityRef, role, store, player, Constants.CAT_CARRIER_ITEM);

    UUID storedCatUuid = heldItem.getFromMetadataOrNull(META_CAT_UUID, UUID_CODEC);
    if (storedCatUuid != null) {
      String storedName = heldItem.getFromMetadataOrNull(META_CAT_NAME, STRING_CODEC);
      player.sendMessage(
          Message.translation("cats.interactions.carrier.already_occupied")
              .param("catName", storedName != null ? storedName : "a cat")
              .param("catUuid", storedCatUuid.toString())
              .color(Constants.COLOR_WARNING));
      return true;
    }

    CatOwnerComponent ownerComponent =
        store.getComponent(entityRef, CatOwnerComponent.getComponentType());
    if (ownerComponent == null || !ownerComponent.hasOwner()) {
      player.sendMessage(
          Message.translation("cats.interactions.carrier.not_tamed")
              .color(Constants.COLOR_WARNING));
      return true;
    }

    UUID playerUuid = player.getUuid();
    if (playerUuid == null || !playerUuid.equals(ownerComponent.getOwnerUUID())) {
      player.sendMessage(
          Message.translation("cats.interactions.carrier.not_owner").color(Constants.COLOR_ERROR));
      return true;
    }

    CatsManager catsManager = CatsManager.getInstance();
    UUID catUuid = catsManager.getUuid(entityRef, store);
    if (catUuid == null) {
      player.sendMessage(
          Message.translation("cats.interactions.carrier.error").color(Constants.COLOR_ERROR));
      return true;
    }

    Nameplate nameplate = store.getComponent(entityRef, Nameplate.getComponentType());
    String catName = nameplate != null ? nameplate.getText() : null;

    ItemStack updatedItem = heldItem.withMetadata(META_CAT_UUID, UUID_CODEC, catUuid);
    if (catName != null && !catName.isEmpty()) {
      updatedItem = updatedItem.withMetadata(META_CAT_NAME, STRING_CODEC, catName);
    }

    try {
      CapturedNPCMetadata capturedMeta = new CapturedNPCMetadata();
      capturedMeta.setFullItemIcon(FULL_CARRIER_ICON);

      String displayNameForMeta = catName != null && !catName.isEmpty() ? catName : "Cat";
      capturedMeta.setNpcNameKey(displayNameForMeta);

      PersistentModel persistentModel =
          store.getComponent(entityRef, PersistentModel.getComponentType());
      if (persistentModel != null && persistentModel.getModelReference() != null) {
        ModelAsset modelAsset =
            ModelAsset.getAssetMap()
                .getAsset(persistentModel.getModelReference().getModelAssetId());
        if (modelAsset != null && modelAsset.getIcon() != null) {
          capturedMeta.setIconPath(modelAsset.getIcon());
        }
      }

      updatedItem = updatedItem.withMetadata(CapturedNPCMetadata.KEYED_CODEC, capturedMeta);
    } catch (Exception e) {
      LOGGER.at(Level.WARNING).withCause(e).log(
          "Failed to set CapturedNPCMetadata on carrier, capture will proceed without icon swap");
    }

    catsManager.despawnCat(entityRef, store);

    NPCEntity npcEntity = store.getComponent(entityRef, NPCEntity.getComponentType());
    if (npcEntity != null) {
      npcEntity.setDespawning(true);
      npcEntity.setDespawnRemainingSeconds(0.0f);
    }

    updateHeldItem(player, heldItem, updatedItem);

    String displayName = catName != null && !catName.isEmpty() ? catName : "Cat";
    player.sendMessage(
        Message.translation("cats.interactions.carrier.captured")
            .param("catName", displayName)
            .param("catUuid", catUuid.toString())
            .color(Constants.COLOR_SUCCESS));

    LOGGER.at(Level.INFO).log(
        "Player %s captured cat %s (UUID: %s) into carrier",
        player.getUuid(), displayName, catUuid);

    return true;
  }

  public static boolean handleRelease(
      @Nonnull Store<EntityStore> store, @Nonnull Player player, @Nonnull ItemStack heldItem) {
    return handleRelease(store, player, heldItem, null);
  }

  public static boolean handleRelease(
      @Nonnull Store<EntityStore> store,
      @Nonnull Player player,
      @Nonnull ItemStack heldItem,
      @Nullable Vector3d targetPos) {

    UUID storedCatUuid = heldItem.getFromMetadataOrNull(META_CAT_UUID, UUID_CODEC);
    if (storedCatUuid == null) {
      LOGGER.at(Level.FINE).log("handleRelease: carrier is empty (no CatUUID metadata)");
      return false;
    }

    String storedName = heldItem.getFromMetadataOrNull(META_CAT_NAME, STRING_CODEC);
    LOGGER.at(Level.INFO).log(
        "handleRelease: releasing cat %s (UUID: %s)", storedName, storedCatUuid);

    UUID playerUuid = player.getUuid();
    if (playerUuid == null) {
      player.sendMessage(
          Message.translation("cats.interactions.carrier.error").color(Constants.COLOR_ERROR));
      return true;
    }

    CatsManager catsManager = CatsManager.getInstance();
    CatDataEntry catData = catsManager.getCatData(storedCatUuid, store);
    if (catData == null) {
      player.sendMessage(
          Message.translation("cats.interactions.carrier.error").color(Constants.COLOR_ERROR));
      LOGGER.at(Level.WARNING).log("Cannot release cat: no data found for UUID %s", storedCatUuid);
      return true;
    }

    if (catData.ownerUuid() != null && !playerUuid.equals(catData.ownerUuid())) {
      player.sendMessage(
          Message.translation("cats.interactions.carrier.not_owner").color(Constants.COLOR_ERROR));
      return true;
    }

    Vector3d spawnPos;
    if (targetPos != null) {
      spawnPos =
          new Vector3d(targetPos.getX() + 0.5, targetPos.getY() + 1.0, targetPos.getZ() + 0.5);
    } else {
      Ref<EntityStore> playerRef = store.getExternalData().getRefFromUUID(playerUuid);
      if (playerRef == null || !playerRef.isValid()) {
        player.sendMessage(
            Message.translation("cats.interactions.carrier.error").color(Constants.COLOR_ERROR));
        return true;
      }
      TransformComponent playerTransform =
          store.getComponent(playerRef, TransformComponent.getComponentType());
      if (playerTransform == null) {
        player.sendMessage(
            Message.translation("cats.interactions.carrier.error").color(Constants.COLOR_ERROR));
        return true;
      }
      Vector3d playerPos = playerTransform.getPosition();
      spawnPos = playerPos.add(Math.random() * 4 - 2, 0, Math.random() * 4 - 2);
    }

    if (!spawnCatFromData(catData, spawnPos, store)) {
      player.sendMessage(
          Message.translation("cats.interactions.carrier.release_failed")
              .color(Constants.COLOR_ERROR));
      return true;
    }

    ItemStack emptyCarrier = heldItem.withMetadata(null);
    updateHeldItem(player, heldItem, emptyCarrier);

    String displayName = storedName != null && !storedName.isEmpty() ? storedName : "Cat";
    player.sendMessage(
        Message.translation("cats.interactions.carrier.released")
            .param("catName", displayName)
            .color(Constants.COLOR_SUCCESS));

    LOGGER.at(Level.INFO).log(
        "Player %s released cat %s from carrier at (%s)", player.getUuid(), displayName, spawnPos);

    return true;
  }

  public static boolean hasStoredCat(@Nullable ItemStack heldItem) {
    if (heldItem == null) {
      return false;
    }
    return heldItem.getFromMetadataOrNull(META_CAT_UUID, UUID_CODEC) != null;
  }

  private static boolean spawnCatFromData(
      @Nonnull CatDataEntry catData,
      @Nonnull Vector3d position,
      @Nonnull Store<EntityStore> store) {

    if (catData.catType() == CatType.UNKNOWN || catData.catType().getTamedRoleName().isEmpty()) {
      return false;
    }

    try {
      NPCPlugin npcPlugin = NPCPlugin.get();
      if (npcPlugin == null) {
        LOGGER.at(Level.WARNING).log("Cannot spawn cat from carrier: NPCPlugin is not available");
        return false;
      }

      String roleName = catData.catType().getRoleName();
      int roleIndex = npcPlugin.getIndex(roleName);
      if (roleIndex < 0) {
        LOGGER.at(Level.WARNING).log(
            "Cannot spawn cat from carrier: Role '%s' not found", roleName);
        return false;
      }

      Vector3f rotation = new Vector3f();
      Pair<Ref<EntityStore>, NPCEntity> spawnResult =
          npcPlugin.spawnEntity(store, roleIndex, position, rotation, null, null, null);

      if (spawnResult == null || spawnResult.left() == null || !spawnResult.left().isValid()) {
        LOGGER.at(Level.WARNING).log(
            "Failed to spawn cat from carrier: spawnEntity returned null or invalid");
        return false;
      }

      Ref<EntityStore> catRef = spawnResult.left();
      CatsManager catsManager = CatsManager.getInstance();

      UUID newEntityUuid = catsManager.getUuid(catRef, store);
      if (newEntityUuid != null && !newEntityUuid.equals(catData.uuid())) {
        catsManager.updateCatUuid(catData.uuid(), newEntityUuid, store);
      }

      if (catData.ownerUuid() != null) {
        CatOwnerComponent ownerComponent =
            new CatOwnerComponent(catData.ownerUuid(), catData.ownerName());
        store.putComponent(catRef, CatOwnerComponent.getComponentType(), ownerComponent);
      }

      if (catData.name() != null && !catData.name().isEmpty()) {
        store.ensureAndGetComponent(catRef, Nameplate.getComponentType()).setText(catData.name());
      }

      if (catData.state() != null) {
        CatStateComponent stateComponent = new CatStateComponent(catData.state());
        store.putComponent(catRef, CatStateComponent.getComponentType(), stateComponent);
      }

      // Disable spawn tracking to prevent the engine's despawn system.
      NPCEntity spawnedNpcEntity = spawnResult.right();
      if (spawnedNpcEntity != null) {
        spawnedNpcEntity.setSpawnConfiguration(Integer.MIN_VALUE);
        spawnedNpcEntity.updateSpawnTrackingState(false);
      }

      return true;
    } catch (Exception e) {
      LOGGER.at(Level.WARNING).withCause(e).log("Failed to spawn cat from carrier");
      return false;
    }
  }

  private static void updateHeldItem(
      @Nonnull Player player, @Nonnull ItemStack oldItem, @Nonnull ItemStack newItem) {
    Inventory inventory = player.getInventory();
    if (inventory == null) {
      LOGGER.at(Level.WARNING).log("Cannot update held item: player inventory is null");
      return;
    }

    byte activeSlot = inventory.getActiveHotbarSlot();
    try {
      inventory.getHotbar().replaceItemStackInSlot(activeSlot, oldItem, newItem);
      LOGGER.at(Level.INFO).log("Updated carrier in hotbar slot %d", activeSlot);
    } catch (Exception e) {
      LOGGER.at(Level.WARNING).withCause(e).log(
          "replaceItemStackInSlot failed, falling back to remove+add");
      inventory.getHotbar().removeItemStackFromSlot(activeSlot, 1);
      inventory.getHotbar().addItemStackToSlot(activeSlot, newItem);
    }
  }
}
