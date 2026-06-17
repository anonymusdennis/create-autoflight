package com.simibubi.create.content.equipment.wrench;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.Create;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent.BreakEvent;

public interface IWrenchable {
   default InteractionResult onWrenched(BlockState state, UseOnContext context) {
      Level level = context.getLevel();
      BlockPos pos = context.getClickedPos();
      BlockState rotated = this.getRotatedBlockState(state, context.getClickedFace());
      if (!rotated.canSurvive(level, context.getClickedPos())) {
         return InteractionResult.PASS;
      } else {
         KineticBlockEntity.switchToBlockState(level, pos, this.updateAfterWrenched(rotated, context));
         if (level.getBlockState(pos) != state) {
            playRotateSound(level, pos);
         }

         return InteractionResult.SUCCESS;
      }
   }

   default BlockState updateAfterWrenched(BlockState newState, UseOnContext context) {
      return Block.updateFromNeighbourShapes(newState, context.getLevel(), context.getClickedPos());
   }

   default InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
      Level world = context.getLevel();
      BlockPos pos = context.getClickedPos();
      Player player = context.getPlayer();
      if (world instanceof ServerLevel serverLevel) {
         BreakEvent event = new BreakEvent(world, pos, world.getBlockState(pos), player);
         NeoForge.EVENT_BUS.post(event);
         if (event.isCanceled()) {
            return InteractionResult.SUCCESS;
         } else {
            if (player != null && !player.isCreative()) {
               Block.getDrops(state, serverLevel, pos, world.getBlockEntity(pos), player, context.getItemInHand())
                  .forEach(itemStack -> player.getInventory().placeItemBackInInventory(itemStack));
            }

            state.spawnAfterBreak(serverLevel, pos, ItemStack.EMPTY, true);
            world.destroyBlock(pos, false);
            playRemoveSound(world, pos);
            return InteractionResult.SUCCESS;
         }
      } else {
         return InteractionResult.SUCCESS;
      }
   }

   static void playRemoveSound(Level level, BlockPos pos) {
      AllSoundEvents.WRENCH_REMOVE.playOnServer(level, pos, 1.0F, Create.RANDOM.nextFloat() * 0.5F + 0.5F);
   }

   static void playRotateSound(Level level, BlockPos pos) {
      AllSoundEvents.WRENCH_ROTATE.playOnServer(level, pos, 1.0F, Create.RANDOM.nextFloat() + 0.5F);
   }

   default BlockState getRotatedBlockState(BlockState originalState, Direction targetedFace) {
      BlockState newState = originalState;
      if (targetedFace.getAxis() == Axis.Y) {
         if (originalState.hasProperty(HorizontalAxisKineticBlock.HORIZONTAL_AXIS)) {
            return (BlockState)originalState.setValue(
               HorizontalAxisKineticBlock.HORIZONTAL_AXIS,
               VoxelShaper.axisAsFace((Axis)originalState.getValue(HorizontalAxisKineticBlock.HORIZONTAL_AXIS)).getClockWise(targetedFace.getAxis()).getAxis()
            );
         }

         if (originalState.hasProperty(HorizontalKineticBlock.HORIZONTAL_FACING)) {
            return (BlockState)originalState.setValue(
               HorizontalKineticBlock.HORIZONTAL_FACING,
               ((Direction)originalState.getValue(HorizontalKineticBlock.HORIZONTAL_FACING)).getClockWise(targetedFace.getAxis())
            );
         }
      }

      if (originalState.hasProperty(RotatedPillarKineticBlock.AXIS)) {
         return (BlockState)originalState.setValue(
            RotatedPillarKineticBlock.AXIS,
            VoxelShaper.axisAsFace((Axis)originalState.getValue(RotatedPillarKineticBlock.AXIS)).getClockWise(targetedFace.getAxis()).getAxis()
         );
      } else if (!originalState.hasProperty(DirectionalKineticBlock.FACING)) {
         return originalState;
      } else {
         Direction stateFacing = (Direction)originalState.getValue(DirectionalKineticBlock.FACING);
         if (stateFacing.getAxis().equals(targetedFace.getAxis())) {
            return originalState.hasProperty(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE)
               ? (BlockState)originalState.cycle(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE)
               : originalState;
         } else {
            do {
               newState = (BlockState)newState.setValue(
                  DirectionalKineticBlock.FACING, ((Direction)newState.getValue(DirectionalKineticBlock.FACING)).getClockWise(targetedFace.getAxis())
               );
               if (targetedFace.getAxis() == Axis.Y && newState.hasProperty(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE)) {
                  newState = (BlockState)newState.cycle(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE);
               }
            } while (((Direction)newState.getValue(DirectionalKineticBlock.FACING)).getAxis().equals(targetedFace.getAxis()));

            return newState;
         }
      }
   }
}
