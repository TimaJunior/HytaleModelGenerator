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
import java.util.Random;
import javax.annotation.Nonnull;

public class CatTamingProgressComponent implements Component<EntityStore> {

  public static final String CURRENT_PROGRESS_TAG = "CurrentProgress";
  public static final String REQUIRED_PROGRESS_TAG = "RequiredProgress";
  public static final String LAST_FED_TIMESTAMP_TAG = "LastFedTimestamp";
  private static final Random RANDOM = new Random();
  private static final int MIN_REQUIRED_FISH = 2;
  private static final int MAX_REQUIRED_FISH = 5;

  @Nonnull
  public static final BuilderCodec<CatTamingProgressComponent> CODEC =
      BuilderCodec.builder(CatTamingProgressComponent.class, CatTamingProgressComponent::new)
          .append(
              new KeyedCodec<>(CURRENT_PROGRESS_TAG, new IntegerCodec()),
              (component, value) -> component.currentProgress = value,
              component -> component.currentProgress)
          .documentation("The current taming progress (number of fish fed).")
          .add()
          .append(
              new KeyedCodec<>(REQUIRED_PROGRESS_TAG, new IntegerCodec()),
              (component, value) -> component.requiredProgress = value,
              component -> component.requiredProgress)
          .documentation("The required progress to tame the cat (random between 2-5).")
          .add()
          .append(
              new KeyedCodec<>(LAST_FED_TIMESTAMP_TAG, new LongCodec()),
              (component, value) -> component.lastFedTimestamp = value,
              component -> component.lastFedTimestamp)
          .documentation("The timestamp when the cat was last fed.")
          .add()
          .build();

  private int currentProgress;
  private int requiredProgress;
  private long lastFedTimestamp;

  public CatTamingProgressComponent() {
    this.currentProgress = 0;
    this.requiredProgress =
        MIN_REQUIRED_FISH + RANDOM.nextInt(MAX_REQUIRED_FISH - MIN_REQUIRED_FISH + 1);
    this.lastFedTimestamp = 0L;
  }

  public static ComponentType<EntityStore, CatTamingProgressComponent> getComponentType() {
    return Cats.getInstance().catTamingProgressComponentType;
  }

  public int getCurrentProgress() {
    return currentProgress;
  }

  public int getRequiredProgress() {
    return requiredProgress;
  }

  public long getLastFedTimestamp() {
    return lastFedTimestamp;
  }

  public void incrementProgress() {
    this.currentProgress++;
    this.lastFedTimestamp = System.currentTimeMillis();
  }

  public boolean isReadyToTame() {
    return currentProgress >= requiredProgress;
  }

  public boolean canFeedNow(long cooldownMillis) {
    return (System.currentTimeMillis() - lastFedTimestamp) >= cooldownMillis;
  }

  @Override
  @Nonnull
  public CatTamingProgressComponent clone() {
    CatTamingProgressComponent cloned = new CatTamingProgressComponent();
    cloned.currentProgress = this.currentProgress;
    cloned.requiredProgress = this.requiredProgress;
    cloned.lastFedTimestamp = this.lastFedTimestamp;
    return cloned;
  }
}
