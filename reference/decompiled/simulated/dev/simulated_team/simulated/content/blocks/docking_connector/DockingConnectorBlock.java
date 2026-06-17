package dev.simulated_team.simulated.content.blocks.docking_connector;

import com.google.common.collect.Maps;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock;
import dev.ryanhcode.sable.api.block.BlockSubLevelAssemblyListener;
import dev.simulated_team.simulated.index.SimBlockEntityTypes;
import dev.simulated_team.simulated.index.SimBlocks;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class DockingConnectorBlock extends WrenchableDirectionalBlock implements IBE<DockingConnectorBlockEntity>, BlockSubLevelAssemblyListener {
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
   public static final BooleanProperty EXTENDED = BlockStateProperties.EXTENDED;
   private static final VoxelShape UP_OPEN_AABB = Block.box(0.0, 15.0, 0.0, 16.0, 16.0, 16.0);
   private static final VoxelShape DOWN_OPEN_AABB = Block.box(0.0, 0.0, 0.0, 16.0, 1.0, 16.0);
   private static final VoxelShape WEST_OPEN_AABB = Block.box(0.0, 0.0, 0.0, 1.0, 16.0, 16.0);
   private static final VoxelShape EAST_OPEN_AABB = Block.box(15.0, 0.0, 0.0, 16.0, 16.0, 16.0);
   private static final VoxelShape NORTH_OPEN_AABB = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 1.0);
   private static final VoxelShape SOUTH_OPEN_AABB = Block.box(0.0, 0.0, 15.0, 16.0, 16.0, 16.0);
   private static final Map<Direction, VoxelShape> OPEN_SHAPE_BY_DIRECTION = (Map<Direction, VoxelShape>)Util.make(
      Maps.newEnumMap(Direction.class), enumMap -> {
         enumMap.put(Direction.NORTH, NORTH_OPEN_AABB);
         enumMap.put(Direction.EAST, EAST_OPEN_AABB);
         enumMap.put(Direction.SOUTH, SOUTH_OPEN_AABB);
         enumMap.put(Direction.WEST, WEST_OPEN_AABB);
         enumMap.put(Direction.UP, UP_OPEN_AABB);
         enumMap.put(Direction.DOWN, DOWN_OPEN_AABB);
      }
   );

   public DockingConnectorBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)((BlockState)this.defaultBlockState().setValue(POWERED, false)).setValue(EXTENDED, false));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{POWERED, EXTENDED});
      super.createBlockStateDefinition(builder);
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      Direction nearestLookingDirection = context.getNearestLookingDirection();
      Player player = context.getPlayer();
      if (player != null && player.isShiftKeyDown()) {
         nearestLookingDirection = nearestLookingDirection.getOpposite();
      }

      return (BlockState)((BlockState)super.getStateForPlacement(context).setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos())))
         .setValue(FACING, nearestLookingDirection.getOpposite());
   }

   public void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean isMoving) {
      boolean blockChanged = !state.is(newState.getBlock());
      if ((Boolean)state.getValue(POWERED) && (blockChanged || state.getValue(FACING) != newState.getValue(FACING))) {
         BlockPos pairedConnectorPos = pos.relative((Direction)state.getValue(FACING));
         if (level.getBlockState(pairedConnectorPos).is(SimBlocks.PAIRED_DOCKING_CONNECTOR)) {
            level.removeBlock(pairedConnectorPos, isMoving);
         }
      }

      if (blockChanged) {
         level.getBlockEntity(pos, (BlockEntityType)SimBlockEntityTypes.DOCKING_CONNECTOR.get())
            .ifPresent(connector -> Containers.dropContents(level, pos, connector.inventory));
         super.onRemove(state, level, pos, newState, isMoving);
      }
   }

   public void neighborChanged(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Block block, @NotNull BlockPos fromPos, boolean isMoving) {
      if (!level.isClientSide()) {
         this.withBlockEntityDo(level, pos, DockingConnectorBlockEntity::updateSignal);
         boolean previouslyPowered = (Boolean)state.getValue(POWERED);
         if (previouslyPowered != level.hasNeighborSignal(pos)) {
            level.setBlock(pos, (BlockState)state.cycle(POWERED), 2);
         }
      }
   }

   @NotNull
   public VoxelShape getShape(@NotNull BlockState state, BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
      if (level.getBlockEntity(pos) instanceof DockingConnectorBlockEntity be && !be.isRetracted()) {
         return Shapes.create(be.getBoundingBox(state));
      }

      return Shapes.block();
   }

   @NotNull
   public VoxelShape getBlockSupportShape(@NotNull BlockState state, BlockGetter level, @NotNull BlockPos pos) {
      return Shapes.block();
   }

   public Class<DockingConnectorBlockEntity> getBlockEntityClass() {
      return DockingConnectorBlockEntity.class;
   }

   public BlockEntityType<? extends DockingConnectorBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends DockingConnectorBlockEntity>)SimBlockEntityTypes.DOCKING_CONNECTOR.get();
   }

   protected boolean triggerEvent(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, int id, int param) {
      super.triggerEvent(state, level, pos, id, param);
      BlockEntity be = level.getBlockEntity(pos);
      return be != null && be.triggerEvent(id, param);
   }

   public boolean hasAnalogOutputSignal(BlockState pState) {
      return true;
   }

   public int getAnalogOutputSignal(BlockState pState, Level pLevel, BlockPos pPos) {
      DockingConnectorBlockEntity be = (DockingConnectorBlockEntity)this.getBlockEntity(pLevel, pPos);
      if (!be.isExtended()) {
         return 0;
      } else {
         return be.hasOtherConnector() ? 15 : Math.min(14, Math.max(0, 14 - (int)(14.0 * be.closestPairDistance / 4.0)));
      }
   }

   public void afterMove(ServerLevel originLevel, ServerLevel resultingLevel, BlockState newState, BlockPos oldPos, BlockPos newPos) {
      if (originLevel.getBlockEntity(oldPos) instanceof DockingConnectorBlockEntity be
         && be.hasOtherConnector()
         && originLevel.getBlockEntity(be.otherConnectorPosition) instanceof DockingConnectorBlockEntity connected
         && connected.otherConnectorPosition.equals(oldPos)
         && resultingLevel.getBlockEntity(newPos) instanceof DockingConnectorBlockEntity newBe) {
         be.unDock();
         connected.unDock();
         newBe.unDock();
         newBe.pairTo(connected);
         resultingLevel.blockEvent(newPos, this, 1, 0);
      }
   }
}
