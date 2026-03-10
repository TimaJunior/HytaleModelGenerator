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

package de.markusbordihn.cats.sensors;

import com.google.gson.JsonElement;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.SensorBase;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderSensorBase;
import com.hypixel.hytale.server.npc.instructions.Sensor;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import de.markusbordihn.cats.component.CatOwnerComponent;
import javax.annotation.Nonnull;

public class BuilderSensorIsCatTamed extends BuilderSensorBase {
  public static final String SENSOR_ID = "CatTamed";

  @Nonnull
  @Override
  public Sensor build(BuilderSupport support) {
    return new SensorIsCatTamed(this, support);
  }

  @Nonnull
  @Override
  public String getShortDescription() {
    return "Checks if the cat is tamed (has an owner)";
  }

  @Nonnull
  @Override
  public String getLongDescription() {
    return "Returns true if the cat has an owner component with a valid owner UUID";
  }

  @Nonnull
  @Override
  public BuilderSensorIsCatTamed readConfig(@Nonnull JsonElement data) {
    return this;
  }

  @Nonnull
  @Override
  public BuilderDescriptorState getBuilderDescriptorState() {
    return BuilderDescriptorState.Stable;
  }

  public static class SensorIsCatTamed extends SensorBase {
    public SensorIsCatTamed(BuilderSensorBase builder, BuilderSupport support) {
      super(builder);
    }

    @Override
    public boolean matches(
        @Nonnull Ref<EntityStore> entityRef,
        @Nonnull Role role,
        double dt,
        @Nonnull Store<EntityStore> store) {
      if (!super.matches(entityRef, role, dt, store)) {
        return false;
      }

      CatOwnerComponent ownerComponent =
          store.getComponent(entityRef, CatOwnerComponent.getComponentType());
      return ownerComponent != null && ownerComponent.hasOwner();
    }

    @Override
    public InfoProvider getSensorInfo() {
      return null;
    }
  }
}
