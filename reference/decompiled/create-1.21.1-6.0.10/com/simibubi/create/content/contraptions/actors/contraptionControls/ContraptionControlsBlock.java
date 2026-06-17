package com.simibubi.create.content.contraptions.actors.contraptionControls;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllShapes;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class ContraptionControlsBlock extends ControlsBlock implements IBE<ContraptionControlsBlockEntity> {
   public static final MapCodec<ContraptionControlsBlock> CODEC = simpleCodec(ContraptionControlsBlock::new);

   public ContraptionControlsBlock(Properties pProperties) {
      super(pProperties);
   }

   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
      return this.onBlockEntityUse(level, pos, cte -> {
         cte.pressButton();
         if (!level.isClientSide()) {
            cte.disabled = !cte.disabled;
            cte.notifyUpdate();
            ContraptionControlsBlockEntity.sendStatus(player, cte.filtering.getFilter(), !cte.disabled);
            AllSoundEvents.CONTROLLER_CLICK.play(cte.getLevel(), null, cte.getBlockPos(), 1.0F, cte.disabled ? 0.8F : 1.5F);
         }

         return InteractionResult.SUCCESS;
      });
   }

   public void neighborChanged(BlockState pState, Level pLevel, BlockPos pPos, Block pBlock, BlockPos pFromPos, boolean pIsMoving) {
      this.withBlockEntityDo(pLevel, pPos, ContraptionControlsBlockEntity::updatePoweredState);
   }

   @Override
   public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return AllShapes.CONTRAPTION_CONTROLS.get((Direction)pState.getValue(FACING));
   }

   @Override
   public VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return AllShapes.CONTRAPTION_CONTROLS_COLLISION.get((Direction)pState.getValue(FACING));
   }

   @Override
   public Class<ContraptionControlsBlockEntity> getBlockEntityClass() {
      return ContraptionControlsBlockEntity.class;
   }

   @Override
   public BlockEntityType<? extends ContraptionControlsBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends ContraptionControlsBlockEntity>)AllBlockEntityTypes.CONTRAPTION_CONTROLS.get();
   }

   @NotNull
   @Override
   protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
      return CODEC;
   }
}
