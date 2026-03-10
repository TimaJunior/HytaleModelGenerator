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

package de.markusbordihn.cats.interaction;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.role.Role;
import de.markusbordihn.cats.Constants;
import java.util.Set;
import java.util.logging.Level;

public class ItemInteraction {
  private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

  private static final Set<String> TAMING_ITEMS =
      Set.of(
          "Food_Fish_Raw",
          "Food_Fish_Raw_Uncommon",
          "Food_Fish_Raw_Rare",
          "Food_Fish_Raw_Epic",
          "Food_Fish_Raw_Legendary",
          "Food_Fish_Grilled",
          "Fish_Salmon_Item",
          "Fish_Catfish_Item",
          "Fish_Trout_Rainbow_Item",
          "Fish_Pike_Item",
          "Fish_Bluegill_Item",
          "Fish_Minnow_Item");

  public static boolean handle(
      Ref<EntityStore> entityRef,
      Role role,
      Store<EntityStore> store,
      Player player,
      ItemStack heldItem) {

    String itemName = heldItem != null ? heldItem.getItemId() : null;

    if (itemName != null && TAMING_ITEMS.contains(itemName)) {
      return InteractionTaming.handle(entityRef, role, store, player, heldItem);
    }

    handleRejection(entityRef, role, store, player, heldItem);
    return true;
  }

  private static void handleRejection(
      Ref<EntityStore> entityRef,
      Role role,
      Store<EntityStore> store,
      Player player,
      ItemStack heldItem) {

    String itemName = heldItem != null ? heldItem.getItemId() : null;
    String rejectReason = itemName == null ? "empty hand" : itemName;
    LOGGER.at(Level.FINE).log("Wild cat rejected interaction with: %s", rejectReason);

    // Trigger Rejection animation state (angry particles, returns to Wild after 1 second)
    role.getStateSupport().setState(entityRef, "Rejection", "Default", store);

    if (player != null) {
      player.sendMessage(
          Message.translation("cats.interactions.wild.reject").color(Constants.COLOR_RED_SOFT));
    }
  }
}
