package com.simibubi.create.content.decoration;

import com.simibubi.create.AllShapes;
import com.simibubi.create.content.equipment.extendoGrip.ExtendoGripItem;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.infrastructure.config.AllConfigs;
import java.util.function.Predicate;
import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementHelpers;
import net.createmod.catnip.placement.PlacementOffset;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class MetalLadderBlock extends LadderBlock implements IWrenchable {
   private static final int placementHelperId = PlacementHelpers.register(new MetalLadderBlock.PlacementHelper());

   public MetalLadderBlock(Properties p_54345_) {
      super(p_54345_);
   }

   @OnlyIn(Dist.CLIENT)
   public boolean supportsExternalFaceHiding(BlockState state) {
      return false;
   }

   @OnlyIn(Dist.CLIENT)
   public boolean skipRendering(BlockState pState, BlockState pAdjacentBlockState, Direction pDirection) {
      return pDirection != null && pDirection.getAxis().isHorizontal()
         ? pAdjacentBlockState.isAir() || !pAdjacentBlockState.blocksMotion()
         : pDirection == Direction.UP && pAdjacentBlockState.getBlock() instanceof LadderBlock;
   }

   public VoxelShape getOcclusionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
      return AllShapes.SIX_VOXEL_POLE.get(Axis.Y);
   }

   public BlockState updateShape(BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos) {
      return !pState.canSurvive(pLevel, pCurrentPos)
         ? Blocks.AIR.defaultBlockState()
         : super.updateShape(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos);
   }

   public boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
      BlockState otherState = pLevel.getBlockState(pPos.relative(Direction.UP));
      return super.canSurvive(pState, pLevel, pPos) || otherState.is(this) && ((Direction)pState.getValue(FACING)).equals(otherState.getValue(FACING));
   }

   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
   ) {
      if (!player.isShiftKeyDown() && player.mayBuild()) {
         IPlacementHelper helper = PlacementHelpers.get(placementHelperId);
         return helper.matchesItem(stack)
            ? helper.getOffset(player, level, state, pos, hitResult).placeInWorld(level, (BlockItem)stack.getItem(), player, hand, hitResult)
            : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      } else {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      }
   }

   @MethodsReturnNonnullByDefault
   private static class PlacementHelper implements IPlacementHelper {
      public Predicate<ItemStack> getItemPredicate() {
         return i -> i.getItem() instanceof BlockItem && ((BlockItem)i.getItem()).getBlock() instanceof MetalLadderBlock;
      }

      public Predicate<BlockState> getStatePredicate() {
         return s -> s.getBlock() instanceof LadderBlock;
      }

      public int attachedLadders(Level world, BlockPos pos, Direction direction) {
         BlockPos checkPos = pos.relative(direction);
         BlockState state = world.getBlockState(checkPos);

         int count;
         for (count = 0; this.getStatePredicate().test(state); state = world.getBlockState(checkPos)) {
            count++;
            checkPos = checkPos.relative(direction);
         }

         return count;
      }

      public PlacementOffset getOffset(Player player, Level world, BlockState state, BlockPos pos, BlockHitResult ray) {
         Direction dir = player.getXRot() < 0.0F ? Direction.UP : Direction.DOWN;
         int range = (Integer)AllConfigs.server().equipment.placementAssistRange.get();
         if (player != null) {
            AttributeInstance reach = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
            if (reach != null && reach.hasModifier(ExtendoGripItem.singleRangeAttributeModifier.id())) {
               range += 4;
            }
         }

         int ladders = this.attachedLadders(world, pos, dir);
         if (ladders >= range) {
            return PlacementOffset.fail();
         } else {
            BlockPos newPos = pos.relative(dir, ladders + 1);
            BlockState newState = world.getBlockState(newPos);
            if (!state.canSurvive(world, newPos)) {
               return PlacementOffset.fail();
            } else {
               return newState.canBeReplaced()
                  ? PlacementOffset.success(newPos, bState -> (BlockState)bState.setValue(LadderBlock.FACING, (Direction)state.getValue(LadderBlock.FACING)))
                  : PlacementOffset.fail();
            }
         }
      }
   }
}
