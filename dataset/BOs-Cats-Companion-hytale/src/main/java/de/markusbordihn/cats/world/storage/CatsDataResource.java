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

package de.markusbordihn.cats.world.storage;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.component.Resource;
import com.hypixel.hytale.component.ResourceType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.markusbordihn.cats.Cats;
import de.markusbordihn.cats.data.CatDataEntry;
import de.markusbordihn.cats.data.CatStatus;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CatsDataResource implements Resource<EntityStore> {

  private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

  public static final BuilderCodec<CatsDataResource> CODEC =
      BuilderCodec.builder(CatsDataResource.class, CatsDataResource::new)
          .append(
              new KeyedCodec<>("Cats", new MapCodec<>(CatDataEntry.CODEC, HashMap::new, false)),
              (resource, map) -> {
                resource.cats = new HashMap<>();
                map.forEach((k, v) -> resource.cats.put(UUID.fromString(k), v));
                LOGGER.at(Level.FINE).log("Loaded %d cats into resource", resource.cats.size());
              },
              resource -> {
                Map<String, CatDataEntry> stringMap = new HashMap<>();
                resource.cats.forEach((k, v) -> stringMap.put(k.toString(), v));
                return stringMap;
              })
          .add()
          .build();

  @Nonnull private Map<UUID, CatDataEntry> cats = new HashMap<>();

  public CatsDataResource() {}

  @Nonnull
  public static ResourceType<EntityStore, CatsDataResource> getResourceType() {
    return Cats.getInstance().catsDataResourceType;
  }

  public void addCat(@Nonnull CatDataEntry catDataEntry) {
    cats.put(catDataEntry.uuid(), catDataEntry);
    LOGGER.at(Level.INFO).log("Added cat: %s", catDataEntry);
  }

  public void updateCat(@Nonnull UUID catUuid, @Nonnull CatDataEntry catDataEntry) {
    cats.put(catUuid, catDataEntry);
    LOGGER.at(Level.FINE).log("Updated cat: %s", catDataEntry);
  }

  public void removeCat(@Nonnull UUID catUuid) {
    CatDataEntry removed = cats.remove(catUuid);
    if (removed != null) {
      LOGGER.at(Level.INFO).log("Removed cat: %s", removed);
    }
  }

  @Nullable
  public CatDataEntry getCat(@Nonnull UUID catUuid) {
    return cats.get(catUuid);
  }

  @Nonnull
  public Set<UUID> getAllCatUuids() {
    return Collections.unmodifiableSet(cats.keySet());
  }

  @Nonnull
  public Set<CatDataEntry> getAllCats() {
    return cats.values().stream().collect(Collectors.toUnmodifiableSet());
  }

  @Nonnull
  public Set<CatDataEntry> getCatsByOwner(@Nonnull UUID ownerUuid) {
    return cats.values().stream()
        .filter(cat -> ownerUuid.equals(cat.ownerUuid()))
        .collect(Collectors.toUnmodifiableSet());
  }

  @Nonnull
  public Set<CatDataEntry> getCatsWithoutOwner() {
    return cats.values().stream()
        .filter(cat -> !cat.hasOwner())
        .collect(Collectors.toUnmodifiableSet());
  }

  @Nonnull
  public Set<CatDataEntry> getSpawnedCats() {
    return cats.values().stream()
        .filter(CatDataEntry::isSpawned)
        .collect(Collectors.toUnmodifiableSet());
  }

  @Nonnull
  public Set<CatDataEntry> getDespawnedCats() {
    return cats.values().stream()
        .filter(cat -> cat.status() == CatStatus.DESPAWNED)
        .collect(Collectors.toUnmodifiableSet());
  }

  public int getTotalCatCount() {
    return cats.size();
  }

  public int getOwnedCatCount(@Nonnull UUID ownerUuid) {
    return (int) cats.values().stream().filter(cat -> ownerUuid.equals(cat.ownerUuid())).count();
  }

  @Nullable
  public UUID findOwnerOfCat(@Nonnull UUID catUuid) {
    CatDataEntry catDataEntry = cats.get(catUuid);
    return catDataEntry != null ? catDataEntry.ownerUuid() : null;
  }

  @Nonnull
  @Override
  public CatsDataResource clone() {
    CatsDataResource cloned = new CatsDataResource();
    cloned.cats = new HashMap<>(this.cats);
    return cloned;
  }
}
