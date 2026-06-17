package net.createmod.catnip.placement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.ghostblock.GhostBlocks;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public interface IPlacementHelper {
   BlockState ID = new BlockState(Blocks.AIR, null, null);

   Predicate<ItemStack> getItemPredicate();

   Predicate<BlockState> getStatePredicate();

   PlacementOffset getOffset(Player var1, Level var2, BlockState var3, BlockPos var4, BlockHitResult var5);

   default PlacementOffset getOffset(Player player, Level world, BlockState state, BlockPos pos, BlockHitResult ray, ItemStack heldItem) {
      PlacementOffset offset = this.getOffset(player, world, state, pos, ray);
      if (heldItem.getItem() instanceof BlockItem blockItem) {
         offset = offset.withGhostState(blockItem.getBlock().defaultBlockState());
      }

      return offset;
   }

   default void renderAt(BlockPos pos, BlockState state, BlockHitResult ray, PlacementOffset offset) {
      this.displayGhost(offset);
   }

   static void renderArrow(Vec3 center, Vec3 target, Direction arrowPlane) {
      renderArrow(center, target, arrowPlane, 1.0);
   }

   static void renderArrow(Vec3 center, Vec3 target, Direction arrowPlane, double distanceFromCenter) {
      Vec3 direction = target.subtract(center).normalize();
      Vec3 facing = Vec3.atLowerCornerOf(arrowPlane.getNormal());
      Vec3 start = center.add(direction);
      Vec3 offset = direction.scale(distanceFromCenter - 1.0);
      Vec3 offsetA = direction.cross(facing).normalize().scale(0.25);
      Vec3 offsetB = facing.cross(direction).normalize().scale(0.25);
      Vec3 endA = center.add(direction.scale(0.75)).add(offsetA);
      Vec3 endB = center.add(direction.scale(0.75)).add(offsetB);
      Outliner.getInstance().showLine("placementArrowA" + center + target, start.add(offset), endA.add(offset)).lineWidth(0.0625F);
      Outliner.getInstance().showLine("placementArrowB" + center + target, start.add(offset), endB.add(offset)).lineWidth(0.0625F);
   }

   default void displayGhost(PlacementOffset offset) {
      if (offset.hasGhostState()) {
         GhostBlocks.getInstance().showGhostState(this, offset.getTransform().apply(offset.getGhostState())).at(offset.getBlockPos()).breathingAlpha();
      }
   }

   static List<Direction> orderedByDistanceOnlyAxis(BlockPos pos, Vec3 hit, Axis axis) {
      return orderedByDistance(pos, hit, dir -> dir.getAxis() == axis);
   }

   static List<Direction> orderedByDistanceOnlyAxis(BlockPos pos, Vec3 hit, Axis axis, Predicate<Direction> includeDirection) {
      return orderedByDistance(pos, hit, (dir -> dir.getAxis() == axis).and(includeDirection));
   }

   static List<Direction> orderedByDistanceExceptAxis(BlockPos pos, Vec3 hit, Axis axis) {
      return orderedByDistance(pos, hit, dir -> dir.getAxis() != axis);
   }

   static List<Direction> orderedByDistanceExceptAxis(BlockPos pos, Vec3 hit, Axis axis, Predicate<Direction> includeDirection) {
      return orderedByDistance(pos, hit, (dir -> dir.getAxis() != axis).and(includeDirection));
   }

   static List<Direction> orderedByDistanceExceptAxis(BlockPos pos, Vec3 hit, Axis first, Axis second) {
      return orderedByDistanceExceptAxis(pos, hit, first, (Predicate<Direction>)(d -> d.getAxis() != second));
   }

   static List<Direction> orderedByDistanceExceptAxis(BlockPos pos, Vec3 hit, Axis first, Axis second, Predicate<Direction> includeDirection) {
      return orderedByDistanceExceptAxis(pos, hit, first, (d -> d.getAxis() != second).and(includeDirection));
   }

   static List<Direction> orderedByDistance(BlockPos pos, Vec3 hit) {
      return orderedByDistance(pos, hit, _$ -> true);
   }

   static List<Direction> orderedByDistance(BlockPos pos, Vec3 hit, Predicate<Direction> includeDirection) {
      List<Direction> directions = new ArrayList<>();

      for (Direction dir : Iterate.directions) {
         if (includeDirection.test(dir)) {
            directions.add(dir);
         }
      }

      return orderedByDistance(pos, hit, directions);
   }

   static List<Direction> orderedByDistance(BlockPos pos, Vec3 hit, Collection<Direction> directions) {
      Vec3 centerToHit = hit.subtract(VecHelper.getCenterOf(pos));
      List<Pair<Direction, Double>> distances = new ArrayList<>();

      for (Direction dir : directions) {
         distances.add(Pair.of(dir, Vec3.atLowerCornerOf(dir.getNormal()).distanceTo(centerToHit)));
      }

      distances.sort(Comparator.comparingDouble(Pair::getSecond));
      List<Direction> sortedDirections = new ArrayList<>();

      for (Pair<Direction, Double> p : distances) {
         sortedDirections.add(p.getFirst());
      }

      return sortedDirections;
   }

   default boolean matchesItem(ItemStack item) {
      return this.getItemPredicate().test(item);
   }

   default boolean matchesState(BlockState state) {
      return this.getStatePredicate().test(state);
   }
}
