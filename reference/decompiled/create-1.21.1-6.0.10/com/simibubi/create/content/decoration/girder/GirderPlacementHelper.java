package com.simibubi.create.content.decoration.girder;

import com.google.common.base.Predicates;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.equipment.extendoGrip.ExtendoGripItem;
import com.simibubi.create.infrastructure.config.AllConfigs;
import java.util.function.Predicate;
import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementOffset;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class GirderPlacementHelper implements IPlacementHelper {
   public Predicate<ItemStack> getItemPredicate() {
      return AllBlocks.METAL_GIRDER::isIn;
   }

   public Predicate<BlockState> getStatePredicate() {
      return Predicates.or(AllBlocks.METAL_GIRDER::has, AllBlocks.METAL_GIRDER_ENCASED_SHAFT::has);
   }

   private boolean canExtendToward(BlockState state, Direction side) {
      Axis axis = side.getAxis();
      if (state.getBlock() instanceof GirderBlock) {
         boolean x = (Boolean)state.getValue(GirderBlock.X);
         boolean z = (Boolean)state.getValue(GirderBlock.Z);
         if (!x && !z) {
            return axis == Axis.Y;
         } else {
            return x && z ? true : axis == (x ? Axis.X : Axis.Z);
         }
      } else {
         return !(state.getBlock() instanceof GirderEncasedShaftBlock)
            ? false
            : axis != Axis.Y && axis != state.getValue(GirderEncasedShaftBlock.HORIZONTAL_AXIS);
      }
   }

   private int attachedPoles(Level world, BlockPos pos, Direction direction) {
      BlockPos checkPos = pos.relative(direction);
      BlockState state = world.getBlockState(checkPos);

      int count;
      for (count = 0; this.canExtendToward(state, direction); state = world.getBlockState(checkPos)) {
         count++;
         checkPos = checkPos.relative(direction);
      }

      return count;
   }

   private BlockState withAxis(BlockState state, Axis axis) {
      if (state.getBlock() instanceof GirderBlock) {
         return (BlockState)((BlockState)((BlockState)state.setValue(GirderBlock.X, axis == Axis.X)).setValue(GirderBlock.Z, axis == Axis.Z))
            .setValue(GirderBlock.AXIS, axis);
      } else {
         return state.getBlock() instanceof GirderEncasedShaftBlock && axis.isHorizontal()
            ? (BlockState)state.setValue(GirderEncasedShaftBlock.HORIZONTAL_AXIS, axis == Axis.X ? Axis.Z : Axis.X)
            : state;
      }
   }

   public PlacementOffset getOffset(Player player, Level world, BlockState state, BlockPos pos, BlockHitResult ray) {
      for (Direction dir : IPlacementHelper.orderedByDistance(pos, ray.getLocation(), dirx -> this.canExtendToward(state, dirx))) {
         int range = (Integer)AllConfigs.server().equipment.placementAssistRange.get();
         if (player != null) {
            AttributeInstance reach = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
            if (reach != null && reach.hasModifier(ExtendoGripItem.singleRangeAttributeModifier.id())) {
               range += 4;
            }
         }

         int poles = this.attachedPoles(world, pos, dir);
         if (poles < range) {
            BlockPos newPos = pos.relative(dir, poles + 1);
            BlockState newState = world.getBlockState(newPos);
            if (newState.canBeReplaced()) {
               return PlacementOffset.success(newPos, bState -> Block.updateFromNeighbourShapes(this.withAxis(bState, dir.getAxis()), world, newPos));
            }
         }
      }

      return PlacementOffset.fail();
   }
}
