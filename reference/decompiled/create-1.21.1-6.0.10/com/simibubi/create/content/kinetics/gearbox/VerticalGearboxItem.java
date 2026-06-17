package com.simibubi.create.content.kinetics.gearbox;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.IRotate;
import java.util.Map;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class VerticalGearboxItem extends BlockItem {
   public VerticalGearboxItem(Properties builder) {
      super((Block)AllBlocks.GEARBOX.get(), builder);
   }

   public String getDescriptionId() {
      return "item.create.vertical_gearbox";
   }

   public void registerBlocks(Map<Block, Item> p_195946_1_, Item p_195946_2_) {
   }

   protected boolean updateCustomBlockEntityTag(BlockPos pos, Level world, Player player, ItemStack stack, BlockState state) {
      Axis prefferedAxis = null;

      for (Direction side : Iterate.horizontalDirections) {
         BlockState blockState = world.getBlockState(pos.relative(side));
         if (blockState.getBlock() instanceof IRotate
            && ((IRotate)blockState.getBlock()).hasShaftTowards(world, pos.relative(side), blockState, side.getOpposite())) {
            if (prefferedAxis != null && prefferedAxis != side.getAxis()) {
               prefferedAxis = null;
               break;
            }

            prefferedAxis = side.getAxis();
         }
      }

      Axis axis = prefferedAxis == null ? player.getDirection().getClockWise().getAxis() : (prefferedAxis == Axis.X ? Axis.Z : Axis.X);
      world.setBlockAndUpdate(pos, (BlockState)state.setValue(BlockStateProperties.AXIS, axis));
      return super.updateCustomBlockEntityTag(pos, world, player, stack, state);
   }
}
