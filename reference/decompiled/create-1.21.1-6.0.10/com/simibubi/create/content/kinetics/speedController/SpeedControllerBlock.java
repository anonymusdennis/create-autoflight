package com.simibubi.create.content.kinetics.speedController;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.foundation.block.IBE;
import java.util.function.Predicate;
import javax.annotation.ParametersAreNonnullByDefault;
import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementHelpers;
import net.createmod.catnip.placement.PlacementOffset;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SpeedControllerBlock extends HorizontalAxisKineticBlock implements IBE<SpeedControllerBlockEntity> {
   private static final int placementHelperId = PlacementHelpers.register(new SpeedControllerBlock.PlacementHelper());

   public SpeedControllerBlock(Properties properties) {
      super(properties);
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      BlockState above = context.getLevel().getBlockState(context.getClickedPos().above());
      return ICogWheel.isLargeCog(above) && ((Axis)above.getValue(CogWheelBlock.AXIS)).isHorizontal()
         ? (BlockState)this.defaultBlockState().setValue(HORIZONTAL_AXIS, above.getValue(CogWheelBlock.AXIS) == Axis.X ? Axis.Z : Axis.X)
         : super.getStateForPlacement(context);
   }

   public void neighborChanged(BlockState state, Level world, BlockPos pos, Block p_220069_4_, BlockPos neighbourPos, boolean p_220069_6_) {
      if (neighbourPos.equals(pos.above())) {
         this.withBlockEntityDo(world, pos, SpeedControllerBlockEntity::updateBracket);
      }
   }

   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
   ) {
      IPlacementHelper helper = PlacementHelpers.get(placementHelperId);
      return helper.matchesItem(stack)
         ? helper.getOffset(player, level, state, pos, hitResult).placeInWorld(level, (BlockItem)stack.getItem(), player, hand, hitResult)
         : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
   }

   public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
      return AllShapes.SPEED_CONTROLLER;
   }

   @Override
   public Class<SpeedControllerBlockEntity> getBlockEntityClass() {
      return SpeedControllerBlockEntity.class;
   }

   @Override
   public BlockEntityType<? extends SpeedControllerBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends SpeedControllerBlockEntity>)AllBlockEntityTypes.ROTATION_SPEED_CONTROLLER.get();
   }

   @MethodsReturnNonnullByDefault
   private static class PlacementHelper implements IPlacementHelper {
      public Predicate<ItemStack> getItemPredicate() {
         return (ICogWheel::isLargeCogItem).and(ICogWheel::isDedicatedCogItem);
      }

      public Predicate<BlockState> getStatePredicate() {
         return AllBlocks.ROTATION_SPEED_CONTROLLER::has;
      }

      public PlacementOffset getOffset(Player player, Level world, BlockState state, BlockPos pos, BlockHitResult ray) {
         BlockPos newPos = pos.above();
         if (!world.getBlockState(newPos).canBeReplaced()) {
            return PlacementOffset.fail();
         } else {
            Axis newAxis = state.getValue(HorizontalAxisKineticBlock.HORIZONTAL_AXIS) == Axis.X ? Axis.Z : Axis.X;
            return !CogWheelBlock.isValidCogwheelPosition(true, world, newPos, newAxis)
               ? PlacementOffset.fail()
               : PlacementOffset.success(newPos, s -> (BlockState)s.setValue(CogWheelBlock.AXIS, newAxis));
         }
      }
   }
}
