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
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.role.Role;
import de.markusbordihn.cats.Constants;
import java.util.Set;

public class ItemInteractionOwner {

  private static final Set<String> FOOD_ITEMS =
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
          "Fish_Minnow_Item",
          Constants.CAT_TREATS_ITEM);

  public static boolean handle(
      Ref<EntityStore> entityRef,
      Role role,
      Store<EntityStore> store,
      Player player,
      ItemStack heldItem) {

    String itemName = heldItem != null ? heldItem.getItemId() : null;

    if (itemName != null && FOOD_ITEMS.contains(itemName)) {
      return InteractionFeeding.handle(entityRef, role, store, player, heldItem, true);
    }

    if (Constants.CAT_YARN_BALL_ITEM.equals(itemName)) {
      return InteractionPlayingWithYarnBall.handle(entityRef, role, store, player);
    }

    if (Constants.CAT_CARRIER_ITEM.equals(itemName)) {
      if (CatCarrierInteraction.hasStoredCat(heldItem)) {
        return CatCarrierInteraction.handleRelease(store, player, heldItem);
      }
      return CatCarrierInteraction.handleCapture(entityRef, role, store, player, heldItem);
    }

    if (player != null) {
      Nameplate nameplate = store.getComponent(entityRef, Nameplate.getComponentType());
      String catName = nameplate != null ? nameplate.getText() : null;

      if (catName != null && !catName.isEmpty()) {
        player.sendMessage(
            Message.translation("cats.interactions.item.unknown.named")
                .param("catName", catName)
                .color(Constants.COLOR_SOFT_ORANGE));
      } else {
        player.sendMessage(
            Message.translation("cats.interactions.item.unknown")
                .color(Constants.COLOR_SOFT_ORANGE));
      }
    }

    return InteractionOwner.handle(entityRef, role, store, player);
  }
}
