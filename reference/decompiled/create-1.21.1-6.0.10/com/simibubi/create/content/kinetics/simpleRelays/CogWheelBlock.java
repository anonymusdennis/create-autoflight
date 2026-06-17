package com.simibubi.create.content.kinetics.simpleRelays;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.decoration.encasing.EncasableBlock;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.speedController.SpeedControllerBlock;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import javax.annotation.ParametersAreNonnullByDefault;
import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CogWheelBlock extends AbstractSimpleShaftBlock implements ICogWheel, EncasableBlock {
   boolean isLarge;

   protected CogWheelBlock(boolean large, Properties properties) {
      super(properties);
      this.isLarge = large;
   }

   public static CogWheelBlock small(Properties properties) {
      return new CogWheelBlock(false, properties);
   }

   public static CogWheelBlock large(Properties properties) {
      return new CogWheelBlock(true, properties);
   }

   @Override
   public boolean isLargeCog() {
      return this.isLarge;
   }

   @Override
   public boolean isSmallCog() {
      return !this.isLarge;
   }

   public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
      return (this.isLarge ? AllShapes.LARGE_GEAR : AllShapes.SMALL_GEAR).get((Axis)state.getValue(AXIS));
   }

   public boolean canSurvive(BlockState state, LevelReader worldIn, BlockPos pos) {
      return isValidCogwheelPosition(ICogWheel.isLargeCog(state), worldIn, pos, (Axis)state.getValue(AXIS));
   }

   @Override
   public void setPlacedBy(Level worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
      super.setPlacedBy(worldIn, pos, state, placer, stack);
      if (placer instanceof Player player) {
         this.triggerShiftingGearsAdvancement(worldIn, pos, state, player);
      }
   }

   protected void triggerShiftingGearsAdvancement(Level world, BlockPos pos, BlockState state, Player player) {
      if (!world.isClientSide && player != null) {
         Axis axis = (Axis)state.getValue(AXIS);

         for (Axis perpendicular1 : Iterate.axes) {
            if (perpendicular1 != axis) {
               Direction d1 = Direction.get(AxisDirection.POSITIVE, perpendicular1);

               for (Axis perpendicular2 : Iterate.axes) {
                  if (perpendicular1 != perpendicular2 && axis != perpendicular2) {
                     Direction d2 = Direction.get(AxisDirection.POSITIVE, perpendicular2);

                     for (int offset1 : Iterate.positiveAndNegative) {
                        for (int offset2 : Iterate.positiveAndNegative) {
                           BlockPos connectedPos = pos.relative(d1, offset1).relative(d2, offset2);
                           BlockState blockState = world.getBlockState(connectedPos);
                           if (blockState.getBlock() instanceof CogWheelBlock
                              && blockState.getValue(AXIS) == axis
                              && ICogWheel.isLargeCog(blockState) != this.isLarge) {
                              AllAdvancements.COGS.awardTo(player);
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
   ) {
      if (!player.isShiftKeyDown() && player.mayBuild()) {
         ItemInteractionResult result = this.tryEncase(state, level, pos, stack, player, hand, hitResult);
         return result.consumesAction() ? result : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      } else {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      }
   }

   public static boolean isValidCogwheelPosition(boolean large, LevelReader worldIn, BlockPos pos, Axis cogAxis) {
      for (Direction facing : Iterate.directions) {
         if (facing.getAxis() != cogAxis) {
            BlockPos offsetPos = pos.relative(facing);
            BlockState blockState = worldIn.getBlockState(offsetPos);
            if ((!blockState.hasProperty(AXIS) || facing.getAxis() != blockState.getValue(AXIS))
               && (ICogWheel.isLargeCog(blockState) || large && ICogWheel.isSmallCog(blockState))) {
               return false;
            }
         }
      }

      return true;
   }

   protected Axis getAxisForPlacement(BlockPlaceContext context) {
      if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
         return context.getClickedFace().getAxis();
      } else {
         Level world = context.getLevel();
         BlockState stateBelow = world.getBlockState(context.getClickedPos().below());
         if (AllBlocks.ROTATION_SPEED_CONTROLLER.has(stateBelow) && this.isLargeCog()) {
            return stateBelow.getValue(SpeedControllerBlock.HORIZONTAL_AXIS) == Axis.X ? Axis.Z : Axis.X;
         } else {
            BlockPos placedOnPos = context.getClickedPos().relative(context.getClickedFace().getOpposite());
            BlockState placedAgainst = world.getBlockState(placedOnPos);
            Block block = placedAgainst.getBlock();
            if (ICogWheel.isSmallCog(placedAgainst)) {
               return ((IRotate)block).getRotationAxis(placedAgainst);
            } else {
               Axis preferredAxis = getPreferredAxis(context);
               return preferredAxis != null ? preferredAxis : context.getClickedFace().getAxis();
            }
         }
      }
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      boolean shouldWaterlog = context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER;
      return (BlockState)((BlockState)this.defaultBlockState().setValue(AXIS, this.getAxisForPlacement(context)))
         .setValue(BlockStateProperties.WATERLOGGED, shouldWaterlog);
   }

   @Override
   public float getParticleTargetRadius() {
      return this.isLargeCog() ? 1.125F : 0.65F;
   }

   @Override
   public float getParticleInitialRadius() {
      return this.isLargeCog() ? 1.0F : 0.75F;
   }

   @Override
   public boolean isDedicatedCogWheel() {
      return true;
   }
}
