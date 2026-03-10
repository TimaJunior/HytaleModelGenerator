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

package de.markusbordihn.cats.blocks;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockComponentChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.markusbordihn.cats.component.CatStateComponent;
import de.markusbordihn.cats.data.CatBedInfo;
import de.markusbordihn.cats.data.CatState;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class CatBed {
  public static final double DEFAULT_SEARCH_RADIUS = 50.0;
  public static final double BED_OCCUPIED_RADIUS = 1.0;
  private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
  private static final double BED_OCCUPIED_RADIUS_SQ = BED_OCCUPIED_RADIUS * BED_OCCUPIED_RADIUS;

  private CatBed() {}

  @Nonnull
  public static List<CatBedInfo> findCatBeds(
      @Nonnull World world, @Nonnull Vector3d searchCenter, double searchRadius) {
    List<CatBedInfo> beds = new ArrayList<>();
    double searchRadiusSq = searchRadius * searchRadius;
    int centerChunkX = (int) Math.floor(searchCenter.x) >> ChunkUtil.BITS;
    int centerChunkZ = (int) Math.floor(searchCenter.z) >> ChunkUtil.BITS;
    int chunkRadius = (int) Math.ceil(searchRadius / ChunkUtil.SIZE) + 1;
    LOGGER.at(Level.FINE).log(
        "Scanning chunks around (%d, %d) with radius %d chunks",
        centerChunkX, centerChunkZ, chunkRadius);

    for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
      for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
        int chunkX = centerChunkX + dx;
        int chunkZ = centerChunkZ + dz;
        long chunkIndex =
            ChunkUtil.indexChunkFromBlock(chunkX << ChunkUtil.BITS, chunkZ << ChunkUtil.BITS);

        Ref<ChunkStore> chunkRef = world.getChunkStore().getChunkReference(chunkIndex);
        if (chunkRef == null) {
          continue;
        }

        Store<ChunkStore> chunkStore = chunkRef.getStore();
        WorldChunk worldChunk = chunkStore.getComponent(chunkRef, WorldChunk.getComponentType());
        if (worldChunk == null) {
          continue;
        }

        BlockComponentChunk blockComponentChunk =
            chunkStore.getComponent(chunkRef, BlockComponentChunk.getComponentType());
        if (blockComponentChunk == null) {
          continue;
        }

        Int2ObjectMap<Ref<ChunkStore>> entityRefs = blockComponentChunk.getEntityReferences();
        if (entityRefs == null || entityRefs.isEmpty()) {
          continue;
        }

        for (Int2ObjectMap.Entry<Ref<ChunkStore>> entry : entityRefs.int2ObjectEntrySet()) {
          int blockIndex = entry.getIntKey();
          int localX = ChunkUtil.xFromIndex(blockIndex);
          int localY = blockIndex / (ChunkUtil.SIZE * ChunkUtil.SIZE);
          int localZ = ChunkUtil.zFromColumn(blockIndex % ChunkUtil.SIZE_COLUMNS);
          int worldX = (chunkX << ChunkUtil.BITS) + localX;
          int worldZ = (chunkZ << ChunkUtil.BITS) + localZ;

          BlockType blockType = worldChunk.getBlockType(new Vector3i(worldX, localY, worldZ));
          if (blockType == null) {
            continue;
          }

          String blockTypeId = blockType.getId();
          if (blockTypeId == null || !blockTypeId.contains("Cat_Bed")) {
            continue;
          }

          Vector3d bedPos = new Vector3d(worldX + 0.5, localY, worldZ + 0.5);
          double distSq = distanceSquared(searchCenter, bedPos);
          if (distSq <= searchRadiusSq) {
            double dist = Math.sqrt(distSq);
            LOGGER.at(Level.FINE).log(
                "Found cat bed '%s' at (%.1f, %.1f, %.1f) distance %.1f blocks",
                blockTypeId, bedPos.x, bedPos.y, bedPos.z, dist);
            beds.add(new CatBedInfo(bedPos, dist));
          }
        }
      }
    }

    LOGGER.at(Level.FINE).log("Found %d cat beds in range", beds.size());

    beds.sort((a, b) -> Double.compare(a.distance(), b.distance()));
    return beds;
  }

  @Nonnull
  public static List<CatBedInfo> findCatBeds(@Nonnull World world, @Nonnull Vector3d searchCenter) {
    return findCatBeds(world, searchCenter, DEFAULT_SEARCH_RADIUS);
  }

  @Nullable
  public static CatBedInfo findNearestAvailableBed(
      @Nonnull List<CatBedInfo> beds,
      @Nonnull Store<EntityStore> store,
      @Nonnull Vector3d currentCatPos) {
    for (CatBedInfo bed : beds) {
      if (!isBedOccupied(bed.position(), store, currentCatPos)) {
        return bed;
      }
    }
    return null;
  }

  public static boolean isBedOccupied(
      @Nonnull Vector3d bedPos,
      @Nonnull Store<EntityStore> store,
      @Nonnull Vector3d excludeCatPos) {
    final boolean[] occupied = {false};

    store.forEachChunk(
        (ArchetypeChunk<EntityStore> chunk, CommandBuffer<EntityStore> buffer) -> {
          if (occupied[0]) {
            return;
          }

          for (int i = 0; i < chunk.size(); i++) {
            CatStateComponent stateComp =
                chunk.getComponent(i, CatStateComponent.getComponentType());
            if (stateComp == null) {
              continue;
            }

            CatState state = stateComp.getState();
            if (state == CatState.WANDERING || state == CatState.FOLLOWING) {
              continue;
            }

            TransformComponent transformComp =
                chunk.getComponent(i, TransformComponent.getComponentType());
            if (transformComp == null) {
              continue;
            }

            Vector3d catPosition = transformComp.getPosition();

            if (distanceSquared(catPosition, excludeCatPos) < 0.01) {
              continue;
            }

            if (distanceSquared(catPosition, bedPos) <= BED_OCCUPIED_RADIUS_SQ) {
              occupied[0] = true;
              return;
            }
          }
        });

    return occupied[0];
  }

  public static double distanceSquared(@Nonnull Vector3d a, @Nonnull Vector3d b) {
    double dx = a.x - b.x;
    double dy = a.y - b.y;
    double dz = a.z - b.z;
    return dx * dx + dy * dy + dz * dz;
  }

  public static double distance(@Nonnull Vector3d a, @Nonnull Vector3d b) {
    return Math.sqrt(distanceSquared(a, b));
  }
}
