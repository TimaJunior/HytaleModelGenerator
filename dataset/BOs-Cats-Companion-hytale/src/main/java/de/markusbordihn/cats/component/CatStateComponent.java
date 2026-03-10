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
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.markusbordihn.cats.Cats;
import de.markusbordihn.cats.data.CatState;
import de.markusbordihn.cats.data.CatStateData;
import javax.annotation.Nonnull;

public class CatStateComponent implements Component<EntityStore> {

  public static final String STATE_TAG = "State";

  private static final EnumCodec<CatState> STATE_CODEC = new EnumCodec<>(CatState.class);

  @Nonnull
  public static final BuilderCodec<CatStateComponent> CODEC =
      BuilderCodec.builder(CatStateComponent.class, CatStateComponent::new)
          .append(
              new KeyedCodec<>(STATE_TAG, STATE_CODEC),
              (component, value) -> component.data = component.data.withState(value),
              component -> component.data.state())
          .documentation("The current state of the cat (SITTING, SLEEPING, FOLLOWING, etc.).")
          .add()
          .build();

  @Nonnull private CatStateData data;

  public CatStateComponent() {
    this.data = CatStateData.defaultState();
  }

  public CatStateComponent(@Nonnull CatStateData data) {
    this.data = data;
  }

  public CatStateComponent(@Nonnull CatState state) {
    this.data = CatStateData.of(state);
  }

  public static ComponentType<EntityStore, CatStateComponent> getComponentType() {
    return Cats.getInstance().catStateComponentType;
  }

  @Nonnull
  public CatStateData getData() {
    return data;
  }

  public void setData(@Nonnull CatStateData data) {
    this.data = data;
  }

  @Nonnull
  public CatState getState() {
    return data.state();
  }

  public void setState(@Nonnull CatState state) {
    this.data = data.withState(state);
  }

  @Override
  @Nonnull
  public CatStateComponent clone() {
    CatStateComponent cloned = new CatStateComponent();
    cloned.data = this.data;
    return cloned;
  }
}
