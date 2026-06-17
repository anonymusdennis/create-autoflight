package com.simibubi.create.content.decoration.girder;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

public class GirderWrenchBehavior {
   @OnlyIn(Dist.CLIENT)
   public static void tick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.level != null && mc.hitResult instanceof BlockHitResult result) {
         ClientLevel var14 = mc.level;
         BlockPos pos = result.getBlockPos();
         Player player = mc.player;
         ItemStack heldItem = player.getMainHandItem();
         if (!player.isShiftKeyDown()) {
            if (AllBlocks.METAL_GIRDER.has(var14.getBlockState(pos))) {
               if (AllItems.WRENCH.isIn(heldItem)) {
                  Pair<Direction, GirderWrenchBehavior.Action> dirPair = getDirectionAndAction(result, var14, pos);
                  if (dirPair != null) {
                     Vec3 center = VecHelper.getCenterOf(pos);
                     Vec3 edge = center.add(Vec3.atLowerCornerOf(((Direction)dirPair.getFirst()).getNormal()).scale(0.4));
                     Axis[] axes = Arrays.stream(Iterate.axes).filter(axis -> axis != ((Direction)dirPair.getFirst()).getAxis()).toArray(Axis[]::new);
                     double normalMultiplier = dirPair.getSecond() == GirderWrenchBehavior.Action.PAIR ? 4.0 : 1.0;
                     Vec3 corner1 = edge.add(Vec3.atLowerCornerOf(Direction.fromAxisAndDirection(axes[0], AxisDirection.POSITIVE).getNormal()).scale(0.3))
                        .add(Vec3.atLowerCornerOf(Direction.fromAxisAndDirection(axes[1], AxisDirection.POSITIVE).getNormal()).scale(0.3))
                        .add(Vec3.atLowerCornerOf(((Direction)dirPair.getFirst()).getNormal()).scale(0.1 * normalMultiplier));
                     normalMultiplier = dirPair.getSecond() == GirderWrenchBehavior.Action.HORIZONTAL ? 9.0 : 2.0;
                     Vec3 corner2 = edge.add(Vec3.atLowerCornerOf(Direction.fromAxisAndDirection(axes[0], AxisDirection.NEGATIVE).getNormal()).scale(0.3))
                        .add(Vec3.atLowerCornerOf(Direction.fromAxisAndDirection(axes[1], AxisDirection.NEGATIVE).getNormal()).scale(0.3))
                        .add(Vec3.atLowerCornerOf(((Direction)dirPair.getFirst()).getOpposite().getNormal()).scale(0.1 * normalMultiplier));
                     Outliner.getInstance().showAABB("girderWrench", new AABB(corner1, corner2)).lineWidth(0.03125F).colored(new Color(127, 127, 127));
                  }
               }
            }
         }
      }
   }

   @Nullable
   private static Pair<Direction, GirderWrenchBehavior.Action> getDirectionAndAction(BlockHitResult result, Level world, BlockPos pos) {
      List<Pair<Direction, GirderWrenchBehavior.Action>> validDirections = getValidDirections(world, pos);
      if (validDirections.isEmpty()) {
         return null;
      } else {
         List<Direction> directions = IPlacementHelper.orderedByDistance(pos, result.getLocation(), validDirections.stream().map(Pair::getFirst).toList());
         if (directions.isEmpty()) {
            return null;
         } else {
            Direction dir = directions.get(0);
            return validDirections.stream()
               .filter(pair -> pair.getFirst() == dir)
               .findFirst()
               .orElseGet(() -> Pair.of(dir, GirderWrenchBehavior.Action.SINGLE));
         }
      }
   }

   public static List<Pair<Direction, GirderWrenchBehavior.Action>> getValidDirections(BlockGetter level, BlockPos pos) {
      BlockState blockState = level.getBlockState(pos);
      return !AllBlocks.METAL_GIRDER.has(blockState)
         ? Collections.emptyList()
         : Arrays.stream(Iterate.directions).<Pair<Direction, GirderWrenchBehavior.Action>>mapMulti((direction, consumer) -> {
            BlockState other = level.getBlockState(pos.relative(direction));
            if ((Boolean)blockState.getValue(GirderBlock.X) || (Boolean)blockState.getValue(GirderBlock.Z)) {
               if (direction.getAxis() == Axis.Y) {
                  if (!AllBlocks.METAL_GIRDER.has(other)) {
                     if (!(Boolean)blockState.getValue(GirderBlock.X) ^ !(Boolean)blockState.getValue(GirderBlock.Z)) {
                        consumer.accept(Pair.of(direction, GirderWrenchBehavior.Action.SINGLE));
                     }
                  } else if (blockState.getValue(GirderBlock.X) != blockState.getValue(GirderBlock.Z)) {
                     if (other.getValue(GirderBlock.X) != other.getValue(GirderBlock.Z)) {
                        consumer.accept(Pair.of(direction, GirderWrenchBehavior.Action.PAIR));
                     }
                  }
               }
            }
         }).toList();
   }

   public static boolean handleClick(Level level, BlockPos pos, BlockState state, BlockHitResult result) {
      Pair<Direction, GirderWrenchBehavior.Action> dirPair = getDirectionAndAction(result, level, pos);
      if (dirPair == null) {
         return false;
      } else if (level.isClientSide) {
         return true;
      } else if (!(Boolean)state.getValue(GirderBlock.X) && !(Boolean)state.getValue(GirderBlock.Z)) {
         return false;
      } else {
         Direction dir = (Direction)dirPair.getFirst();
         BlockPos otherPos = pos.relative(dir);
         BlockState other = level.getBlockState(otherPos);
         if (dir == Direction.UP) {
            level.setBlock(pos, postProcess((BlockState)state.cycle(GirderBlock.TOP)), 18);
            if (dirPair.getSecond() == GirderWrenchBehavior.Action.PAIR && AllBlocks.METAL_GIRDER.has(other)) {
               level.setBlock(otherPos, postProcess((BlockState)other.cycle(GirderBlock.BOTTOM)), 18);
            }

            return true;
         } else if (dir == Direction.DOWN) {
            level.setBlock(pos, postProcess((BlockState)state.cycle(GirderBlock.BOTTOM)), 18);
            if (dirPair.getSecond() == GirderWrenchBehavior.Action.PAIR && AllBlocks.METAL_GIRDER.has(other)) {
               level.setBlock(otherPos, postProcess((BlockState)other.cycle(GirderBlock.TOP)), 18);
            }

            return true;
         } else {
            return true;
         }
      }
   }

   private static BlockState postProcess(BlockState newState) {
      if ((Boolean)newState.getValue(GirderBlock.TOP) && (Boolean)newState.getValue(GirderBlock.BOTTOM)) {
         return newState;
      } else {
         return newState.getValue(GirderBlock.AXIS) != Axis.Y
            ? newState
            : (BlockState)newState.setValue(GirderBlock.AXIS, newState.getValue(GirderBlock.X) ? Axis.X : Axis.Z);
      }
   }

   private static enum Action {
      SINGLE,
      PAIR,
      HORIZONTAL;
   }
}
