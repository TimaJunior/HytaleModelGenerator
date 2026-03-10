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

package de.markusbordihn.cats.config;

import java.util.LinkedHashMap;
import java.util.Properties;

public class SpawnConfig extends Config {

  private static final boolean DEFAULT_SPAWN_ENABLED = true;
  private static final float DEFAULT_SPAWN_WEIGHT_MULTIPLIER = 1.0f;

  public static boolean SPAWN_ENABLED = DEFAULT_SPAWN_ENABLED;
  public static float SPAWN_WEIGHT_MULTIPLIER = DEFAULT_SPAWN_WEIGHT_MULTIPLIER;

  private static SpawnConfig instance;

  public SpawnConfig() {
    super("spawn.cfg");
  }

  public static void initialize() {
    if (instance == null) {
      instance = new SpawnConfig();
    }
    instance.init();
  }

  public static void reloadConfig() {
    if (instance != null) {
      instance.reload();
    }
  }

  public static SpawnConfig getInstance() {
    return instance;
  }

  @Override
  protected String getHeader() {
    return "Cats Companion - Spawn Configuration";
  }

  @Override
  protected LinkedHashMap<String, String> getDefaults() {
    LinkedHashMap<String, String> defaults = new LinkedHashMap<>();
    defaults.put("spawn.enabled", String.valueOf(DEFAULT_SPAWN_ENABLED));
    defaults.put("spawn.weight_multiplier", String.valueOf(DEFAULT_SPAWN_WEIGHT_MULTIPLIER));
    return defaults;
  }

  @Override
  protected LinkedHashMap<String, String> getComments() {
    LinkedHashMap<String, String> comments = new LinkedHashMap<>();
    comments.put(
        "spawn.enabled",
        "Enable or disable wild cat spawning entirely.\nSet to false to prevent any wild cats from spawning in the world.\nDefault: true");
    comments.put(
        "spawn.weight_multiplier",
        "Global multiplier for cat spawn weights.\nValues < 1.0 reduce spawn frequency, values > 1.0 increase it.\n0.0 = no spawns, 0.5 = half the normal rate, 2.0 = double the rate.\nRange: 0.0 - 5.0\nDefault: 1.0");
    return comments;
  }

  @Override
  protected void applyProperties(Properties props) {
    SPAWN_ENABLED = parseBoolean(props, "spawn.enabled", DEFAULT_SPAWN_ENABLED);
    SPAWN_WEIGHT_MULTIPLIER =
        Math.max(
            0.0f,
            Math.min(
                5.0f,
                parseFloat(props, "spawn.weight_multiplier", DEFAULT_SPAWN_WEIGHT_MULTIPLIER)));
  }

  @Override
  protected void applyDefaults() {
    SPAWN_ENABLED = DEFAULT_SPAWN_ENABLED;
    SPAWN_WEIGHT_MULTIPLIER = DEFAULT_SPAWN_WEIGHT_MULTIPLIER;
  }
}
