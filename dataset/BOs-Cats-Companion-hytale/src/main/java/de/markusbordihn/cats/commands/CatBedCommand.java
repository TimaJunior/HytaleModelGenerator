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

import com.hypixel.hytale.builtin.path.path.TransientPath;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.types.EntityWrappedArg;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import de.markusbordihn.cats.Constants;
import de.markusbordihn.cats.blocks.CatBed;
import de.markusbordihn.cats.component.CatBedTargetComponent;
import de.markusbordihn.cats.component.CatStateComponent;
import de.markusbordihn.cats.data.CatBedInfo;
import de.markusbordihn.cats.data.CatState;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import javax.annotation.Nonnull;

final class CatBedCommand extends CatCommand {
  private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

  @Nonnull private final EntityWrappedArg entityArg;

  public CatBedCommand() {
    super("bed", "Makes your cat sleep on the nearest available bed");
    this.entityArg = this.withOptionalArg("entity", "The cat entity", ArgTypes.ENTITY_ID);
  }

  @Override
  protected void execute(
      @Nonnull CommandContext context, @Nonnull World world, @Nonnull Store<EntityStore> store) {
    Optional<Ref<EntityStore>> entityRefOpt = getEntityFromArgument(this.entityArg, store, context);
    if (entityRefOpt.isEmpty()) {
      context.sendMessage(
          Message.translation("cats.commands.error.no_cat").color(Constants.COLOR_ERROR));
      return;
    }

    Ref<EntityStore> catRef = entityRefOpt.get();
    if (!checkOwnership(catRef, store, context)) {
      return;
    }

    TransformComponent catTransform =
        store.getComponent(catRef, TransformComponent.getComponentType());
    if (catTransform == null) {
      context.sendMessage(
          Message.translation("cats.commands.error.no_position").color(Constants.COLOR_ERROR));
      return;
    }

    Vector3d catPos = catTransform.getPosition();

    // Find all cat beds in range using CatBed helper
    List<CatBedInfo> beds = CatBed.findCatBeds(world, catPos);
    if (beds.isEmpty()) {
      String catName = getCatDisplayName(catRef, store);
      context.sendMessage(
          Message.translation("cats.commands.bed.no_beds_found")
              .param("name", catName)
              .param("x", String.format("%.1f", catPos.x))
              .param("y", String.format("%.1f", catPos.y))
              .param("z", String.format("%.1f", catPos.z))
              .param("radius", String.format("%.0f", CatBed.DEFAULT_SEARCH_RADIUS))
              .color(Constants.COLOR_INFO));
      return;
    }

    LOGGER.at(Level.INFO).log("Found %d cat bed(s) in range", beds.size());

    // Find the nearest unoccupied bed using CatBed helper
    CatBedInfo availableBed = CatBed.findNearestAvailableBed(beds, store, catPos);
    if (availableBed == null) {
      String catName = getCatDisplayName(catRef, store);
      context.sendMessage(
          Message.translation("cats.commands.bed.all_beds_occupied")
              .param("name", catName)
              .param("count", String.valueOf(beds.size()))
              .color(Constants.COLOR_INFO));
      return;
    }

    // Send cat to bed
    String catName = getCatDisplayName(catRef, store);
    Vector3d bedPos = availableBed.getPosition();
    double distanceToBed = CatBed.distance(catPos, bedPos);

    NPCEntity npcEntity = store.getComponent(catRef, NPCEntity.getComponentType());
    if (npcEntity == null || npcEntity.getRole() == null) {
      context.sendMessage(
          Message.translation("cats.commands.error.no_cat")
              .param("name", catName)
              .color(Constants.COLOR_INFO));
      return;
    }

    // Set path to bed
    TransientPath path = new TransientPath();
    path.addWaypoint(new Vector3d(bedPos.x, bedPos.y + 0.5, bedPos.z), new Vector3f(0, 0, 0));
    npcEntity.getPathManager().setTransientPath(path);

    // Store bed target position for timeout-based teleport
    store.putComponent(
        catRef, CatBedTargetComponent.getComponentType(), new CatBedTargetComponent(bedPos));

    // Set cat to going to bed state
    store.putComponent(
        catRef, CatStateComponent.getComponentType(), new CatStateComponent(CatState.GOING_TO_BED));
    npcEntity.getRole().getStateSupport().setState(catRef, "Pet", "GoingToBed", store);
    context.sendMessage(
        Message.translation("cats.commands.bed.going_to_bed")
            .param("name", catName)
            .param("distance", String.format("%.1f", distanceToBed))
            .param("x", String.format("%.1f", bedPos.x))
            .param("y", String.format("%.1f", bedPos.y))
            .param("z", String.format("%.1f", bedPos.z))
            .color(Constants.COLOR_SUCCESS));
  }
}
