package com.simibubi.create.content.contraptions.mounted;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.redstone.rail.ControllerRailBlock;
import com.simibubi.create.foundation.advancement.AdvancementBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import org.jetbrains.annotations.NotNull;

public class CartAssemblerBlockItem extends BlockItem {
   public CartAssemblerBlockItem(Block block, Properties properties) {
      super(block, properties);
   }

   @NotNull
   public InteractionResult useOn(UseOnContext context) {
      if (this.tryPlaceAssembler(context)) {
         context.getLevel().playSound(null, context.getClickedPos(), SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
         return InteractionResult.SUCCESS;
      } else {
         return super.useOn(context);
      }
   }

   public boolean tryPlaceAssembler(UseOnContext context) {
      BlockPos pos = context.getClickedPos();
      Level world = context.getLevel();
      BlockState state = world.getBlockState(pos);
      Block block = state.getBlock();
      Player player = context.getPlayer();
      if (player == null) {
         return false;
      } else if (!(block instanceof BaseRailBlock)) {
         CreateLang.translate("block.cart_assembler.invalid").sendStatus(player);
         return false;
      } else {
         RailShape shape = ((BaseRailBlock)block).getRailDirection(state, world, pos, null);
         if (shape != RailShape.EAST_WEST && shape != RailShape.NORTH_SOUTH) {
            return false;
         } else {
            BlockState newState = (BlockState)AllBlocks.CART_ASSEMBLER.getDefaultState().setValue(CartAssemblerBlock.RAIL_SHAPE, shape);
            CartAssembleRailType newType = null;

            for (CartAssembleRailType type : CartAssembleRailType.values()) {
               if (type.matches(state)) {
                  newType = type;
               }
            }

            if (newType == null) {
               return false;
            } else if (world.isClientSide) {
               return true;
            } else {
               newState = (BlockState)newState.setValue(CartAssemblerBlock.RAIL_TYPE, newType);
               if (state.hasProperty(ControllerRailBlock.BACKWARDS)) {
                  newState = (BlockState)newState.setValue(CartAssemblerBlock.BACKWARDS, (Boolean)state.getValue(ControllerRailBlock.BACKWARDS));
               } else {
                  Direction direction = player.getMotionDirection();
                  newState = (BlockState)newState.setValue(CartAssemblerBlock.BACKWARDS, direction.getAxisDirection() == AxisDirection.POSITIVE);
               }

               world.setBlockAndUpdate(pos, newState);
               if (!player.isCreative()) {
                  context.getItemInHand().shrink(1);
               }

               AdvancementBehaviour.setPlacedBy(world, pos, player);
               return true;
            }
         }
      }
   }
}
