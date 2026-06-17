package dev.simulated_team.simulated.util.placement_helpers;

import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import java.util.function.Predicate;
import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementOffset;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;

public class CogwheelPlacementExtension extends SimplePlacementHelper {
   public CogwheelPlacementExtension(Predicate<ItemStack> itemPredicate, Predicate<BlockState> statePredicate) {
      super(itemPredicate, statePredicate);
   }

   public PlacementOffset getOffset(Player player, Level world, BlockState state, BlockPos pos, BlockHitResult ray) {
      ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
      if (heldItem.isEmpty()) {
         heldItem = player.getItemInHand(InteractionHand.OFF_HAND);
      }

      Axis facingAxis;
      if (state.hasProperty(BlockStateProperties.AXIS)) {
         facingAxis = (Axis)state.getValue(BlockStateProperties.AXIS);
      } else {
         if (!state.hasProperty(BlockStateProperties.FACING)) {
            return PlacementOffset.fail();
         }

         facingAxis = ((Direction)state.getValue(BlockStateProperties.FACING)).getAxis();
      }

      if (ICogWheel.isSmallCogItem(heldItem)) {
         for (Direction dir : IPlacementHelper.orderedByDistanceExceptAxis(pos, ray.getLocation(), facingAxis)) {
            BlockPos newPos = pos.relative(dir);
            if (CogWheelBlock.isValidCogwheelPosition(false, world, newPos, facingAxis) && world.getBlockState(newPos).canBeReplaced()) {
               return PlacementOffset.success(newPos, s -> (BlockState)s.setValue(CogWheelBlock.AXIS, facingAxis));
            }
         }
      } else {
         Direction closest = (Direction)IPlacementHelper.orderedByDistanceExceptAxis(pos, ray.getLocation(), facingAxis).get(0);

         for (Direction dirx : IPlacementHelper.orderedByDistanceExceptAxis(pos, ray.getLocation(), facingAxis, d -> d.getAxis() != closest.getAxis())) {
            BlockPos newPos = pos.relative(dirx).relative(closest);
            if (world.getBlockState(newPos).canBeReplaced() && CogWheelBlock.isValidCogwheelPosition(ICogWheel.isLargeCog(state), world, newPos, facingAxis)) {
               return PlacementOffset.success(newPos, s -> (BlockState)s.setValue(CogWheelBlock.AXIS, facingAxis));
            }
         }
      }

      return PlacementOffset.fail();
   }
}
