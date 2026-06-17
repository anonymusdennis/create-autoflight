package com.simibubi.create.content.redstone.link;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllShapes;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class RedstoneLinkBlock extends WrenchableDirectionalBlock implements IBE<RedstoneLinkBlockEntity> {
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
   public static final BooleanProperty RECEIVER = BooleanProperty.create("receiver");

   public RedstoneLinkBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)((BlockState)this.defaultBlockState().setValue(POWERED, false)).setValue(RECEIVER, false));
   }

   public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
      if (!level.isClientSide) {
         Direction blockFacing = (Direction)state.getValue(FACING);
         if (fromPos.equals(pos.relative(blockFacing.getOpposite())) && !this.canSurvive(state, level, pos)) {
            level.destroyBlock(pos, true);
         } else {
            if (!level.getBlockTicks().willTickThisTick(pos, this)) {
               level.scheduleTick(pos, this, 1);
            }
         }
      }
   }

   public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource r) {
      this.updateTransmittedSignal(state, level, pos);
      if (!(Boolean)state.getValue(RECEIVER)) {
         Direction attachedFace = ((Direction)state.getValue(FACING)).getOpposite();
         BlockPos attachedPos = pos.relative(attachedFace);
         level.blockUpdated(pos, level.getBlockState(pos).getBlock());
         level.blockUpdated(attachedPos, level.getBlockState(attachedPos).getBlock());
      }
   }

   public void onPlace(BlockState state, Level worldIn, BlockPos pos, BlockState oldState, boolean isMoving) {
      if (state.getBlock() != oldState.getBlock() && !isMoving) {
         this.updateTransmittedSignal(state, worldIn, pos);
      }
   }

   public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pMovedByPiston) {
      IBE.onRemove(pState, pLevel, pPos, pNewState);
   }

   public void updateTransmittedSignal(BlockState state, Level level, BlockPos pos) {
      if (!level.isClientSide) {
         if (!(Boolean)state.getValue(RECEIVER)) {
            int power = getPower(level, state, pos);
            int powerFromPanels = this.getBlockEntityOptional(level, pos).map(be -> {
               if (be.panelSupport == null) {
                  return 0;
               } else {
                  Boolean tri = be.panelSupport.shouldBePoweredTristate();
                  return tri == null ? -1 : tri ? 15 : 0;
               }
            }).orElse(0);
            if (powerFromPanels != -1) {
               power = Math.max(power, powerFromPanels);
               boolean previouslyPowered = (Boolean)state.getValue(POWERED);
               if (previouslyPowered != power > 0) {
                  level.setBlock(pos, (BlockState)state.cycle(POWERED), 2);
               }

               this.withBlockEntityDo(level, pos, be -> be.transmit(power));
            }
         }
      }
   }

   private static int getPower(Level level, BlockState state, BlockPos pos) {
      int power = 0;

      for (Direction direction : Iterate.directions) {
         power = Math.max(level.getSignal(pos.relative(direction), direction), power);
      }

      for (Direction direction : Iterate.directions) {
         if (((Direction)state.getValue(FACING)).getOpposite() != direction) {
            power = Math.max(level.getSignal(pos.relative(direction), Direction.UP), power);
         }
      }

      return power;
   }

   public boolean isSignalSource(BlockState state) {
      return (Boolean)state.getValue(POWERED) && (Boolean)state.getValue(RECEIVER);
   }

   public int getDirectSignal(BlockState blockState, BlockGetter blockAccess, BlockPos pos, Direction side) {
      return side != blockState.getValue(FACING) ? 0 : this.getSignal(blockState, blockAccess, pos, side);
   }

   public int getSignal(BlockState state, BlockGetter blockAccess, BlockPos pos, Direction side) {
      return !state.getValue(RECEIVER) ? 0 : this.getBlockEntityOptional(blockAccess, pos).map(RedstoneLinkBlockEntity::getReceivedSignal).orElse(0);
   }

   @Override
   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{POWERED, RECEIVER});
      super.createBlockStateDefinition(builder);
   }

   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
      if (player.isShiftKeyDown() && this.toggleMode(state, level, pos) == InteractionResult.SUCCESS) {
         level.scheduleTick(pos, this, 1);
         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.PASS;
      }
   }

   public InteractionResult toggleMode(BlockState state, Level level, BlockPos pos) {
      return level.isClientSide ? InteractionResult.SUCCESS : this.onBlockEntityUse(level, pos, be -> {
         Boolean wasReceiver = (Boolean)state.getValue(RECEIVER);
         boolean blockPowered = level.hasNeighborSignal(pos);
         level.setBlock(pos, (BlockState)((BlockState)state.cycle(RECEIVER)).setValue(POWERED, blockPowered), 3);
         be.transmit(wasReceiver ? 0 : getPower(level, state, pos));
         return InteractionResult.SUCCESS;
      });
   }

   @Override
   public InteractionResult onWrenched(BlockState state, UseOnContext context) {
      if (this.toggleMode(state, context.getLevel(), context.getClickedPos()) == InteractionResult.SUCCESS) {
         context.getLevel().scheduleTick(context.getClickedPos(), this, 1);
         return InteractionResult.SUCCESS;
      } else {
         return super.onWrenched(state, context);
      }
   }

   @Override
   public BlockState getRotatedBlockState(BlockState originalState, Direction _targetedFace) {
      return originalState;
   }

   public boolean canConnectRedstone(BlockState state, BlockGetter world, BlockPos pos, Direction side) {
      return side != null;
   }

   public boolean canSurvive(BlockState state, LevelReader worldIn, BlockPos pos) {
      BlockPos neighbourPos = pos.relative(((Direction)state.getValue(FACING)).getOpposite());
      BlockState neighbour = worldIn.getBlockState(neighbourPos);
      return !neighbour.canBeReplaced();
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      BlockState state = this.defaultBlockState();
      return (BlockState)state.setValue(FACING, context.getClickedFace());
   }

   public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
      return AllShapes.REDSTONE_BRIDGE.get((Direction)state.getValue(FACING));
   }

   protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
      return false;
   }

   @Override
   public Class<RedstoneLinkBlockEntity> getBlockEntityClass() {
      return RedstoneLinkBlockEntity.class;
   }

   @Override
   public BlockEntityType<? extends RedstoneLinkBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends RedstoneLinkBlockEntity>)AllBlockEntityTypes.REDSTONE_LINK.get();
   }
}
