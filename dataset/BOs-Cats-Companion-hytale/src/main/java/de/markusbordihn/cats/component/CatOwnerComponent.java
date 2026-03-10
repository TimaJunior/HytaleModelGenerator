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
import com.hypixel.hytale.codec.codecs.simple.LongCodec;
import com.hypixel.hytale.codec.codecs.simple.StringCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.markusbordihn.cats.Cats;
import de.markusbordihn.cats.data.CatOwnerData;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CatOwnerComponent implements Component<EntityStore> {

  public static final String OWNER_ID_TAG = "OwnerId";
  public static final String OWNER_NAME_TAG = "OwnerName";
  public static final String TAMED_TIMESTAMP_TAG = "TamedTimestamp";

  @Nonnull
  public static final BuilderCodec<CatOwnerComponent> CODEC =
      BuilderCodec.builder(CatOwnerComponent.class, CatOwnerComponent::new)
          .append(
              new KeyedCodec<>(OWNER_ID_TAG, new UUIDBinaryCodec()),
              (component, value) -> component.data = component.data.withOwnerId(value),
              component -> component.data.ownerId())
          .documentation("The UUID of the cat's owner.")
          .add()
          .append(
              new KeyedCodec<>(OWNER_NAME_TAG, new StringCodec()),
              (component, value) -> component.data = component.data.withOwnerName(value),
              component -> component.data.ownerName())
          .documentation("The name of the cat's owner.")
          .add()
          .append(
              new KeyedCodec<>(TAMED_TIMESTAMP_TAG, new LongCodec()),
              (component, value) -> component.data = component.data.withTamedTimestamp(value),
              component -> component.data.tamedTimestamp())
          .documentation("The timestamp when the cat was tamed.")
          .add()
          .build();

  @Nonnull private CatOwnerData data;

  public CatOwnerComponent() {
    this.data = CatOwnerData.empty();
  }

  public CatOwnerComponent(@Nonnull CatOwnerData data) {
    this.data = data;
  }

  public CatOwnerComponent(UUID ownerId, String ownerName) {
    this.data = CatOwnerData.create(ownerId, ownerName);
  }

  public static ComponentType<EntityStore, CatOwnerComponent> getComponentType() {
    return Cats.getInstance().catOwnerComponentType;
  }

  @Nonnull
  public CatOwnerData getData() {
    return data;
  }

  public void setData(@Nonnull CatOwnerData data) {
    this.data = data;
  }

  public boolean hasOwner() {
    return data.hasOwner();
  }

  @Nullable
  public UUID getOwnerUUID() {
    return data.ownerId();
  }

  public void setOwnerId(@Nullable UUID ownerId) {
    this.data = data.withOwnerId(ownerId);
  }

  @Nullable
  public String getOwnerName() {
    return data.ownerName();
  }

  public void setOwnerName(@Nullable String ownerName) {
    this.data = data.withOwnerName(ownerName);
  }

  public long getTamedTimestamp() {
    return data.tamedTimestamp();
  }

  public void setOwner(UUID ownerId, String ownerName) {
    this.data = new CatOwnerData(ownerId, ownerName, System.currentTimeMillis());
  }

  public void clearOwner() {
    this.data = CatOwnerData.empty();
  }

  @Override
  @Nonnull
  public CatOwnerComponent clone() {
    CatOwnerComponent cloned = new CatOwnerComponent();
    cloned.data = this.data;
    return cloned;
  }
}
