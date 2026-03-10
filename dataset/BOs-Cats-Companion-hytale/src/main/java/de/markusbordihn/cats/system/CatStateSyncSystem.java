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

package de.markusbordihn.cats.system;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.support.StateSupport;
import de.markusbordihn.cats.component.CatMoodComponent;
import de.markusbordihn.cats.component.CatStateComponent;
import de.markusbordihn.cats.data.CatDataEntry;
import de.markusbordihn.cats.data.CatState;
import de.markusbordihn.cats.data.MoodData;
import de.markusbordihn.cats.manager.CatsManager;
import java.util.logging.Level;
import javax.annotation.Nonnull;

public class CatStateSyncSystem extends RefSystem<EntityStore> {
  private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
  private final ComponentType<EntityStore, CatStateComponent> componentType;

  public CatStateSyncSystem(ComponentType<EntityStore, CatStateComponent> componentType) {
    this.componentType = componentType;
  }

  @Nonnull
  @Override
  public Query<EntityStore> getQuery() {
    return componentType;
  }

  @Override
  public void onEntityAdded(
      @Nonnull Ref<EntityStore> entityRef,
      @Nonnull AddReason addReason,
      @Nonnull Store<EntityStore> store,
      @Nonnull CommandBuffer<EntityStore> commandBuffer) {

    CatStateComponent stateComponent = store.getComponent(entityRef, componentType);
    if (stateComponent == null) {
      return;
    }

    NPCEntity npcEntity = store.getComponent(entityRef, NPCEntity.getComponentType());
    if (npcEntity == null || npcEntity.getRole() == null) {
      return;
    }

    try {
      StateSupport stateSupport = npcEntity.getRole().getStateSupport();
      CatState state = stateComponent.getState();
      String substate =
          switch (state) {
            case SITTING -> "Sitting";
            case SLEEPING -> "Sleeping";
            case PLAYING -> "Playing";
            case SEARCHING -> "Searching";
            case WAITING -> "Waiting";
            case WANDERING -> "Wandering";
            case FOLLOWING -> "Default";
            case ATTACKING -> "Attacking";
            default -> null;
          };

      if (substate != null) {
        stateSupport.setState(entityRef, "Pet", substate, store);
        LOGGER.at(Level.FINE).log("Synced cat to %s substate", substate);
      } else {
        LOGGER.at(Level.WARNING).log("Unknown cat state: %s", state);
      }
    } catch (Exception e) {
      LOGGER.at(Level.WARNING).log("Failed to sync cat state: " + e.getMessage());
    }

    CatsManager catsManager = CatsManager.getInstance();
    if (catsManager != null) {
      CatDataEntry catData = catsManager.getCatData(entityRef, store);
      if (catData != null && catData.hasOwner()) {
        commandBuffer.putComponent(
            entityRef,
            CatMoodComponent.getComponentType(),
            new CatMoodComponent(new MoodData(catData.happiness(), catData.lastMoodUpdate())));
        if (catData.personalityType() == null) {
          catsManager.assignPersonality(entityRef, store);
        }
      }
    }
  }

  @Override
  public void onEntityRemove(
      @Nonnull Ref<EntityStore> entityRef,
      @Nonnull RemoveReason removeReason,
      @Nonnull Store<EntityStore> store,
      @Nonnull CommandBuffer<EntityStore> commandBuffer) {}
}
