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
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.types.EntityWrappedArg;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.markusbordihn.cats.Constants;
import de.markusbordihn.cats.data.CatDataEntry;
import de.markusbordihn.cats.data.HappinessLevel;
import de.markusbordihn.cats.manager.CatsManager;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nonnull;

final class CatInfoCommand extends CatCommand {
  @Nonnull private final EntityWrappedArg entityArg;

  public CatInfoCommand() {
    super("info", "Shows information about a cat NPC");
    this.entityArg = this.withOptionalArg("entity", "The cat entity to check", ArgTypes.ENTITY_ID);
  }

  @Override
  protected void execute(
      @Nonnull CommandContext context, @Nonnull World world, @Nonnull Store<EntityStore> store) {

    Optional<Ref<EntityStore>> entityOpt = getEntityFromArgument(this.entityArg, store, context);
    if (entityOpt.isEmpty()) {
      context.sendMessage(Message.raw("No entity in view.").color(Constants.COLOR_ERROR));
      context.sendMessage(
          Message.raw("Look at a cat and use: /cat info").color(Constants.COLOR_GRAY));
      return;
    }

    Ref<EntityStore> entityRef = entityOpt.get();

    // Get UUID (persistent identifier)
    CatsManager catsManager = CatsManager.getInstance();
    UUID catUuid = catsManager.getUuid(entityRef, store);
    if (catUuid == null) {
      context.sendMessage(
          Message.raw("Unable to get cat UUID (not a cat entity?)").color(Constants.COLOR_ERROR));
      return;
    }

    CatDataEntry catData = catsManager.getCatData(catUuid, store);
    if (catData == null) {
      context.sendMessage(
          Message.raw("Cat data not found for UUID: " + catUuid).color(Constants.COLOR_ERROR));
      return;
    }

    UUID playerUuid = context.sender().getUuid();
    boolean isOwner = playerUuid != null && playerUuid.equals(catData.ownerUuid());

    if (catData.name() != null && !catData.name().isEmpty()) {
      context.sendMessage(Message.raw("Name: " + catData.name()).color(Constants.COLOR_GOLD));
    } else {
      context.sendMessage(Message.raw("Name: (unnamed)").color(Constants.COLOR_GRAY));
    }

    context.sendMessage(Message.raw("UUID: " + catUuid.toString()).color(Constants.COLOR_GRAY));
    context.sendMessage(Message.raw("Type: " + catData.catType()).color(Constants.COLOR_INFO));

    if (catData.hasOwner()) {
      context.sendMessage(Message.raw("Status: Tamed").color(Constants.COLOR_SUCCESS));
      String ownerName = catData.ownerName() != null ? catData.ownerName() : "Unknown";
      context.sendMessage(Message.raw("Owner: " + ownerName).color(Constants.COLOR_INFO));
    } else {
      context.sendMessage(Message.raw("Status: Wild").color(Constants.COLOR_WARNING));
    }

    if (isOwner) {
      String stateColor =
          switch (catData.state()) {
            case SITTING -> Constants.COLOR_ORANGE;
            case SLEEPING -> Constants.COLOR_PURPLE;
            case FOLLOWING -> Constants.COLOR_SUCCESS;
            case PLAYING -> Constants.COLOR_PINK;
            case SEARCHING -> Constants.COLOR_GOLD;
            case WAITING -> Constants.COLOR_SKY_BLUE;
            default -> Constants.COLOR_INFO;
          };
      context.sendMessage(Message.raw("State: " + catData.state()).color(stateColor));
      context.sendMessage(
          Message.raw("Spawn Status: " + catData.status()).color(Constants.COLOR_GRAY));

      if (catData.personalityType() != null) {
        String personalityText = catData.personalityType().name();
        if (catData.secondaryPersonality() != null) {
          personalityText += " / " + catData.secondaryPersonality().name();
        }
        context.sendMessage(
            Message.raw("Personality: " + personalityText).color(Constants.COLOR_LAVENDER));
      }

      HappinessLevel happinessLevel = HappinessLevel.fromValue(catData.happiness());
      String moodColor =
          switch (happinessLevel) {
            case ECSTATIC -> Constants.COLOR_GOLD;
            case HAPPY -> Constants.COLOR_SUCCESS;
            case NEUTRAL -> Constants.COLOR_INFO;
            case SAD -> Constants.COLOR_ORANGE;
            case MISERABLE -> Constants.COLOR_ERROR;
          };
      context.sendMessage(
          Message.raw("Mood: " + happinessLevel.name() + " (" + catData.happiness() + "/100)")
              .color(moodColor));

      if (catData.totalGifts() > 0) {
        context.sendMessage(
            Message.raw("Gifts Given: " + catData.totalGifts()).color(Constants.COLOR_GOLD));
      }

      if (catData.position() != null) {
        String position =
            String.format(
                "%d, %d, %d", catData.position().x, catData.position().y, catData.position().z);
        context.sendMessage(Message.raw("Last Position: " + position).color(Constants.COLOR_GRAY));
      }
    }

    context.sendMessage(Message.raw(""));
    context.sendMessage(
        Message.raw("Tip: Use /cat sit, /cat sleep, /cat follow, /cat wait, /cat play")
            .color(Constants.COLOR_INFO));
  }
}
