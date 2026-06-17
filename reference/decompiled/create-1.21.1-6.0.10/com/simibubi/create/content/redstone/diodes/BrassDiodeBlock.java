package com.simibubi.create.content.redstone.diodes;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

public class BrassDiodeBlock extends AbstractDiodeBlock implements IBE<BrassDiodeBlockEntity> {
   public static final BooleanProperty POWERING = BooleanProperty.create("powering");
   public static final BooleanProperty INVERTED = BooleanProperty.create("inverted");
   public static final MapCodec<BrassDiodeBlock> CODEC = simpleCodec(BrassDiodeBlock::new);

   public BrassDiodeBlock(Properties properties) {
      super(properties);
      this.registerDefaultState(
         (BlockState)((BlockState)((BlockState)this.defaultBlockState().setValue(POWERED, false)).setValue(POWERING, false)).setValue(INVERTED, false)
      );
   }

   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
   ) {
      return this.toggle(level, pos, state, player, hand);
   }

   public ItemInteractionResult toggle(Level pLevel, BlockPos pPos, BlockState pState, Player player, InteractionHand pHand) {
      if (!player.mayBuild()) {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      } else if (player.isShiftKeyDown()) {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      } else if (AllItems.WRENCH.isIn(player.getItemInHand(pHand))) {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      } else if (pLevel.isClientSide) {
         return ItemInteractionResult.SUCCESS;
      } else {
         pLevel.setBlock(pPos, (BlockState)pState.cycle(INVERTED), 3);
         float f = !pState.getValue(INVERTED) ? 0.6F : 0.5F;
         pLevel.playSound(null, pPos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.3F, f);
         return ItemInteractionResult.SUCCESS;
      }
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{POWERED, POWERING, FACING, INVERTED});
      super.createBlockStateDefinition(builder);
   }

   protected int getOutputSignal(BlockGetter worldIn, BlockPos pos, BlockState state) {
      return state.getValue(POWERING) ^ state.getValue(INVERTED) ? 15 : 0;
   }

   public int getSignal(BlockState blockState, BlockGetter blockAccess, BlockPos pos, Direction side) {
      return blockState.getValue(FACING) == side ? this.getOutputSignal(blockAccess, pos, blockState) : 0;
   }

   protected int getDelay(BlockState state) {
      return 2;
   }

   public boolean canConnectRedstone(BlockState state, BlockGetter world, BlockPos pos, Direction side) {
      return side == null ? false : side.getAxis() == ((Direction)state.getValue(FACING)).getAxis();
   }

   @Override
   public Class<BrassDiodeBlockEntity> getBlockEntityClass() {
      return BrassDiodeBlockEntity.class;
   }

   @Override
   public BlockEntityType<? extends BrassDiodeBlockEntity> getBlockEntityType() {
      return AllBlocks.PULSE_TIMER.is(this)
         ? (BlockEntityType)AllBlockEntityTypes.PULSE_TIMER.get()
         : (
            AllBlocks.PULSE_EXTENDER.is(this)
               ? (BlockEntityType)AllBlockEntityTypes.PULSE_EXTENDER.get()
               : (BlockEntityType)AllBlockEntityTypes.PULSE_REPEATER.get()
         );
   }

   @NotNull
   protected MapCodec<? extends DiodeBlock> codec() {
      return CODEC;
   }
}
