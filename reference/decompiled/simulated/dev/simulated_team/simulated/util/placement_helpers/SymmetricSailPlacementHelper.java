package dev.simulated_team.simulated.util.placement_helpers;

import java.util.function.Predicate;
import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementOffset;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;

public class SymmetricSailPlacementHelper extends SimplePlacementHelper {
   public SymmetricSailPlacementHelper(Predicate<ItemStack> itemPredicate, Predicate<BlockState> statePredicate) {
      super(itemPredicate, statePredicate);
   }

   public PlacementOffset getOffset(Player player, Level world, BlockState state, BlockPos pos, BlockHitResult ray) {
      if (state.hasProperty(BlockStateProperties.AXIS)) {
         Axis axis = (Axis)state.getValue(BlockStateProperties.AXIS);

         for (Direction dir : IPlacementHelper.orderedByDistanceExceptAxis(pos, ray.getLocation(), axis)) {
            if (world.getBlockState(pos.relative(dir)).canBeReplaced()) {
               return PlacementOffset.success(pos.relative(dir), s -> (BlockState)s.setValue(BlockStateProperties.AXIS, axis));
            }
         }

         return PlacementOffset.fail();
      } else {
         return PlacementOffset.fail();
      }
   }
}
