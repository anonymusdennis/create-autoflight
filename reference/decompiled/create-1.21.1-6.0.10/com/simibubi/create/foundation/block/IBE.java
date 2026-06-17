package com.simibubi.create.foundation.block;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntityTicker;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public interface IBE<T extends BlockEntity> extends EntityBlock {
   Class<T> getBlockEntityClass();

   BlockEntityType<? extends T> getBlockEntityType();

   default void withBlockEntityDo(BlockGetter world, BlockPos pos, Consumer<T> action) {
      this.getBlockEntityOptional(world, pos).ifPresent(action);
   }

   default InteractionResult onBlockEntityUse(BlockGetter world, BlockPos pos, Function<T, InteractionResult> action) {
      return this.getBlockEntityOptional(world, pos).map(action).orElse(InteractionResult.PASS);
   }

   default ItemInteractionResult onBlockEntityUseItemOn(BlockGetter world, BlockPos pos, Function<T, ItemInteractionResult> action) {
      return this.getBlockEntityOptional(world, pos).map(action).orElse(ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION);
   }

   static void onRemove(BlockState blockState, Level level, BlockPos pos, BlockState newBlockState) {
      if (blockState.hasBlockEntity()) {
         if (!blockState.is(newBlockState.getBlock()) || !newBlockState.hasBlockEntity()) {
            if (level.getBlockEntity(pos) instanceof SmartBlockEntity sbe) {
               sbe.destroy();
            }

            level.removeBlockEntity(pos);
         }
      }
   }

   default Optional<T> getBlockEntityOptional(BlockGetter world, BlockPos pos) {
      return Optional.ofNullable(this.getBlockEntity(world, pos));
   }

   default BlockEntity newBlockEntity(BlockPos p_153215_, BlockState p_153216_) {
      return this.getBlockEntityType().create(p_153215_, p_153216_);
   }

   default <S extends BlockEntity> BlockEntityTicker<S> getTicker(Level p_153212_, BlockState p_153213_, BlockEntityType<S> p_153214_) {
      return SmartBlockEntity.class.isAssignableFrom(this.getBlockEntityClass()) ? new SmartBlockEntityTicker<>() : null;
   }

   @Nullable
   default T getBlockEntity(BlockGetter worldIn, BlockPos pos) {
      BlockEntity blockEntity = worldIn.getBlockEntity(pos);
      Class<T> expectedClass = this.getBlockEntityClass();
      if (blockEntity == null) {
         return null;
      } else {
         return (T)(!expectedClass.isInstance(blockEntity) ? null : blockEntity);
      }
   }
}
