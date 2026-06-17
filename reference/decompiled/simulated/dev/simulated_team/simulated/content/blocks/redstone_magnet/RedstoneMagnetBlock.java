package dev.simulated_team.simulated.content.blocks.redstone_magnet;

import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock;
import dev.simulated_team.simulated.data.advancements.SimAdvancements;
import dev.simulated_team.simulated.index.SimBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;

public class RedstoneMagnetBlock extends WrenchableDirectionalBlock implements IBE<RedstoneMagnetBlockEntity> {
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

   public RedstoneMagnetBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)this.defaultBlockState().setValue(POWERED, false));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{POWERED});
      super.createBlockStateDefinition(builder);
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      Direction nearestLookingDirection = context.getNearestLookingDirection();
      if (context.getPlayer().isShiftKeyDown()) {
         nearestLookingDirection = nearestLookingDirection.getOpposite();
      }

      return (BlockState)((BlockState)super.getStateForPlacement(context).setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos())))
         .setValue(FACING, nearestLookingDirection.getOpposite());
   }

   public void neighborChanged(BlockState state, Level level, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
      if (!level.isClientSide) {
         this.withBlockEntityDo(level, pos, RedstoneMagnetBlockEntity::updateSignal);
         boolean previouslyPowered = (Boolean)state.getValue(POWERED);
         if (previouslyPowered != level.hasNeighborSignal(pos)) {
            level.setBlock(pos, (BlockState)state.cycle(POWERED), 2);
         }

         if ((Boolean)state.getValue(POWERED)) {
            SimAdvancements.OPPOSITES_ATTRACT.awardToNearby(pos, level);
         }
      }
   }

   public Class<RedstoneMagnetBlockEntity> getBlockEntityClass() {
      return RedstoneMagnetBlockEntity.class;
   }

   public BlockEntityType<? extends RedstoneMagnetBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends RedstoneMagnetBlockEntity>)SimBlockEntityTypes.REDSTONE_MAGNET.get();
   }
}
