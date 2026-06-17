package com.simibubi.create.content.logistics.packagerLink;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PackagerLinkBlock extends FaceAttachedHorizontalDirectionalBlock implements IBE<PackagerLinkBlockEntity>, ProperWaterloggedBlock, IWrenchable {
   public static final MapCodec<PackagerLinkBlock> CODEC = simpleCodec(PackagerLinkBlock::new);
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

   public PackagerLinkBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)((BlockState)this.defaultBlockState().setValue(POWERED, false)).setValue(WATERLOGGED, false));
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      BlockPos pos = context.getClickedPos();
      BlockState placed = super.getStateForPlacement(context);
      if (placed == null) {
         return null;
      } else {
         if (placed.getValue(FACE) == AttachFace.CEILING) {
            placed = (BlockState)placed.setValue(FACING, ((Direction)placed.getValue(FACING)).getOpposite());
         }

         return this.withWater((BlockState)placed.setValue(POWERED, getPower(placed, context.getLevel(), pos) > 0), context);
      }
   }

   public static Direction getConnectedDirection(BlockState state) {
      return FaceAttachedHorizontalDirectionalBlock.getConnectedDirection(state);
   }

   public boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
      return true;
   }

   public FluidState getFluidState(BlockState pState) {
      return this.fluidState(pState);
   }

   public BlockState updateShape(BlockState pState, Direction pDirection, BlockState pNeighborState, LevelAccessor pLevel, BlockPos pPos, BlockPos pNeighborPos) {
      this.updateWater(pLevel, pState, pPos);
      return pState;
   }

   public void neighborChanged(BlockState state, Level worldIn, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
      if (!worldIn.isClientSide) {
         int power = getPower(state, worldIn, pos);
         boolean powered = power > 0;
         boolean previouslyPowered = (Boolean)state.getValue(POWERED);
         if (previouslyPowered != powered) {
            worldIn.setBlock(pos, (BlockState)state.cycle(POWERED), 2);
         }

         this.withBlockEntityDo(worldIn, pos, link -> link.behaviour.redstonePowerChanged(power));
      }
   }

   public static int getPower(BlockState state, Level worldIn, BlockPos pos) {
      int power = 0;

      for (Direction d : Iterate.directions) {
         if (d.getOpposite() != getConnectedDirection(state)) {
            power = Math.max(power, worldIn.getSignal(pos.relative(d), d));
         }
      }

      return power;
   }

   public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, LivingEntity pPlacer, ItemStack pStack) {
      super.setPlacedBy(pLevel, pPos, pState, pPlacer, pStack);
      this.withBlockEntityDo(pLevel, pPos, plbe -> {
         if (pPlacer instanceof Player player) {
            plbe.placedBy = player.getUUID();
            plbe.notifyUpdate();
         }
      });
   }

   public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return AllShapes.STOCK_LINK.get(getConnectedDirection(pState));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder.add(new Property[]{POWERED, WATERLOGGED, FACE, FACING}));
   }

   protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
      return false;
   }

   @Override
   public Class<PackagerLinkBlockEntity> getBlockEntityClass() {
      return PackagerLinkBlockEntity.class;
   }

   @Override
   public BlockEntityType<? extends PackagerLinkBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends PackagerLinkBlockEntity>)AllBlockEntityTypes.PACKAGER_LINK.get();
   }

   public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pMovedByPiston) {
      IBE.onRemove(pState, pLevel, pPos, pNewState);
   }

   protected MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
      return CODEC;
   }
}
