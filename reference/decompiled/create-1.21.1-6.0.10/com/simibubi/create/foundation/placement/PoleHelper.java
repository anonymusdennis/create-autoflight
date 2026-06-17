package com.simibubi.create.foundation.placement;

import com.simibubi.create.content.equipment.extendoGrip.ExtendoGripItem;
import com.simibubi.create.infrastructure.config.AllConfigs;
import java.util.function.Function;
import java.util.function.Predicate;
import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementOffset;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;

@MethodsReturnNonnullByDefault
public abstract class PoleHelper<T extends Comparable<T>> implements IPlacementHelper {
   protected final Predicate<BlockState> statePredicate;
   protected final Property<T> property;
   protected final Function<BlockState, Axis> axisFunction;

   public PoleHelper(Predicate<BlockState> statePredicate, Function<BlockState, Axis> axisFunction, Property<T> property) {
      this.statePredicate = statePredicate;
      this.axisFunction = axisFunction;
      this.property = property;
   }

   public boolean matchesAxis(BlockState state, Axis axis) {
      return !this.statePredicate.test(state) ? false : this.axisFunction.apply(state) == axis;
   }

   public int attachedPoles(Level world, BlockPos pos, Direction direction) {
      BlockPos checkPos = pos.relative(direction);
      BlockState state = world.getBlockState(checkPos);

      int count;
      for (count = 0; this.matchesAxis(state, direction.getAxis()); state = world.getBlockState(checkPos)) {
         count++;
         checkPos = checkPos.relative(direction);
      }

      return count;
   }

   public Predicate<BlockState> getStatePredicate() {
      return this.statePredicate;
   }

   public PlacementOffset getOffset(Player player, Level world, BlockState state, BlockPos pos, BlockHitResult ray) {
      for (Direction dir : IPlacementHelper.orderedByDistance(pos, ray.getLocation(), dirx -> dirx.getAxis() == this.axisFunction.apply(state))) {
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
               return PlacementOffset.success(newPos, bState -> (BlockState)bState.setValue(this.property, state.getValue(this.property)));
            }
         }
      }

      return PlacementOffset.fail();
   }
}
