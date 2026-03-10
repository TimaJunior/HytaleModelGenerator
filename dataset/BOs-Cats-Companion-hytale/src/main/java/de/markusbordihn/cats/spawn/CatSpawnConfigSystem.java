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

package de.markusbordihn.cats.spawn;

import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.spawning.assets.spawns.config.RoleSpawnParameters;
import com.hypixel.hytale.server.spawning.assets.spawns.config.WorldNPCSpawn;
import de.markusbordihn.cats.config.SpawnConfig;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.logging.Level;
import javax.annotation.Nonnull;

public class CatSpawnConfigSystem {

  private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
  private static final String CAT_SPAWN_PREFIX = "Spawns_Zone";
  private static final String CAT_SPAWN_IDENTIFIER = "_Cats";

  private CatSpawnConfigSystem() {}

  @SuppressWarnings("unused")
  public static void onWorldNPCSpawnsLoaded(
      @Nonnull LoadedAssetsEvent<String, WorldNPCSpawn, ?> event) {

    if (!SpawnConfig.getInstance().isLoaded()) {
      return;
    }

    Map<String, WorldNPCSpawn> loadedAssets = event.getLoadedAssets();
    if (loadedAssets == null || loadedAssets.isEmpty()) {
      return;
    }

    int modifiedCount = 0;
    for (Map.Entry<String, WorldNPCSpawn> entry : loadedAssets.entrySet()) {
      String key = entry.getKey();
      WorldNPCSpawn spawnConfig = entry.getValue();

      if (!isCatSpawnConfig(key)) {
        continue;
      }

      if (!SpawnConfig.SPAWN_ENABLED) {
        disableSpawnWeights(spawnConfig, key);
        modifiedCount++;
        LOGGER.at(Level.INFO).log("Disabled cat spawn config: %s", key);
      } else if (Math.abs(SpawnConfig.SPAWN_WEIGHT_MULTIPLIER - 1.0f) > 0.001f) {
        applyWeightMultiplier(spawnConfig, key, SpawnConfig.SPAWN_WEIGHT_MULTIPLIER);
        modifiedCount++;
        LOGGER.at(Level.INFO).log(
            "Applied spawn weight multiplier %.2f to: %s",
            SpawnConfig.SPAWN_WEIGHT_MULTIPLIER, key);
      }
    }

    if (modifiedCount > 0) {
      LOGGER.at(Level.INFO).log(
          "Modified %d cat spawn configuration(s) (enabled=%s, multiplier=%.2f)",
          modifiedCount, SpawnConfig.SPAWN_ENABLED, SpawnConfig.SPAWN_WEIGHT_MULTIPLIER);
    }
  }

  private static boolean isCatSpawnConfig(@Nonnull String key) {
    return key.contains(CAT_SPAWN_IDENTIFIER)
        && (key.startsWith(CAT_SPAWN_PREFIX) || key.contains("Cats"));
  }

  private static void disableSpawnWeights(@Nonnull WorldNPCSpawn spawnConfig, @Nonnull String key) {
    RoleSpawnParameters[] npcs = spawnConfig.getNPCs();
    if (npcs == null || npcs.length == 0) {
      return;
    }

    for (RoleSpawnParameters npc : npcs) {
      setWeight(npc, 0, key);
    }
  }

  private static void applyWeightMultiplier(
      @Nonnull WorldNPCSpawn spawnConfig, @Nonnull String key, float multiplier) {
    RoleSpawnParameters[] npcs = spawnConfig.getNPCs();
    if (npcs == null || npcs.length == 0) {
      return;
    }

    for (RoleSpawnParameters npc : npcs) {
      int originalWeight = getWeight(npc);
      int newWeight = Math.max(1, Math.round(originalWeight * multiplier));
      setWeight(npc, newWeight, key);
    }
  }

  private static int getWeight(@Nonnull RoleSpawnParameters params) {
    try {
      Field weightField = findField(params.getClass(), "weight", "Weight");
      if (weightField != null) {
        weightField.setAccessible(true);
        return weightField.getInt(params);
      }
    } catch (Exception e) {
      LOGGER.at(Level.FINE).log(
          "Could not read weight from RoleSpawnParameters: %s", e.getMessage());
    }
    return 1;
  }

  private static void setWeight(
      @Nonnull RoleSpawnParameters params, int weight, @Nonnull String configKey) {
    try {
      Field weightField = findField(params.getClass(), "weight", "Weight");
      if (weightField != null) {
        weightField.setAccessible(true);
        weightField.setInt(params, weight);
      } else {
        LOGGER.at(Level.WARNING).log(
            "Could not find weight field in RoleSpawnParameters for config: %s", configKey);
      }
    } catch (Exception e) {
      LOGGER.at(Level.WARNING).log(
          "Failed to set weight for spawn config %s: %s", configKey, e.getMessage());
    }
  }

  private static Field findField(@Nonnull Class<?> clazz, @Nonnull String... names) {
    for (String name : names) {
      try {
        return clazz.getDeclaredField(name);
      } catch (NoSuchFieldException ignored) {
      }
    }

    Class<?> superClass = clazz.getSuperclass();
    if (superClass != null && superClass != Object.class) {
      for (String name : names) {
        try {
          return superClass.getDeclaredField(name);
        } catch (NoSuchFieldException ignored) {
        }
      }
    }
    return null;
  }
}
