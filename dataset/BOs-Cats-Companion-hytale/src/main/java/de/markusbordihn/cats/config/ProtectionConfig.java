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

public class ProtectionConfig extends Config {

  private static final boolean DEFAULT_DAMAGE_FROM_OWNER = false;
  private static final boolean DEFAULT_DAMAGE_FROM_PLAYERS = false;
  private static final boolean DEFAULT_DAMAGE_FROM_MOBS = true;
  private static final boolean DEFAULT_DAMAGE_FROM_ENVIRONMENT = true;
  private static final boolean DEFAULT_DAMAGE_FROM_PROJECTILES = true;

  public static boolean DAMAGE_FROM_OWNER = DEFAULT_DAMAGE_FROM_OWNER;
  public static boolean DAMAGE_FROM_PLAYERS = DEFAULT_DAMAGE_FROM_PLAYERS;
  public static boolean DAMAGE_FROM_MOBS = DEFAULT_DAMAGE_FROM_MOBS;
  public static boolean DAMAGE_FROM_ENVIRONMENT = DEFAULT_DAMAGE_FROM_ENVIRONMENT;
  public static boolean DAMAGE_FROM_PROJECTILES = DEFAULT_DAMAGE_FROM_PROJECTILES;

  private static ProtectionConfig instance;

  public ProtectionConfig() {
    super("protection.cfg");
  }

  public static void initialize() {
    if (instance == null) {
      instance = new ProtectionConfig();
    }
    instance.init();
  }

  public static void reloadConfig() {
    if (instance != null) {
      instance.reload();
    }
  }

  public static ProtectionConfig getInstance() {
    return instance;
  }

  @Override
  protected String getHeader() {
    return "Cats Companion - Protection Configuration";
  }

  @Override
  protected LinkedHashMap<String, String> getDefaults() {
    LinkedHashMap<String, String> defaults = new LinkedHashMap<>();
    defaults.put("damage.from_owner", String.valueOf(DEFAULT_DAMAGE_FROM_OWNER));
    defaults.put("damage.from_players", String.valueOf(DEFAULT_DAMAGE_FROM_PLAYERS));
    defaults.put("damage.from_mobs", String.valueOf(DEFAULT_DAMAGE_FROM_MOBS));
    defaults.put("damage.from_environment", String.valueOf(DEFAULT_DAMAGE_FROM_ENVIRONMENT));
    defaults.put("damage.from_projectiles", String.valueOf(DEFAULT_DAMAGE_FROM_PROJECTILES));
    return defaults;
  }

  @Override
  protected LinkedHashMap<String, String> getComments() {
    LinkedHashMap<String, String> comments = new LinkedHashMap<>();
    comments.put(
        "damage.from_owner",
        "Allow the cat's owner to damage their own tamed cats.\n"
            + "When set to false (default), owner damage is completely blocked,\n"
            + "including melee attacks and projectiles from the owner.\n"
            + "Default: false (protected)");
    comments.put(
        "damage.from_players",
        "Allow other players (non-owners) to damage tamed cats.\n"
            + "When set to false (default), tamed cats are protected from all other players.\n"
            + "Default: false (protected)");
    comments.put(
        "damage.from_mobs", "Allow mobs (hostile NPCs) to damage tamed cats.\nDefault: true");
    comments.put(
        "damage.from_environment",
        "Allow environment damage (fall, drowning, fire, etc.) to tamed cats.\nDefault: true");
    comments.put(
        "damage.from_projectiles",
        "Allow projectile damage (arrows, thrown items, etc.) to tamed cats.\nDefault: true");
    return comments;
  }

  @Override
  protected void applyProperties(Properties props) {
    DAMAGE_FROM_OWNER = parseBoolean(props, "damage.from_owner", DEFAULT_DAMAGE_FROM_OWNER);
    DAMAGE_FROM_PLAYERS = parseBoolean(props, "damage.from_players", DEFAULT_DAMAGE_FROM_PLAYERS);
    DAMAGE_FROM_MOBS = parseBoolean(props, "damage.from_mobs", DEFAULT_DAMAGE_FROM_MOBS);
    DAMAGE_FROM_ENVIRONMENT =
        parseBoolean(props, "damage.from_environment", DEFAULT_DAMAGE_FROM_ENVIRONMENT);
    DAMAGE_FROM_PROJECTILES =
        parseBoolean(props, "damage.from_projectiles", DEFAULT_DAMAGE_FROM_PROJECTILES);
  }

  @Override
  protected void applyDefaults() {
    DAMAGE_FROM_OWNER = DEFAULT_DAMAGE_FROM_OWNER;
    DAMAGE_FROM_PLAYERS = DEFAULT_DAMAGE_FROM_PLAYERS;
    DAMAGE_FROM_MOBS = DEFAULT_DAMAGE_FROM_MOBS;
    DAMAGE_FROM_ENVIRONMENT = DEFAULT_DAMAGE_FROM_ENVIRONMENT;
    DAMAGE_FROM_PROJECTILES = DEFAULT_DAMAGE_FROM_PROJECTILES;
  }
}
