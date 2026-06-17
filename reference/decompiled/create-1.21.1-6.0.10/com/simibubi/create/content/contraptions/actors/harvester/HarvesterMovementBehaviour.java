package com.simibubi.create.content.contraptions.actors.harvester;

import com.simibubi.create.AllTags;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.compat.Mods;
import com.simibubi.create.compat.farmersdelight.FarmersDelightCompat;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ActorVisual;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.mixin.accessor.CropBlockAccessor;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import com.simibubi.create.infrastructure.config.AllConfigs;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.GrowingPlantBlock;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.SpecialPlantable;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.Nullable;

public class HarvesterMovementBehaviour implements MovementBehaviour {
   @Override
   public boolean isActive(MovementContext context) {
      return MovementBehaviour.super.isActive(context)
         && !VecHelper.isVecPointingTowards(context.relativeMotion, ((Direction)context.state.getValue(HarvesterBlock.FACING)).getOpposite());
   }

   @Override
   public Vec3 getActiveAreaOffset(MovementContext context) {
      return Vec3.atLowerCornerOf(((Direction)context.state.getValue(HarvesterBlock.FACING)).getNormal()).scale(0.45);
   }

   @Override
   public void visitNewPosition(MovementContext context, BlockPos pos) {
      Level world = context.world;
      if (!world.isClientSide) {
         BlockState stateVisited = world.getBlockState(pos);
         if (!stateVisited.isAir() && !AllTags.AllBlockTags.NON_HARVESTABLE.matches(stateVisited)) {
            boolean notCropButCuttable = false;
            if (!this.isValidCrop(world, pos, stateVisited)) {
               if (!this.isValidOther(world, pos, stateVisited)) {
                  return;
               }

               notCropButCuttable = true;
            }

            ItemStack item = ItemStack.EMPTY;
            float effectChance = 1.0F;
            if (stateVisited.is(BlockTags.LEAVES)) {
               item = new ItemStack(Items.SHEARS);
               effectChance = 0.45F;
            }

            MutableBoolean seedSubtracted = new MutableBoolean(notCropButCuttable);
            BlockHelper.destroyBlockAs(
               world,
               pos,
               null,
               item,
               effectChance,
               stack -> {
                  if ((Boolean)AllConfigs.server().kinetics.harvesterReplants.get()
                     && !seedSubtracted.getValue()
                     && ItemHelper.sameItem(stack, new ItemStack(stateVisited.getBlock()))) {
                     stack.shrink(1);
                     seedSubtracted.setTrue();
                  }

                  this.collectOrDropItem(context, stack);
               }
            );
            BlockState cutCrop = this.cutCrop(world, pos, stateVisited);
            world.setBlockAndUpdate(pos, cutCrop.canSurvive(world, pos) ? cutCrop : Blocks.AIR.defaultBlockState());
         }
      }
   }

   public boolean isValidCrop(Level world, BlockPos pos, BlockState state) {
      boolean harvestPartial = (Boolean)AllConfigs.server().kinetics.harvestPartiallyGrown.get();
      boolean replant = (Boolean)AllConfigs.server().kinetics.harvesterReplants.get();
      if (state.getBlock() instanceof CropBlock crop) {
         return !harvestPartial ? crop.isMaxAge(state) : state != crop.getStateForAge(0) || !replant;
      } else {
         if (state.getCollisionShape(world, pos).isEmpty() || state.getBlock() instanceof CocoaBlock) {
            for (Property<?> property : state.getProperties()) {
               if (property instanceof IntegerProperty) {
                  IntegerProperty ageProperty = (IntegerProperty)property;
                  if (property.getName().equals(BlockStateProperties.AGE_1.getName())) {
                     int age = (Integer)state.getValue(ageProperty);
                     if ((!(state.getBlock() instanceof SweetBerryBushBlock) || age > 1 || !replant)
                        && (age != 0 || !replant)
                        && (harvestPartial || ageProperty.getPossibleValues().size() - 1 == age)) {
                        return true;
                     }
                  }
               }
            }
         }

         return false;
      }
   }

   public boolean isValidOther(Level world, BlockPos pos, BlockState state) {
      if (state.getBlock() instanceof CropBlock) {
         return false;
      } else if (state.getBlock() instanceof SugarCaneBlock) {
         return true;
      } else if (state.is(BlockTags.LEAVES)) {
         return true;
      } else if (state.getBlock() instanceof CocoaBlock) {
         return (Integer)state.getValue(CocoaBlock.AGE) == 2;
      } else {
         if (state.getCollisionShape(world, pos).isEmpty()) {
            if (state.getBlock() instanceof GrowingPlantBlock) {
               return true;
            }

            for (Property<?> property : state.getProperties()) {
               if (property instanceof IntegerProperty && property.getName().equals(BlockStateProperties.AGE_1.getName())) {
                  return false;
               }
            }

            if (state.getBlock() instanceof MushroomBlock && Mods.FARMERSDELIGHT.isLoaded()) {
               return FarmersDelightCompat.shouldHarvestMushroom(world, pos, state);
            }

            if (state.getBlock() instanceof BushBlock) {
               return true;
            }

            if (state.getBlock() instanceof SpecialPlantable) {
               return true;
            }
         }

         return false;
      }
   }

   private BlockState cutCrop(Level world, BlockPos pos, BlockState state) {
      if (!(Boolean)AllConfigs.server().kinetics.harvesterReplants.get()) {
         return state.getFluidState().isEmpty() ? Blocks.AIR.defaultBlockState() : state.getFluidState().createLegacyBlock();
      } else {
         Block block = state.getBlock();
         if (block instanceof CropBlock crop) {
            BlockState newState = crop.getStateForAge(0);
            if (!newState.is(block)) {
               return newState;
            } else {
               IntegerProperty ageProperty = ((CropBlockAccessor)crop).create$callGetAgeProperty();
               return (BlockState)state.setValue(ageProperty, 0);
            }
         } else if (block == Blocks.SWEET_BERRY_BUSH) {
            return (BlockState)state.setValue(BlockStateProperties.AGE_3, 1);
         } else if (!AllTags.AllBlockTags.SUGAR_CANE_VARIANTS.matches(block) && !(block instanceof GrowingPlantBlock)) {
            if (state.getCollisionShape(world, pos).isEmpty() || block instanceof CocoaBlock) {
               for (Property<?> property : state.getProperties()) {
                  if (property instanceof IntegerProperty && property.getName().equals(BlockStateProperties.AGE_1.getName())) {
                     return (BlockState)state.setValue(property, 0);
                  }
               }
            }

            return state.getFluidState().isEmpty() ? Blocks.AIR.defaultBlockState() : state.getFluidState().createLegacyBlock();
         } else {
            return state.getFluidState().isEmpty() ? Blocks.AIR.defaultBlockState() : state.getFluidState().createLegacyBlock();
         }
      }
   }

   @Override
   public boolean disableBlockEntityRendering() {
      return true;
   }

   @Override
   public void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld, ContraptionMatrices matrices, MultiBufferSource buffers) {
      if (!VisualizationManager.supportsVisualization(context.world)) {
         HarvesterRenderer.renderInContraption(context, renderWorld, matrices, buffers);
      }
   }

   @Nullable
   @Override
   public ActorVisual createVisual(VisualizationContext visualizationContext, VirtualRenderWorld simulationWorld, MovementContext movementContext) {
      return new HarvesterActorVisual(visualizationContext, simulationWorld, movementContext);
   }
}
