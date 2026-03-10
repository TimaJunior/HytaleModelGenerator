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
import com.hypixel.hytale.codec.codecs.simple.BooleanCodec;
import com.hypixel.hytale.codec.codecs.simple.DoubleCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.markusbordihn.cats.Cats;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CatBedTargetComponent implements Component<EntityStore> {

  private static final String X_TAG = "X";
  private static final String Y_TAG = "Y";
  private static final String Z_TAG = "Z";
  private static final String HAS_TARGET_TAG = "HasTarget";

  @Nonnull
  public static final BuilderCodec<CatBedTargetComponent> CODEC =
      BuilderCodec.builder(CatBedTargetComponent.class, CatBedTargetComponent::new)
          .append(
              new KeyedCodec<>(HAS_TARGET_TAG, new BooleanCodec()),
              (component, value) -> component.hasTarget = value,
              component -> component.hasTarget)
          .documentation("Whether a bed target has been explicitly set")
          .add()
          .append(
              new KeyedCodec<>(X_TAG, new DoubleCodec()),
              (component, value) -> component.x = value,
              component -> component.x)
          .documentation("Target bed X coordinate")
          .add()
          .append(
              new KeyedCodec<>(Y_TAG, new DoubleCodec()),
              (component, value) -> component.y = value,
              component -> component.y)
          .documentation("Target bed Y coordinate")
          .add()
          .append(
              new KeyedCodec<>(Z_TAG, new DoubleCodec()),
              (component, value) -> component.z = value,
              component -> component.z)
          .documentation("Target bed Z coordinate")
          .add()
          .build();

  private double x;
  private double y;
  private double z;
  private boolean hasTarget;

  public CatBedTargetComponent() {
    this.x = 0;
    this.y = 0;
    this.z = 0;
    this.hasTarget = false;
  }

  public CatBedTargetComponent(@Nonnull Vector3d position) {
    this.x = position.x;
    this.y = position.y;
    this.z = position.z;
    this.hasTarget = true;
  }

  public CatBedTargetComponent(double x, double y, double z) {
    this.x = x;
    this.y = y;
    this.z = z;
    this.hasTarget = true;
  }

  @Nullable
  public static ComponentType<EntityStore, CatBedTargetComponent> getComponentType() {
    return Cats.getInstance().catBedTargetComponentType;
  }

  @Nonnull
  public Vector3d getTargetPosition() {
    return new Vector3d(x, y, z);
  }

  public double getX() {
    return x;
  }

  public double getY() {
    return y;
  }

  public double getZ() {
    return z;
  }

  public boolean hasTarget() {
    return hasTarget;
  }

  @Override
  @Nonnull
  public CatBedTargetComponent clone() {
    CatBedTargetComponent clone = new CatBedTargetComponent(x, y, z);
    clone.hasTarget = this.hasTarget;
    return clone;
  }
}
