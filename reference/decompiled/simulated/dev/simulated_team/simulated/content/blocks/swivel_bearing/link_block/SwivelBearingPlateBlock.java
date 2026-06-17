package dev.simulated_team.simulated.content.blocks.swivel_bearing.link_block;

import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import dev.ryanhcode.sable.api.block.BlockSubLevelAssemblyListener;
import dev.simulated_team.simulated.index.SimBlockEntityTypes;
import dev.simulated_team.simulated.index.SimBlockShapes;
import dev.simulated_team.simulated.index.SimBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SwivelBearingPlateBlock extends DirectionalKineticBlock implements IBE<SwivelBearingPlateBlockEntity>, BlockSubLevelAssemblyListener {
   public SwivelBearingPlateBlock(Properties properties) {
      super(properties);
   }

   public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
      return face == state.getValue(FACING);
   }

   public void beforeMove(ServerLevel originLevel, ServerLevel resultingLevel, BlockState newState, BlockPos oldPos, BlockPos newPos) {
      this.withBlockEntityDo(originLevel, oldPos, SwivelBearingPlateBlockEntity::beforeAssembly);
   }

   public Axis getRotationAxis(BlockState state) {
      return ((Direction)state.getValue(FACING)).getAxis();
   }

   public Class<SwivelBearingPlateBlockEntity> getBlockEntityClass() {
      return SwivelBearingPlateBlockEntity.class;
   }

   public BlockEntityType<? extends SwivelBearingPlateBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends SwivelBearingPlateBlockEntity>)SimBlockEntityTypes.SWIVEL_BEARING_LINK_BLOCK.get();
   }

   protected VoxelShape getCollisionShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
      return SimBlockShapes.SWIVEL_BEARING_PLATE_COLLISION.get((Direction)blockState.getValue(FACING));
   }

   protected VoxelShape getShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
      return SimBlockShapes.SWIVEL_BEARING_PLATE.get((Direction)blockState.getValue(FACING));
   }

   protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
      return SimBlockShapes.SWIVEL_BEARING_PLATE.get((Direction)state.getValue(FACING));
   }

   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
   ) {
      if (!player.mayBuild()) {
         return ItemInteractionResult.FAIL;
      } else if (player.isShiftKeyDown()) {
         return ItemInteractionResult.FAIL;
      } else {
         if (player.getItemInHand(hand).isEmpty()) {
            if (level.isClientSide) {
               return ItemInteractionResult.SUCCESS;
            }

            this.withBlockEntityDo(level, pos, SwivelBearingPlateBlockEntity::setParentAssembleNextTick);
         }

         return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
      }
   }

   public InteractionResult onWrenched(BlockState state, UseOnContext context) {
      return InteractionResult.PASS;
   }

   public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
      return SimBlocks.SWIVEL_BEARING.asStack();
   }

   public void afterMove(ServerLevel originLevel, ServerLevel resultingLevel, BlockState newState, BlockPos oldPos, BlockPos newPos) {
      this.withBlockEntityDo(resultingLevel, newPos, SwivelBearingPlateBlockEntity::fixParentLinkingWhenMoved);
   }
}
