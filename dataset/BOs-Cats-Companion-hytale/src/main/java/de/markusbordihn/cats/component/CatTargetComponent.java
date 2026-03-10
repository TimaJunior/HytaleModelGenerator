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

package de.markusbordihn.cats.component;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.UUIDBinaryCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.markusbordihn.cats.Cats;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CatTargetComponent implements Component<EntityStore> {

  public static final String TARGET_ENTITY_UUID_TAG = "TargetEntityUUID";

  @Nonnull
  public static final BuilderCodec<CatTargetComponent> CODEC =
      BuilderCodec.builder(CatTargetComponent.class, CatTargetComponent::new)
          .append(
              new KeyedCodec<>(TARGET_ENTITY_UUID_TAG, new UUIDBinaryCodec()),
              (component, value) -> component.targetEntityUUID = value,
              component -> component.targetEntityUUID)
          .documentation("The UUID of the target entity to attack")
          .add()
          .build();

  @Nullable private UUID targetEntityUUID;

  public CatTargetComponent() {
    this.targetEntityUUID = null;
  }

  public CatTargetComponent(@Nullable UUID targetEntityUUID) {
    this.targetEntityUUID = targetEntityUUID;
  }

  @Nullable
  public static ComponentType<EntityStore, CatTargetComponent> getComponentType() {
    return Cats.catTargetComponentType;
  }

  @Nullable
  public UUID getTargetEntityUUID() {
    return targetEntityUUID;
  }

  public void setTargetEntityUUID(@Nullable UUID targetEntityUUID) {
    this.targetEntityUUID = targetEntityUUID;
  }

  public boolean hasTarget() {
    return targetEntityUUID != null;
  }

  @Override
  @Nonnull
  public CatTargetComponent clone() {
    return new CatTargetComponent(targetEntityUUID);
  }

  @Override
  public String toString() {
    return "CatTargetComponent{targetEntityUUID=" + targetEntityUUID + '}';
  }
}
