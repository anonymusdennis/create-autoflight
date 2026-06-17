package dev.ryanhcode.sable.physics.chunk;

import dev.ryanhcode.sable.api.block.BlockWithSubLevelCollisionCallback;
import dev.ryanhcode.sable.util.LevelAccelerator;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import java.util.function.BiFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.KelpBlock;
import net.minecraft.world.level.block.KelpPlantBlock;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

public enum VoxelNeighborhoodState {
   EMPTY(0),
   FACE(9163292),
   EDGE(15330059),
   CORNER(15428619),
   INTERIOR(0);

   public static BiFunction<BlockGetter, BlockState, Boolean> IS_SOLID_MEMOIZED = new BiFunction<BlockGetter, BlockState, Boolean>() {
      private final Int2BooleanOpenHashMap cache = new Int2BooleanOpenHashMap();

      public Boolean apply(BlockGetter blockGetter, BlockState state) {
         return this.cache.computeIfAbsent(state.hashCode(), x -> {
            if (state.isAir()) {
               return false;
            } else {
               return state.getBlock() instanceof MovingPistonBlock ? true : !state.getCollisionShape(blockGetter, BlockPos.ZERO).isEmpty();
            }
         });
      }
   };
   public static BiFunction<BlockGetter, BlockState, Boolean> IS_FULL_BLOCK = new BiFunction<BlockGetter, BlockState, Boolean>() {
      private final Int2BooleanOpenHashMap cache = new Int2BooleanOpenHashMap();

      public Boolean apply(BlockGetter blockGetter, BlockState state) {
         return this.cache.computeIfAbsent(state.hashCode(), x -> state.isAir() ? false : state.isCollisionShapeFullBlock(blockGetter, BlockPos.ZERO));
      }
   };
   private final int debugColor;

   private VoxelNeighborhoodState(final int debugColor) {
      this.debugColor = debugColor;
   }

   public static boolean isSolid(BlockGetter blockGetter, BlockPos pos, BlockState state) {
      return IS_SOLID_MEMOIZED.apply(blockGetter, state);
   }

   public static boolean isFullBlock(BlockGetter blockGetter, BlockPos pos, BlockState state) {
      return IS_FULL_BLOCK.apply(blockGetter, state);
   }

   public static boolean isLiquid(BlockState state) {
      return state.liquid() || state.getBlock() instanceof KelpPlantBlock || state.getBlock() instanceof KelpBlock;
   }

   public static VoxelNeighborhoodState getState(LevelAccelerator level, BlockPos pos, @Nullable LevelChunk chunk) {
      ChunkPos initialPos = new ChunkPos(pos);
      BlockState state = chunk != null ? level.getBlockState(chunk, pos) : level.getBlockState(pos);
      if (!isLiquid(state) && !BlockWithSubLevelCollisionCallback.hasCallback(state)) {
         if (!isSolid(level, pos, state)) {
            return EMPTY;
         } else if (!isFullBlock(level, pos, state)) {
            return CORNER;
         } else {
            boolean allSolid = true;
            boolean cornerSolid = true;
            int bothSidesCount = 0;

            for (Axis axis : Axis.VALUES) {
               BlockPos nPos = pos.relative(Direction.get(AxisDirection.NEGATIVE, axis));
               BlockPos pPos = pos.relative(Direction.get(AxisDirection.POSITIVE, axis));
               BlockState nState = chunk != null && new ChunkPos(nPos).equals(initialPos) ? level.getBlockState(chunk, nPos) : level.getBlockState(nPos);
               BlockState pState = chunk != null && new ChunkPos(pPos).equals(initialPos) ? level.getBlockState(chunk, pPos) : level.getBlockState(pPos);
               boolean negativeSolid = isSolid(level, nPos, nState) && isFullBlock(level, nPos, nState);
               boolean positiveSolid = isSolid(level, pPos, pState) && isFullBlock(level, pPos, pState);
               if (!negativeSolid || !positiveSolid) {
                  allSolid = false;
               }

               if (negativeSolid && positiveSolid) {
                  cornerSolid = false;
                  bothSidesCount++;
               }
            }

            if (allSolid) {
               return INTERIOR;
            } else if (bothSidesCount == 1) {
               return EDGE;
            } else {
               return cornerSolid ? CORNER : FACE;
            }
         }
      } else {
         return CORNER;
      }
   }

   public int getDebugColor() {
      return this.debugColor;
   }

   public byte byteRepresentation() {
      return (byte)this.ordinal();
   }
}
