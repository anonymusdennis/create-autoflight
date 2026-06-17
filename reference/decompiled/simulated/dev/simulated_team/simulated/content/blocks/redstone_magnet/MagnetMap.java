package dev.simulated_team.simulated.content.blocks.redstone_magnet;

import dev.simulated_team.simulated.util.SimMovementContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class MagnetMap<T extends BlockEntity & SimMagnet> {
   public final Map<LevelAccessor, Map<SectionPos, HashSet<BlockPos>>> magnetMap = new WeakHashMap<>();
   public final Map<LevelAccessor, Map<MagnetPairIdentifier, MagnetPair<T>>> pairMap = new WeakHashMap<>();

   public void addMagnet(LevelAccessor level, SectionPos sectionPos, BlockPos pos) {
      this.magnetMap.putIfAbsent(level, new HashMap<>());
      Map<SectionPos, HashSet<BlockPos>> levelMap = this.magnetMap.get(level);
      levelMap.putIfAbsent(sectionPos, new HashSet<>());
      HashSet<BlockPos> posSet = levelMap.get(sectionPos);
      posSet.add(pos);
   }

   public void removeMagnet(LevelAccessor level, SectionPos sectionPos, BlockPos pos) {
      Map<SectionPos, HashSet<BlockPos>> levelMap = this.magnetMap.get(level);
      if (levelMap != null) {
         HashSet<BlockPos> posSet = levelMap.get(sectionPos);
         if (posSet != null) {
            posSet.remove(pos);
            if (posSet.isEmpty()) {
               levelMap.remove(sectionPos);
               if (levelMap.isEmpty()) {
                  this.magnetMap.remove(level);
               }
            }
         }
      }
   }

   public List<SimMovementContext> findNearby(SimMovementContext context) {
      Map<SectionPos, HashSet<BlockPos>> sectionMap = this.magnetMap.get(context.level());
      if (sectionMap == null) {
         return List.of();
      } else {
         int minX = Math.floorDiv((int)context.globalPosition().x - 8, 16);
         int minY = Math.floorDiv((int)context.globalPosition().y - 8, 16);
         int minZ = Math.floorDiv((int)context.globalPosition().z - 8, 16);
         List<SimMovementContext> contexts = new ArrayList<>();

         for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
               for (int k = 0; k < 2; k++) {
                  SectionPos section = SectionPos.of(minX + i, minY + j, minZ + k);
                  HashSet<BlockPos> posSet = sectionMap.get(section);
                  if (posSet != null) {
                     for (BlockPos blockPos : posSet) {
                        if (!blockPos.equals(context.localBlockPos())) {
                           SimMovementContext otherContext = SimMovementContext.getMovementContext(context.level(), Vec3.atCenterOf(blockPos));
                           contexts.add(otherContext);
                        }
                     }
                  }
               }
            }
         }

         return contexts;
      }
   }

   @Nullable
   public MagnetPair<T> tryAddPair(Level level, BlockPos pos1, BlockPos pos2, MagnetConsumer<T> consumer) {
      this.pairMap.putIfAbsent(level, new HashMap<>());
      Map<MagnetPairIdentifier, MagnetPair<T>> levelMap = this.pairMap.get(level);
      MagnetPairIdentifier id = new MagnetPairIdentifier(pos1, pos2);
      MagnetPair<T> currentPair = levelMap.get(id);
      if (currentPair == null) {
         levelMap.put(id, consumer.apply(level, pos1, pos2));
      } else {
         currentPair.alive = true;
      }

      return currentPair;
   }

   @Nullable
   public MagnetPair<T> getPair(Level level, BlockPos pos1, BlockPos pos2) {
      Map<MagnetPairIdentifier, MagnetPair<T>> levelMap = this.pairMap.get(level);
      return levelMap == null ? null : levelMap.get(new MagnetPairIdentifier(pos1, pos2));
   }

   public void tick(Level level) {
      Map<MagnetPairIdentifier, MagnetPair<T>> map = this.pairMap.get(level);
      if (map != null) {
         map.entrySet().removeIf(x -> !x.getValue().alive);

         for (MagnetPair<?> pair : map.values()) {
            pair.tick();
         }
      }
   }

   public void physicsTick(double substepTimeStep, Level level) {
      Map<MagnetPairIdentifier, MagnetPair<T>> pairs = this.pairMap.get(level);
      if (pairs != null) {
         for (MagnetPair<T> pair : pairs.values()) {
            pair.physicsTick(substepTimeStep);
         }
      }
   }
}
