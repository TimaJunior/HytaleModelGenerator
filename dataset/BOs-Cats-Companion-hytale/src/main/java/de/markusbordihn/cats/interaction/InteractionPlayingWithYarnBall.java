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
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.util.Alarm;
import de.markusbordihn.cats.data.CatDataEntry;
import de.markusbordihn.cats.data.HappinessSource;
import de.markusbordihn.cats.manager.CatsManager;
import java.time.Duration;

public class InteractionPlayingWithYarnBall {

  private static final String PLAY_BOOST_ALARM = "PlayBoostCooldown";
  private static final long BASE_COOLDOWN_MINUTES = 3;

  public static boolean handle(
      Ref<EntityStore> entityRef, Role role, Store<EntityStore> store, Player player) {
    InteractionLogger.logInteraction(
        "PLAYING WITH YARN BALL", entityRef, role, store, player, "Cat_Yarn_Ball");

    role.getStateSupport().setState(entityRef, "PlayingWithYarnBall", "Default", store);

    NPCEntity npcEntity = store.getComponent(entityRef, NPCEntity.getComponentType());
    if (npcEntity == null) {
      return false;
    }

    Alarm boostAlarm = npcEntity.getAlarmStore().get(npcEntity, PLAY_BOOST_ALARM);
    WorldTimeResource worldTimeResource = store.getResource(WorldTimeResource.getResourceType());

    if (boostAlarm.isSet() && !boostAlarm.hasPassed(worldTimeResource.getGameTime())) {
      return false;
    }

    CatsManager catsManager = CatsManager.getInstance();
    if (catsManager != null) {
      CatDataEntry catData = catsManager.getCatData(entityRef, store);
      long cooldownMinutes = BASE_COOLDOWN_MINUTES;
      if (catData != null && catData.personalityType() != null) {
        cooldownMinutes =
            Math.max(
                1, (long) (BASE_COOLDOWN_MINUTES / catData.personalityType().getPlayModifier()));
      }
      catsManager.boostHappiness(entityRef, HappinessSource.PLAYING, store);
      boostAlarm.set(
          entityRef,
          worldTimeResource.getGameTime().plus(Duration.ofMinutes(cooldownMinutes)),
          store);
    }

    return false;
  }
}
