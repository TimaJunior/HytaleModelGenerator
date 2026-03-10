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
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.markusbordihn.cats.component.CatStateComponent;
import de.markusbordihn.cats.data.CatState;
import java.util.logging.Level;
import javax.annotation.Nonnull;

public class CatStateSystem extends RefSystem<EntityStore> {
  private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
  private final ComponentType<EntityStore, CatStateComponent> componentType;

  public CatStateSystem(ComponentType<EntityStore, CatStateComponent> componentType) {
    this.componentType = componentType;
  }

  @Nonnull
  @Override
  public Query<EntityStore> getQuery() {
    return componentType;
  }

  @Override
  public void onEntityAdded(
      @Nonnull Ref<EntityStore> ref,
      @Nonnull AddReason reason,
      @Nonnull Store<EntityStore> store,
      @Nonnull CommandBuffer<EntityStore> commandBuffer) {
    CatStateComponent stateComponent = store.getComponent(ref, componentType);
    if (stateComponent != null) {
      LOGGER.at(Level.FINE).log("Cat state initialized: %s", stateComponent.getState());
    }
  }

  @Override
  public void onEntityRemove(
      @Nonnull Ref<EntityStore> ref,
      @Nonnull RemoveReason reason,
      @Nonnull Store<EntityStore> store,
      @Nonnull CommandBuffer<EntityStore> commandBuffer) {}

  public CatState getState(
      @Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> catRef) {
    CatStateComponent stateComponent = accessor.getComponent(catRef, componentType);
    return stateComponent != null ? stateComponent.getState() : CatState.WANDERING;
  }

  public void setState(
      @Nonnull CommandBuffer<EntityStore> buffer,
      @Nonnull Ref<EntityStore> catRef,
      @Nonnull CatState state) {
    CatStateComponent component = new CatStateComponent(state);
    buffer.putComponent(catRef, componentType, component);
    LOGGER.at(Level.FINE).log("Cat state changed to %s", state);
  }
}
