package com.simibubi.create.content.kinetics.simpleRelays;

import com.google.common.base.Predicates;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.decoration.encasing.EncasableBlock;
import com.simibubi.create.content.decoration.girder.GirderEncasedShaftBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.steamEngine.PoweredShaftBlock;
import com.simibubi.create.foundation.placement.PoleHelper;
import java.util.function.Predicate;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ShaftBlock extends AbstractSimpleShaftBlock implements EncasableBlock {
   public static final int placementHelperId = PlacementHelpers.register(new ShaftBlock.PlacementHelper());

   public ShaftBlock(Properties properties) {
      super(properties);
   }

   public static boolean isShaft(BlockState state) {
      return AllBlocks.SHAFT.has(state);
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      BlockState stateForPlacement = super.getStateForPlacement(context);
      return pickCorrectShaftType(stateForPlacement, context.getLevel(), context.getClickedPos());
   }

   public static BlockState pickCorrectShaftType(BlockState stateForPlacement, Level level, BlockPos pos) {
      return PoweredShaftBlock.stillValid(stateForPlacement, level, pos) ? PoweredShaftBlock.getEquivalent(stateForPlacement) : stateForPlacement;
   }

   public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
      return AllShapes.SIX_VOXEL_POLE.get((Axis)state.getValue(AXIS));
   }

   @Override
   public float getParticleTargetRadius() {
      return 0.35F;
   }

   @Override
   public float getParticleInitialRadius() {
      return 0.125F;
   }

   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
   ) {
      if (!player.isShiftKeyDown() && player.mayBuild()) {
         ItemInteractionResult result = this.tryEncase(state, level, pos, stack, player, hand, hitResult);
         if (result.consumesAction()) {
            return result;
         } else if (AllBlocks.METAL_GIRDER.isIn(stack) && state.getValue(AXIS) != Axis.Y) {
            KineticBlockEntity.switchToBlockState(
               level,
               pos,
               (BlockState)((BlockState)AllBlocks.METAL_GIRDER_ENCASED_SHAFT.getDefaultState().setValue(WATERLOGGED, (Boolean)state.getValue(WATERLOGGED)))
                  .setValue(GirderEncasedShaftBlock.HORIZONTAL_AXIS, state.getValue(AXIS) == Axis.Z ? Axis.Z : Axis.X)
            );
            if (!level.isClientSide && !player.isCreative()) {
               stack.shrink(1);
               if (stack.isEmpty()) {
                  player.setItemInHand(hand, ItemStack.EMPTY);
               }
            }

            return ItemInteractionResult.SUCCESS;
         } else {
            IPlacementHelper helper = PlacementHelpers.get(placementHelperId);
            return helper.matchesItem(stack)
               ? helper.getOffset(player, level, state, pos, hitResult).placeInWorld(level, (BlockItem)stack.getItem(), player, hand, hitResult)
               : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
         }
      } else {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      }
   }

   @MethodsReturnNonnullByDefault
   private static class PlacementHelper extends PoleHelper<Axis> {
      private PlacementHelper() {
         super(
            state -> state.getBlock() instanceof AbstractSimpleShaftBlock || state.getBlock() instanceof PoweredShaftBlock,
            state -> (Axis)state.getValue(RotatedPillarKineticBlock.AXIS),
            RotatedPillarKineticBlock.AXIS
         );
      }

      public Predicate<ItemStack> getItemPredicate() {
         return i -> i.getItem() instanceof BlockItem && ((BlockItem)i.getItem()).getBlock() instanceof AbstractSimpleShaftBlock;
      }

      @Override
      public Predicate<BlockState> getStatePredicate() {
         return Predicates.or(AllBlocks.SHAFT::has, AllBlocks.POWERED_SHAFT::has);
      }

      @Override
      public PlacementOffset getOffset(Player player, Level world, BlockState state, BlockPos pos, BlockHitResult ray) {
         PlacementOffset offset = super.getOffset(player, world, state, pos, ray);
         if (offset.isSuccessful()) {
            offset.withTransform(
               offset.getTransform().andThen(s -> (BlockState)(world.isClientSide() ? s : ShaftBlock.pickCorrectShaftType(s, world, offset.getBlockPos())))
            );
         }

         return offset;
      }
   }
}
