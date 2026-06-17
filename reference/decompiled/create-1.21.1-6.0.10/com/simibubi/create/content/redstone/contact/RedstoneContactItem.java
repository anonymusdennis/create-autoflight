package com.simibubi.create.content.redstone.contact;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.elevator.ElevatorColumn;
import com.simibubi.create.foundation.utility.BlockHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class RedstoneContactItem extends BlockItem {
   public RedstoneContactItem(Block pBlock, Properties pProperties) {
      super(pBlock, pProperties);
   }

   protected BlockState getPlacementState(BlockPlaceContext ctx) {
      Level world = ctx.getLevel();
      BlockPos pos = ctx.getClickedPos();
      BlockState state = super.getPlacementState(ctx);
      if (state == null) {
         return state;
      } else if (!(state.getBlock() instanceof RedstoneContactBlock)) {
         return state;
      } else {
         Direction facing = (Direction)state.getValue(RedstoneContactBlock.FACING);
         if (facing.getAxis() == Axis.Y) {
            return state;
         } else {
            return ElevatorColumn.get(world, new ElevatorColumn.ColumnCoords(pos.getX(), pos.getZ(), facing)) == null
               ? state
               : BlockHelper.copyProperties(state, AllBlocks.ELEVATOR_CONTACT.getDefaultState());
         }
      }
   }
}
