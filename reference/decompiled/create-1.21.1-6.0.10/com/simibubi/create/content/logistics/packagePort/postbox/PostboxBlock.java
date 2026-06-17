package com.simibubi.create.content.logistics.packagePort.postbox;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class PostboxBlock extends HorizontalDirectionalBlock implements IBE<PostboxBlockEntity>, IWrenchable, ProperWaterloggedBlock {
   public static MapCodec<PostboxBlock> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(BlockBehaviour.propertiesCodec(), DyeColor.CODEC.fieldOf("color").forGetter(PostboxBlock::getColor))
            .apply(instance, PostboxBlock::new)
   );
   public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
   protected final DyeColor color;

   public PostboxBlock(Properties properties, DyeColor color) {
      super(properties);
      this.color = color;
      this.registerDefaultState((BlockState)((BlockState)this.defaultBlockState().setValue(OPEN, false)).setValue(WATERLOGGED, false));
   }

   public DyeColor getColor() {
      return this.color;
   }

   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      Direction facing = pContext.getHorizontalDirection().getOpposite();
      return this.withWater((BlockState)super.getStateForPlacement(pContext).setValue(FACING, facing), pContext);
   }

   public FluidState getFluidState(BlockState pState) {
      return this.fluidState(pState);
   }

   public BlockState updateShape(BlockState pState, Direction pDirection, BlockState pNeighborState, LevelAccessor pLevel, BlockPos pPos, BlockPos pNeighborPos) {
      this.updateWater(pLevel, pState, pPos);
      return pState;
   }

   public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return AllShapes.POSTBOX.get((Direction)pState.getValue(FACING));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
      super.createBlockStateDefinition(pBuilder.add(new Property[]{FACING, OPEN, WATERLOGGED}));
   }

   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
      return this.onBlockEntityUse(level, pos, be -> be.use(player).result());
   }

   @Override
   public Class<PostboxBlockEntity> getBlockEntityClass() {
      return PostboxBlockEntity.class;
   }

   @Override
   public BlockEntityType<? extends PostboxBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends PostboxBlockEntity>)AllBlockEntityTypes.PACKAGE_POSTBOX.get();
   }

   protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
      return false;
   }

   public boolean hasAnalogOutputSignal(BlockState pState) {
      return true;
   }

   public int getAnalogOutputSignal(BlockState pState, Level pLevel, BlockPos pPos) {
      return this.getBlockEntityOptional(pLevel, pPos).map(pbe -> pbe.getComparatorOutput()).orElse(0);
   }

   public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pMovedByPiston) {
      IBE.onRemove(pState, pLevel, pPos, pNewState);
   }

   @NotNull
   protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
      return CODEC;
   }
}
