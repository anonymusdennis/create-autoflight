package com.simibubi.create.content.contraptions.bearing;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.BlockHitResult;

public class ClockworkBearingBlock extends BearingBlock implements IBE<ClockworkBearingBlockEntity> {
   public ClockworkBearingBlock(Properties properties) {
      super(properties);
   }

   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
   ) {
      if (!player.mayBuild()) {
         return ItemInteractionResult.FAIL;
      } else if (player.isShiftKeyDown()) {
         return ItemInteractionResult.FAIL;
      } else if (stack.isEmpty()) {
         if (!level.isClientSide) {
            this.withBlockEntityDo(level, pos, be -> {
               if (be.running) {
                  be.disassemble();
               } else {
                  be.assembleNextTick = true;
               }
            });
         }

         return ItemInteractionResult.SUCCESS;
      } else {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      }
   }

   @Override
   public Class<ClockworkBearingBlockEntity> getBlockEntityClass() {
      return ClockworkBearingBlockEntity.class;
   }

   @Override
   public InteractionResult onWrenched(BlockState state, UseOnContext context) {
      InteractionResult resultType = super.onWrenched(state, context);
      if (!context.getLevel().isClientSide && resultType.consumesAction()) {
         this.withBlockEntityDo(context.getLevel(), context.getClickedPos(), ClockworkBearingBlockEntity::disassemble);
      }

      return resultType;
   }

   @Override
   public BlockEntityType<? extends ClockworkBearingBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends ClockworkBearingBlockEntity>)AllBlockEntityTypes.CLOCKWORK_BEARING.get();
   }
}
