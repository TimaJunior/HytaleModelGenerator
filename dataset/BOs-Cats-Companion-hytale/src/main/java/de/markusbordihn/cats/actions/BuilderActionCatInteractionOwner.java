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

package de.markusbordihn.cats.actions;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import de.markusbordihn.cats.interaction.InteractionOwner;
import de.markusbordihn.cats.interaction.ItemInteractionOwner;
import javax.annotation.Nonnull;

public class BuilderActionCatInteractionOwner extends BuilderActionCatInteractionBase {
  public static final String BUILDER_ID = "CatInteractionOwner";

  @Override
  public String getBuilderId() {
    return BUILDER_ID;
  }

  @Nonnull
  @Override
  public ActionCatInteractionOwner build(BuilderSupport support) {
    return new ActionCatInteractionOwner(this, support);
  }

  @Nonnull
  @Override
  public String getShortDescription() {
    return "Logs owner cat interaction events";
  }

  @Nonnull
  @Override
  public String getLongDescription() {
    return "Custom action that logs when the owner interacts with their tamed cat NPC";
  }

  public static class ActionCatInteractionOwner extends ActionCatInteractionBase {

    public ActionCatInteractionOwner(
        BuilderActionCatInteractionBase builder, BuilderSupport support) {
      super(builder);
    }

    @Override
    public boolean canExecute(
        Ref<EntityStore> entityRef,
        Role role,
        InfoProvider infoProvider,
        double deltaTime,
        Store<EntityStore> store) {
      if (!isCatTamed(entityRef, store)) {
        return false;
      }
      Player player = getPlayerFromInfoProvider(role, infoProvider, store);
      if (player == null) {
        return false;
      }
      return isOwner(entityRef, getPlayerUUID(role, store), store);
    }

    @Override
    public boolean execute(
        Ref<EntityStore> entityRef,
        Role role,
        InfoProvider infoProvider,
        double deltaTime,
        Store<EntityStore> store) {
      Player player = getPlayerFromInfoProvider(role, infoProvider, store);
      ItemStack heldItem = getHeldItem(player);

      if (heldItem != null && heldItem.isValid()) {
        return ItemInteractionOwner.handle(entityRef, role, store, player, heldItem);
      } else {
        return InteractionOwner.handle(entityRef, role, store, player);
      }
    }
  }
}
