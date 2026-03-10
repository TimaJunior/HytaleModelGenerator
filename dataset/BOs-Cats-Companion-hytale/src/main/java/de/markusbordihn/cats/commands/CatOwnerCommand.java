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

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.types.EntityWrappedArg;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.markusbordihn.cats.Constants;
import de.markusbordihn.cats.manager.CatsManager;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nonnull;

final class CatOwnerCommand extends CatCommand {
  @Nonnull private final EntityWrappedArg entityArg;
  @Nonnull private final RequiredArg<String> ownerArg;

  public CatOwnerCommand() {
    super("owner", "Sets the owner of a cat NPC");
    this.entityArg = this.withOptionalArg("entity", "The cat entity to modify", ArgTypes.ENTITY_ID);
    this.ownerArg = this.withRequiredArg("owner", "The new owner (player name)", ArgTypes.STRING);
  }

  @Override
  protected void execute(
      @Nonnull CommandContext context, @Nonnull World world, @Nonnull Store<EntityStore> store) {

    checkPermissionAlways(context, "markusbordihn.cats.command.cat.owner");

    String ownerName = this.ownerArg.get(context);
    Optional<Ref<EntityStore>> entityOpt = getEntityFromArgument(this.entityArg, store, context);
    if (entityOpt.isEmpty()) {
      context.sendMessage(Message.raw("No entity in view.").color(Constants.COLOR_ERROR));
      context.sendMessage(
          Message.raw("Look at a cat and use: /cat owner <player>").color(Constants.COLOR_GRAY));
      return;
    }

    UUID newOwnerId = null;
    for (PlayerRef playerRef : world.getPlayerRefs()) {
      if (playerRef != null && ownerName.equalsIgnoreCase(playerRef.getUsername())) {
        newOwnerId = playerRef.getUuid();
        break;
      }
    }

    if (newOwnerId == null) {
      context.sendMessage(
          Message.raw("Player '" + ownerName + "' not found online!").color(Constants.COLOR_ERROR));
      return;
    }

    CatsManager catsManager = CatsManager.getInstance();
    if (catsManager != null) {
      catsManager.assignOwner(entityOpt.get(), newOwnerId, ownerName, null, "Pet", store);
    }

    context.sendMessage(Message.raw("Owner set to: " + ownerName).color(Constants.COLOR_SUCCESS));
  }
}
