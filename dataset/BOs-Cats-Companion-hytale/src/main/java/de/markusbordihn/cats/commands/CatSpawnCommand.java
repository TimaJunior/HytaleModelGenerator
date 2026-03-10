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

package de.markusbordihn.cats.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import de.markusbordihn.cats.Constants;
import de.markusbordihn.cats.component.CatOwnerComponent;
import de.markusbordihn.cats.component.CatStateComponent;
import de.markusbordihn.cats.data.CatDataEntry;
import de.markusbordihn.cats.data.CatType;
import de.markusbordihn.cats.manager.CatsManager;
import it.unimi.dsi.fastutil.Pair;
import java.util.Collection;
import java.util.UUID;
import java.util.logging.Level;
import javax.annotation.Nonnull;

final class CatSpawnCommand extends CatCommand {

  private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
  private static final double MAX_SPAWN_DISTANCE = 32.0;
  @Nonnull private final OptionalArg<String> filterArg;

  public CatSpawnCommand() {
    super("spawn", "Spawns your despawned cats (optionally filter by name or UUID)");
    this.filterArg =
        this.withOptionalArg(
            "name_or_uuid",
            "Optional: cat name or UUID to spawn (use quotes for names with spaces)",
            ArgTypes.STRING);
  }

  private static boolean matchesFilter(@Nonnull CatDataEntry cat, @Nonnull String filterLower) {
    return cat.uuid().toString().toLowerCase(java.util.Locale.ROOT).contains(filterLower)
        || (cat.name() != null
            && cat.name().toLowerCase(java.util.Locale.ROOT).contains(filterLower));
  }

  @Override
  protected void execute(
      @Nonnull CommandContext context, @Nonnull World world, @Nonnull Store<EntityStore> store) {
    if (!context.isPlayer()) {
      context.sendMessage(
          Message.translation("cats.commands.error.player_only").color(Constants.COLOR_ERROR));
      return;
    }

    UUID playerUuid = context.sender().getUuid();
    if (playerUuid == null) {
      context.sendMessage(
          Message.translation("cats.commands.error.no_uuid").color(Constants.COLOR_ERROR));
      return;
    }

    CatsManager catsManager = CatsManager.getInstance();

    String filter = this.filterArg.get(context);
    if (filter != null && !filter.isEmpty()) {
      String filterLower = filter.toLowerCase(java.util.Locale.ROOT);

      Collection<CatDataEntry> allCats = catsManager.getCatDataByOwner(playerUuid, store);
      for (CatDataEntry cat : allCats) {
        Ref<EntityStore> catRef = catsManager.getCatByUuid(cat.uuid(), store);
        boolean isInWorld = catRef != null;
        boolean isAlive = isInWorld && catsManager.isCatAliveInWorld(cat.uuid(), store);

        if (isInWorld && isAlive && matchesFilter(cat, filterLower)) {
          if (catRef.isValid()) {
            TransformComponent catTransform =
                store.getComponent(catRef, TransformComponent.getComponentType());
            if (catTransform != null) {
              Ref<EntityStore> playerRef = store.getExternalData().getRefFromUUID(playerUuid);
              if (playerRef != null && playerRef.isValid()) {
                TransformComponent playerTransform =
                    store.getComponent(playerRef, TransformComponent.getComponentType());
                if (playerTransform != null) {
                  Vector3d playerPos = playerTransform.getPosition();
                  catTransform.setPosition(playerPos);

                  String catName = cat.name() != null ? cat.name() : "Cat";
                  context.sendMessage(
                      Message.raw("The cat " + catName + " has been teleported to you!")
                          .color(Constants.COLOR_SUCCESS));
                  return;
                }
              }
            }
          }
        }
      }
    }

    Collection<CatDataEntry> despawnedCats =
        catsManager.getCatDataByOwner(playerUuid, store).stream()
            .filter(
                cat -> {
                  boolean isInWorld = catsManager.getCatByUuid(cat.uuid(), store) != null;
                  boolean isAlive = isInWorld && catsManager.isCatAliveInWorld(cat.uuid(), store);
                  return !isInWorld || !isAlive;
                })
            .toList();

    if (filter != null && !filter.isEmpty()) {
      String filterLower = filter.toLowerCase(java.util.Locale.ROOT);
      despawnedCats =
          despawnedCats.stream().filter(cat -> matchesFilter(cat, filterLower)).toList();

      if (despawnedCats.isEmpty()) {
        context.sendMessage(
            Message.translation("cats.commands.spawn.no_match")
                .param("filter", filter)
                .color(Constants.COLOR_WARNING));
        return;
      }
    }

    if (despawnedCats.isEmpty()) {
      context.sendMessage(
          Message.translation("cats.commands.spawn.no_despawned").color(Constants.COLOR_INFO));
      return;
    }

    Ref<EntityStore> playerRef = store.getExternalData().getRefFromUUID(playerUuid);
    if (playerRef == null || !playerRef.isValid()) {
      context.sendMessage(
          Message.translation("cats.commands.error.player_pos_not_found")
              .color(Constants.COLOR_ERROR));
      return;
    }

    TransformComponent playerTransform =
        store.getComponent(playerRef, TransformComponent.getComponentType());
    if (playerTransform == null) {
      context.sendMessage(
          Message.translation("cats.commands.error.player_pos_not_found")
              .color(Constants.COLOR_ERROR));
      return;
    }

    Vector3d playerPos = playerTransform.getPosition();
    int spawnedCount = 0;
    for (CatDataEntry catData : despawnedCats) {
      Vector3d spawnPos = calculateSpawnPosition(catData, playerPos);
      if (spawnCat(catData, spawnPos, world, store)) {
        spawnedCount++;
      }
    }

    if (spawnedCount > 0) {
      context.sendMessage(
          Message.translation("cats.commands.spawn.success")
              .param("count", spawnedCount)
              .color(Constants.COLOR_SUCCESS));
    } else {
      context.sendMessage(
          Message.translation("cats.commands.spawn.failed").color(Constants.COLOR_ERROR));
    }
  }

  @Nonnull
  private Vector3d calculateSpawnPosition(
      @Nonnull CatDataEntry catData, @Nonnull Vector3d playerPos) {
    if (catData.position() != null) {
      Vector3d savedPos =
          new Vector3d(catData.position().x, catData.position().y, catData.position().z);
      double dx = playerPos.x - savedPos.x;
      double dy = playerPos.y - savedPos.y;
      double dz = playerPos.z - savedPos.z;
      double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

      if (distance <= MAX_SPAWN_DISTANCE) {
        return savedPos;
      }
    }

    return playerPos.add(Math.random() * 4 - 2, 0, Math.random() * 4 - 2);
  }

  private boolean spawnCat(
      @Nonnull CatDataEntry catData,
      @Nonnull Vector3d position,
      @Nonnull World world,
      @Nonnull Store<EntityStore> store) {

    CatsManager catsManager = CatsManager.getInstance();
    Ref<EntityStore> existingCat = catsManager.getCatByUuid(catData.uuid(), store);
    if (existingCat != null && existingCat.isValid()) {
      LOGGER.at(Level.WARNING).log(
          "Cat %s (UUID: %s) is already spawned, skipping", catData.name(), catData.uuid());
      return false;
    }

    if (catData.catType() == CatType.UNKNOWN || catData.catType().getRoleName().isEmpty()) {
      return false;
    }

    try {
      NPCPlugin npcPlugin = NPCPlugin.get();
      if (npcPlugin == null) {
        LOGGER.at(Level.WARNING).log("Cannot spawn cat: NPCPlugin is not available");
        return false;
      }

      String roleName = catData.catType().getRoleName();
      int roleIndex = npcPlugin.getIndex(roleName);
      if (roleIndex < 0) {
        LOGGER.at(Level.WARNING).log("Cannot spawn cat: Role '" + roleName + "' not found");
        return false;
      }

      Vector3f rotation = new Vector3f();
      Pair<Ref<EntityStore>, NPCEntity> spawnResult =
          npcPlugin.spawnEntity(store, roleIndex, position, rotation, null, null, null);

      if (spawnResult == null || spawnResult.left() == null || !spawnResult.left().isValid()) {
        LOGGER.at(Level.WARNING).log(
            "Failed to spawn cat: NPCPlugin.spawnEntity returned null or invalid reference");
        return false;
      }

      Ref<EntityStore> catRef = spawnResult.left();

      // Update UUID, if needed
      UUID newEntityUuid = catsManager.getUuid(catRef, store);
      if (newEntityUuid != null && !newEntityUuid.equals(catData.uuid())) {
        catsManager.updateCatUuid(catData.uuid(), newEntityUuid, store);
      }

      // Set owner
      if (catData.ownerUuid() != null) {
        CatOwnerComponent ownerComponent =
            new CatOwnerComponent(catData.ownerUuid(), catData.ownerName());
        store.putComponent(catRef, CatOwnerComponent.getComponentType(), ownerComponent);
      }

      // Set name in Nameplate
      if (catData.name() != null && !catData.name().isEmpty()) {
        store
            .ensureAndGetComponent(
                catRef,
                com.hypixel.hytale.server.core.entity.nameplate.Nameplate.getComponentType())
            .setText(catData.name());
      }

      // Set state
      if (catData.state() != null) {
        CatStateComponent stateComponent = new CatStateComponent(catData.state());
        store.putComponent(catRef, CatStateComponent.getComponentType(), stateComponent);
      }

      return true;
    } catch (Exception e) {
      LOGGER.at(Level.WARNING).withCause(e).log("Failed to spawn cat: " + catData.name());
      return false;
    }
  }
}
