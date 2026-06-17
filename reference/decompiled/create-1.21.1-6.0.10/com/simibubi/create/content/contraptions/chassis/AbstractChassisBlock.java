package com.simibubi.create.content.contraptions.chassis;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.api.contraption.transformable.TransformableBlock;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags.Items;

public abstract class AbstractChassisBlock extends RotatedPillarBlock implements IWrenchable, IBE<ChassisBlockEntity>, TransformableBlock {
   public AbstractChassisBlock(Properties properties) {
      super(properties);
   }

   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
   ) {
      if (!player.mayBuild()) {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      } else {
         boolean isSlimeBall = stack.is(Items.SLIMEBALLS) || AllItems.SUPER_GLUE.isIn(stack);
         BooleanProperty affectedSide = this.getGlueableSide(state, hitResult.getDirection());
         if (affectedSide == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
         } else if (isSlimeBall && (Boolean)state.getValue(affectedSide)) {
            for (Direction face : Iterate.directions) {
               BooleanProperty glueableSide = this.getGlueableSide(state, face);
               if (glueableSide != null && !(Boolean)state.getValue(glueableSide) && this.glueAllowedOnSide(level, pos, state, face)) {
                  if (level.isClientSide) {
                     Vec3 vec = hitResult.getLocation();
                     level.addParticle(ParticleTypes.ITEM_SLIME, vec.x, vec.y, vec.z, 0.0, 0.0, 0.0);
                     return ItemInteractionResult.SUCCESS;
                  }

                  AllSoundEvents.SLIME_ADDED.playOnServer(level, pos, 0.5F, 1.0F);
                  state = (BlockState)state.setValue(glueableSide, true);
               }
            }

            if (!level.isClientSide) {
               level.setBlockAndUpdate(pos, state);
            }

            return ItemInteractionResult.SUCCESS;
         } else if ((!stack.isEmpty() || !player.isShiftKeyDown()) && !isSlimeBall) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
         } else if ((Boolean)state.getValue(affectedSide) == isSlimeBall) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
         } else if (!this.glueAllowedOnSide(level, pos, state, hitResult.getDirection())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
         } else if (level.isClientSide) {
            Vec3 vec = hitResult.getLocation();
            level.addParticle(ParticleTypes.ITEM_SLIME, vec.x, vec.y, vec.z, 0.0, 0.0, 0.0);
            return ItemInteractionResult.SUCCESS;
         } else {
            AllSoundEvents.SLIME_ADDED.playOnServer(level, pos, 0.5F, 1.0F);
            level.setBlockAndUpdate(pos, (BlockState)state.setValue(affectedSide, isSlimeBall));
            return ItemInteractionResult.SUCCESS;
         }
      }
   }

   public BlockState rotate(BlockState state, Rotation rotation) {
      if (rotation == Rotation.NONE) {
         return state;
      } else {
         BlockState rotated = super.rotate(state, rotation);

         for (Direction face : Iterate.directions) {
            BooleanProperty glueableSide = this.getGlueableSide(rotated, face);
            if (glueableSide != null) {
               rotated = (BlockState)rotated.setValue(glueableSide, false);
            }
         }

         for (Direction facex : Iterate.directions) {
            BooleanProperty glueableSide = this.getGlueableSide(state, facex);
            if (glueableSide != null && (Boolean)state.getValue(glueableSide)) {
               Direction rotatedFacing = rotation.rotate(facex);
               BooleanProperty rotatedGlueableSide = this.getGlueableSide(rotated, rotatedFacing);
               if (rotatedGlueableSide != null) {
                  rotated = (BlockState)rotated.setValue(rotatedGlueableSide, true);
               }
            }
         }

         return rotated;
      }
   }

   public BlockState mirror(BlockState state, Mirror mirrorIn) {
      if (mirrorIn == Mirror.NONE) {
         return state;
      } else {
         BlockState mirrored = state;

         for (Direction face : Iterate.directions) {
            BooleanProperty glueableSide = this.getGlueableSide(mirrored, face);
            if (glueableSide != null) {
               mirrored = (BlockState)mirrored.setValue(glueableSide, false);
            }
         }

         for (Direction facex : Iterate.directions) {
            BooleanProperty glueableSide = this.getGlueableSide(state, facex);
            if (glueableSide != null && (Boolean)state.getValue(glueableSide)) {
               Direction mirroredFacing = mirrorIn.mirror(facex);
               BooleanProperty mirroredGlueableSide = this.getGlueableSide(mirrored, mirroredFacing);
               if (mirroredGlueableSide != null) {
                  mirrored = (BlockState)mirrored.setValue(mirroredGlueableSide, true);
               }
            }
         }

         return mirrored;
      }
   }

   @Override
   public BlockState transform(BlockState state, StructureTransform transform) {
      if (transform.mirror != null) {
         state = this.mirror(state, transform.mirror);
      }

      return transform.rotationAxis == Axis.Y ? this.rotate(state, transform.rotation) : this.transformInner(state, transform);
   }

   protected BlockState transformInner(BlockState state, StructureTransform transform) {
      if (transform.rotation == Rotation.NONE) {
         return state;
      } else {
         BlockState rotated = (BlockState)state.setValue(AXIS, transform.rotateAxis((Axis)state.getValue(AXIS)));
         AbstractChassisBlock block = (AbstractChassisBlock)state.getBlock();

         for (Direction face : Iterate.directions) {
            BooleanProperty glueableSide = block.getGlueableSide(rotated, face);
            if (glueableSide != null) {
               rotated = (BlockState)rotated.setValue(glueableSide, false);
            }
         }

         for (Direction facex : Iterate.directions) {
            BooleanProperty glueableSide = block.getGlueableSide(state, facex);
            if (glueableSide != null && (Boolean)state.getValue(glueableSide)) {
               Direction rotatedFacing = transform.rotateFacing(facex);
               BooleanProperty rotatedGlueableSide = block.getGlueableSide(rotated, rotatedFacing);
               if (rotatedGlueableSide != null) {
                  rotated = (BlockState)rotated.setValue(rotatedGlueableSide, true);
               }
            }
         }

         return rotated;
      }
   }

   public abstract BooleanProperty getGlueableSide(BlockState var1, Direction var2);

   protected boolean glueAllowedOnSide(BlockGetter world, BlockPos pos, BlockState state, Direction side) {
      return true;
   }

   @Override
   public Class<ChassisBlockEntity> getBlockEntityClass() {
      return ChassisBlockEntity.class;
   }

   @Override
   public BlockEntityType<? extends ChassisBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends ChassisBlockEntity>)AllBlockEntityTypes.CHASSIS.get();
   }
}
