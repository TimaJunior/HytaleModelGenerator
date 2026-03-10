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
import com.hypixel.hytale.codec.codecs.simple.IntegerCodec;
import com.hypixel.hytale.codec.codecs.simple.LongCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.markusbordihn.cats.Cats;
import de.markusbordihn.cats.data.HappinessLevel;
import de.markusbordihn.cats.data.MoodData;
import javax.annotation.Nonnull;

public class CatMoodComponent implements Component<EntityStore> {

  public static final String HAPPINESS_TAG = "Happiness";
  public static final String LAST_UPDATE_TAG = "LastMoodUpdate";

  private static final IntegerCodec INT_CODEC = new IntegerCodec();
  private static final LongCodec LONG_CODEC = new LongCodec();

  @Nonnull
  public static final BuilderCodec<CatMoodComponent> CODEC =
      BuilderCodec.builder(CatMoodComponent.class, CatMoodComponent::new)
          .append(
              new KeyedCodec<>(HAPPINESS_TAG, INT_CODEC),
              (component, value) -> component.data = component.data.withHappiness(value),
              component -> component.data.happiness())
          .documentation("The happiness value of the cat (0-100).")
          .add()
          .append(
              new KeyedCodec<>(LAST_UPDATE_TAG, LONG_CODEC),
              (component, value) -> component.data = component.data.withLastMoodUpdate(value),
              component -> component.data.lastMoodUpdate())
          .documentation("Timestamp of the last mood update.")
          .add()
          .build();

  @Nonnull private MoodData data;

  public CatMoodComponent() {
    this.data = MoodData.defaultMood();
  }

  public CatMoodComponent(@Nonnull MoodData data) {
    this.data = data;
  }

  public CatMoodComponent(int happiness) {
    this.data = new MoodData(happiness, System.currentTimeMillis());
  }

  public static ComponentType<EntityStore, CatMoodComponent> getComponentType() {
    return Cats.getInstance().catMoodComponentType;
  }

  @Nonnull
  public MoodData getData() {
    return data;
  }

  public void setData(@Nonnull MoodData data) {
    this.data = data;
  }

  public int getHappiness() {
    return data.happiness();
  }

  @Nonnull
  public HappinessLevel getLevel() {
    return data.getLevel();
  }

  public void adjustHappiness(int delta) {
    this.data = data.adjustHappiness(delta);
  }

  @Override
  @Nonnull
  public CatMoodComponent clone() {
    return new CatMoodComponent(this.data);
  }
}
