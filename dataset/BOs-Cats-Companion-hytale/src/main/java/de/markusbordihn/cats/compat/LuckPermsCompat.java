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

package de.markusbordihn.cats.compat;

import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.PluginManager;
import java.util.logging.Level;

public class LuckPermsCompat {

  private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
  private static final PluginIdentifier LUCKPERMS_ID =
      new PluginIdentifier("LuckPerms", "LuckPerms");

  private static boolean isAvailable = false;
  private static boolean isDetected = false;

  private LuckPermsCompat() {
    // Utility class
  }

  public static void detect() {
    if (isDetected) {
      return;
    }
    isDetected = true;

    if (PluginManager.get().getAvailablePlugins().containsKey(LUCKPERMS_ID)) {
      isAvailable = true;
      logLuckPermsEnabled();
    }
  }

  public static boolean isAvailable() {
    return isAvailable;
  }

  private static void logLuckPermsEnabled() {
    LOGGER.at(Level.INFO).log(
        "===============================================================================");
    LOGGER.at(Level.INFO).log(
        "| [Cats Plugin] LuckPerms detected! Permission checks are ENABLED.           |");
    LOGGER.at(Level.INFO).log(
        "|=============================================================================|");
    LOGGER.at(Level.INFO).log(
        "| IMPORTANT: Use wildcard permission to grant access to /cat command         |");
    LOGGER.at(Level.INFO).log(
        "| and all sub-commands (/cat list, /cat info, etc.)                          |");
    LOGGER.at(Level.INFO).log(
        "|                                                                             |");
    LOGGER.at(Level.INFO).log(
        "| Recommended: Grant wildcard to player or group:                            |");
    LOGGER.at(Level.INFO).log(
        "|   /lp user <player> permission set markusbordihn.cats.command.cat.* true   |");
    LOGGER.at(Level.INFO).log(
        "|   /lp group default permission set markusbordihn.cats.command.cat.* true   |");
    LOGGER.at(Level.INFO).log(
        "|                                                                             |");
    LOGGER.at(Level.INFO).log(
        "| Alternative: Grant individual permissions:                                 |");
    LOGGER.at(Level.INFO).log(
        "|   /lp user <player> permission set markusbordihn.cats.command.cat true     |");
    LOGGER.at(Level.INFO).log(
        "|   /lp user <player> permission set markusbordihn.cats.command.cat.list true|");
    LOGGER.at(Level.INFO).log(
        "|   ...and so on for each sub-command                                        |");
    LOGGER.at(Level.INFO).log(
        "|                                                                             |");
    LOGGER.at(Level.INFO).log(
        "| Verify permissions were set:                                               |");
    LOGGER.at(Level.INFO).log(
        "|   /lp user <player> permission info                                        |");
    LOGGER.at(Level.INFO).log(
        "|                                                                             |");
    LOGGER.at(Level.INFO).log(
        "| Available permissions:                                                     |");
    LOGGER.at(Level.INFO).log(
        "|   - markusbordihn.cats.command.cat          - Base /cat command            |");
    LOGGER.at(Level.INFO).log(
        "|   - markusbordihn.cats.command.cat.*        - Wildcard: all sub-commands   |");
    LOGGER.at(Level.INFO).log(
        "|   - markusbordihn.cats.command.cat.attack   - /cat attack                  |");
    LOGGER.at(Level.INFO).log(
        "|   - markusbordihn.cats.command.cat.bed      - /cat bed                     |");
    LOGGER.at(Level.INFO).log(
        "|   - markusbordihn.cats.command.cat.despawn  - /cat despawn (admin)         |");
    LOGGER.at(Level.INFO).log(
        "|   - markusbordihn.cats.command.cat.follow   - /cat follow                  |");
    LOGGER.at(Level.INFO).log(
        "|   - markusbordihn.cats.command.cat.info     - /cat info                    |");
    LOGGER.at(Level.INFO).log(
        "|   - markusbordihn.cats.command.cat.list     - /cat list                    |");
    LOGGER.at(Level.INFO).log(
        "|   - markusbordihn.cats.command.cat.name     - /cat name                    |");
    LOGGER.at(Level.INFO).log(
        "|   - markusbordihn.cats.command.cat.owner    - /cat owner (admin)           |");
    LOGGER.at(Level.INFO).log(
        "|   - markusbordihn.cats.command.cat.play     - /cat play                    |");
    LOGGER.at(Level.INFO).log(
        "|   - markusbordihn.cats.command.cat.release  - /cat release                 |");
    LOGGER.at(Level.INFO).log(
        "|   - markusbordihn.cats.command.cat.search   - /cat search                  |");
    LOGGER.at(Level.INFO).log(
        "|   - markusbordihn.cats.command.cat.sit      - /cat sit                     |");
    LOGGER.at(Level.INFO).log(
        "|   - markusbordihn.cats.command.cat.sleep    - /cat sleep                   |");
    LOGGER.at(Level.INFO).log(
        "|   - markusbordihn.cats.command.cat.spawn    - /cat spawn (admin)           |");
    LOGGER.at(Level.INFO).log(
        "|   - markusbordihn.cats.command.cat.wait     - /cat wait                    |");
    LOGGER.at(Level.INFO).log(
        "|   - markusbordihn.cats.command.cat.wander   - /cat wander                  |");
    LOGGER.at(Level.INFO).log(
        "|   - markusbordihn.cats.admin.bypass         - Bypass ownership checks      |");
    LOGGER.at(Level.INFO).log(
        "|   - markusbordihn.cats.limit.unlimited      - Unlimited cat ownership      |");
    LOGGER.at(Level.INFO).log(
        "|   - markusbordihn.cats.limit.{number}       - Limit to N cats (e.g. .8-32) |");
    LOGGER.at(Level.INFO).log(
        "===============================================================================");
  }
}
