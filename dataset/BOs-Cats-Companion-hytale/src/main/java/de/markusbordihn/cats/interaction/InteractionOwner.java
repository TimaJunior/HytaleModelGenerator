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
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.util.Alarm;
import de.markusbordihn.cats.Constants;
import de.markusbordihn.cats.data.GiftType;
import de.markusbordihn.cats.data.HappinessLevel;
import de.markusbordihn.cats.data.HappinessSource;
import de.markusbordihn.cats.inventory.InventoryHelper;
import de.markusbordihn.cats.manager.CatsManager;
import java.time.Duration;

public class InteractionOwner {

  private static final String PET_COOLDOWN_ALARM = "PetCooldown";
  private static final Duration PET_COOLDOWN_DURATION = Duration.ofMinutes(5);

  public static boolean handle(
      Ref<EntityStore> entityRef, Role role, Store<EntityStore> store, Player player) {

    NPCEntity npcEntity = store.getComponent(entityRef, NPCEntity.getComponentType());
    if (npcEntity == null) {
      return false;
    }

    Alarm petAlarm = npcEntity.getAlarmStore().get(npcEntity, PET_COOLDOWN_ALARM);
    WorldTimeResource worldTimeResource = store.getResource(WorldTimeResource.getResourceType());

    if (petAlarm.isSet() && !petAlarm.hasPassed(worldTimeResource.getGameTime())) {
      player.sendMessage(
          Message.translation("cats.interactions.owner.petting.cooldown")
              .color(Constants.COLOR_LIGHT_GRAY));
      return true;
    }

    InteractionLogger.logInteraction(
        "OWNER: Petting Interaction", entityRef, role, store, player, null);

    CatsManager catsManager = CatsManager.getInstance();
    if (catsManager != null) {
      catsManager.boostHappiness(entityRef, HappinessSource.PETTING, store);

      GiftType gift = catsManager.tryGiveGift(entityRef, store);
      if (gift != null) {
        HappinessLevel happinessLevel = catsManager.getHappinessLevel(entityRef, store);
        String itemId = gift.getItemId(happinessLevel);
        String catName = catsManager.getCatDisplayName(entityRef, store);

        if (itemId != null) {
          InventoryHelper.giveItem(player, itemId);
        }

        player.sendMessage(
            Message.translation("cats.interactions.owner.gift")
                .param("name", catName)
                .param(
                    "gift", itemId != null ? itemId : gift.name().toLowerCase().replace('_', ' '))
                .color(Constants.COLOR_GOLD));

        role.getStateSupport().setState(entityRef, "Pet", "Happy", store);
        petAlarm.set(entityRef, worldTimeResource.getGameTime().plus(PET_COOLDOWN_DURATION), store);
        return false;
      }
    }

    role.getStateSupport().setState(entityRef, "Petting", "Default", store);
    petAlarm.set(entityRef, worldTimeResource.getGameTime().plus(PET_COOLDOWN_DURATION), store);

    return false;
  }
}
