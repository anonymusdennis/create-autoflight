package com.simibubi.create.content.schematics.table;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllShapes;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.item.ItemHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class SchematicTableBlock extends HorizontalDirectionalBlock implements IBE<SchematicTableBlockEntity> {
   public static final MapCodec<SchematicTableBlock> CODEC = simpleCodec(SchematicTableBlock::new);

   public SchematicTableBlock(Properties properties) {
      super(properties);
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{FACING});
      super.createBlockStateDefinition(builder);
   }

   public PushReaction getPistonPushReaction(BlockState state) {
      return PushReaction.BLOCK;
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      return (BlockState)this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
   }

   public VoxelShape getCollisionShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
      return AllShapes.TABLE_POLE_SHAPE;
   }

   public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
      return AllShapes.SCHEMATICS_TABLE.get((Direction)state.getValue(FACING));
   }

   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
      if (level.isClientSide) {
         return InteractionResult.SUCCESS;
      } else {
         this.withBlockEntityDo(level, pos, be -> player.openMenu(be, be::sendToMenu));
         return InteractionResult.SUCCESS;
      }
   }

   public void onRemove(BlockState state, Level worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
      if (state.hasBlockEntity() && state.getBlock() != newState.getBlock()) {
         this.withBlockEntityDo(worldIn, pos, be -> ItemHelper.dropContents(worldIn, pos, be.inventory));
         worldIn.removeBlockEntity(pos);
      }
   }

   @Override
   public Class<SchematicTableBlockEntity> getBlockEntityClass() {
      return SchematicTableBlockEntity.class;
   }

   @Override
   public BlockEntityType<? extends SchematicTableBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends SchematicTableBlockEntity>)AllBlockEntityTypes.SCHEMATIC_TABLE.get();
   }

   protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
      return false;
   }

   @NotNull
   protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
      return CODEC;
   }
}
