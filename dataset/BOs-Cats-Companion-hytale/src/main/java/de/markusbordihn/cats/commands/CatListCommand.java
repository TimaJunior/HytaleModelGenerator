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
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.markusbordihn.cats.Constants;
import de.markusbordihn.cats.data.CatDataEntry;
import de.markusbordihn.cats.manager.CatsManager;
import java.util.Collection;
import java.util.UUID;
import javax.annotation.Nonnull;

final class CatListCommand extends CatCommand {

  public CatListCommand() {
    super("list", "Lists all your cats with name, UUID and position");
  }

  @Override
  protected void execute(
      @Nonnull CommandContext context, @Nonnull World world, @Nonnull Store<EntityStore> store) {
    if (!context.isPlayer()) {
      context.sendMessage(
          Message.raw("This command can only be used by players").color(Constants.COLOR_ERROR));
      return;
    }

    UUID playerUuid = context.sender().getUuid();
    if (playerUuid == null) {
      context.sendMessage(Message.raw("Unable to get player UUID").color(Constants.COLOR_ERROR));
      return;
    }

    CatsManager catsManager = CatsManager.getInstance();
    Collection<CatDataEntry> playerCats = catsManager.getCatDataByOwner(playerUuid, store);

    int currentCount = playerCats.size();
    int limit = getCatLimit(context);
    String limitText = limit == -1 ? "unlimited" : String.valueOf(limit);

    if (playerCats.isEmpty()) {
      context.sendMessage(
          Message.raw("=== Your Cats (0/" + limitText + ") ===").color(Constants.COLOR_GOLD));
      context.sendMessage(Message.raw("You don't have any cats yet.").color(Constants.COLOR_GRAY));
      context.sendMessage(
          Message.raw("Tip: Tame a wild cat by giving it raw fish!").color(Constants.COLOR_INFO));
      return;
    }

    context.sendMessage(
        Message.raw("=== Your Cats (" + currentCount + "/" + limitText + ") ===")
            .color(Constants.COLOR_GOLD));

    int index = 1;
    for (CatDataEntry catData : playerCats) {
      String catName =
          catData.name() != null && !catData.name().isEmpty() ? catData.name() : "Unnamed Cat";

      boolean isInWorld = catsManager.getCatByUuid(catData.uuid(), store) != null;
      String statusIndicator =
          !isInWorld
              ? "[DESPAWNED]"
              : catsManager.isCatAliveInWorld(catData.uuid(), store) ? "[ALIVE]" : "[DEAD]";
      String positionStr =
          catData.position() != null
              ? String.format(
                  "(%d,%d,%d)", catData.position().x, catData.position().y, catData.position().z)
              : "(no position)";
      context.sendMessage(
          Message.raw(index + ". " + catName + " " + statusIndicator + " " + positionStr)
              .color(Constants.COLOR_CYAN));
      context.sendMessage(
          Message.raw("   Type: " + catData.catType() + " | State: " + catData.state())
              .color(Constants.COLOR_GRAY));
      context.sendMessage(
          Message.raw("   UUID: " + catData.uuid().toString().substring(0, 8) + "...")
              .color(Constants.COLOR_GRAY));

      index++;
    }

    context.sendMessage(Message.raw(""));
    context.sendMessage(
        Message.raw("Tip: Use /cat info while looking at a cat for more details")
            .color(Constants.COLOR_INFO));
  }
}
