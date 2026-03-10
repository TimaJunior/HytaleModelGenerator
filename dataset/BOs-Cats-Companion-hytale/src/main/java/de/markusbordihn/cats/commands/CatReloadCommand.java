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

package de.markusbordihn.cats.commands;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.markusbordihn.cats.Constants;
import de.markusbordihn.cats.config.GeneralConfig;
import de.markusbordihn.cats.config.ProtectionConfig;
import de.markusbordihn.cats.config.SpawnConfig;
import de.markusbordihn.cats.manager.CatsNamesManager;
import java.util.logging.Level;
import javax.annotation.Nonnull;

final class CatReloadCommand extends CatCommand {

  private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

  public CatReloadCommand() {
    super("reload", "Reloads the cats configuration files");
  }

  @Override
  protected void execute(
      @Nonnull CommandContext context, @Nonnull World world, @Nonnull Store<EntityStore> store) {

    try {
      checkPermissionAlways(context, "cats.admin.reload");
    } catch (Exception e) {
      context.sendMessage(
          Message.raw("You don't have permission to reload the configuration.")
              .color(Constants.COLOR_ERROR));
      return;
    }

    try {
      LOGGER.at(Level.INFO).log("Reloading cats configuration...");

      GeneralConfig.reloadConfig();
      SpawnConfig.reloadConfig();
      ProtectionConfig.reloadConfig();
      CatsNamesManager.reload();

      LOGGER.at(Level.INFO).log("Cats configuration reloaded successfully");
      context.sendMessage(
          Message.translation("cats.commands.reload.success").color(Constants.COLOR_SUCCESS));
    } catch (Exception e) {
      LOGGER.at(Level.SEVERE).withCause(e).log("Failed to reload cats configuration");
      context.sendMessage(
          Message.translation("cats.commands.reload.failed").color(Constants.COLOR_ERROR));
    }
  }
}
