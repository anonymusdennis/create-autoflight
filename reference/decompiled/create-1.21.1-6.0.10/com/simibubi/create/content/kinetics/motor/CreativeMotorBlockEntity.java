package com.simibubi.create.content.kinetics.motor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.compat.Mods;
import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;
import com.simibubi.create.compat.computercraft.ComputerCraftProxy;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import dan200.computercraft.api.peripheral.PeripheralCapability;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import java.util.List;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public class CreativeMotorBlockEntity extends GeneratingKineticBlockEntity {
   public static final int DEFAULT_SPEED = 16;
   public static final int MAX_SPEED = 256;
   public ScrollValueBehaviour generatedSpeed;
   public AbstractComputerBehaviour computerBehaviour;

   public CreativeMotorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   public static void registerCapabilities(RegisterCapabilitiesEvent event) {
      if (Mods.COMPUTERCRAFT.isLoaded()) {
         event.registerBlockEntity(
            PeripheralCapability.get(), (BlockEntityType)AllBlockEntityTypes.MOTOR.get(), (be, context) -> be.computerBehaviour.getPeripheralCapability()
         );
      }
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      super.addBehaviours(behaviours);
      int max = 256;
      this.generatedSpeed = new KineticScrollValueBehaviour(
         CreateLang.translateDirect("kinetics.creative_motor.rotation_speed"), this, new CreativeMotorBlockEntity.MotorValueBox()
      );
      this.generatedSpeed.between(-max, max);
      this.generatedSpeed.value = 16;
      this.generatedSpeed.withCallback(i -> this.updateGeneratedRotation());
      behaviours.add(this.generatedSpeed);
      behaviours.add(this.computerBehaviour = ComputerCraftProxy.behaviour(this));
   }

   @Override
   public void initialize() {
      super.initialize();
      if (!this.hasSource() || this.getGeneratedSpeed() > this.getTheoreticalSpeed()) {
         this.updateGeneratedRotation();
      }
   }

   @Override
   public float getGeneratedSpeed() {
      return !AllBlocks.CREATIVE_MOTOR.has(this.getBlockState())
         ? 0.0F
         : convertToDirection((float)this.generatedSpeed.getValue(), (Direction)this.getBlockState().getValue(CreativeMotorBlock.FACING));
   }

   @Override
   public void invalidate() {
      super.invalidate();
      this.computerBehaviour.removePeripheral();
   }

   static class MotorValueBox extends ValueBoxTransform.Sided {
      @Override
      protected Vec3 getSouthLocation() {
         return VecHelper.voxelSpace(8.0, 8.0, 12.5);
      }

      @Override
      public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
         Direction facing = (Direction)state.getValue(CreativeMotorBlock.FACING);
         return super.getLocalOffset(level, pos, state).add(Vec3.atLowerCornerOf(facing.getNormal()).scale(-0.0625));
      }

      @Override
      public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
         super.rotate(level, pos, state, ms);
         Direction facing = (Direction)state.getValue(CreativeMotorBlock.FACING);
         if (facing.getAxis() != Axis.Y) {
            if (this.getSide() == Direction.UP) {
               TransformStack.of(ms).rotateZDegrees(-AngleHelper.horizontalAngle(facing) + 180.0F);
            }
         }
      }

      @Override
      protected boolean isSideActive(BlockState state, Direction direction) {
         Direction facing = (Direction)state.getValue(CreativeMotorBlock.FACING);
         return facing.getAxis() != Axis.Y && direction == Direction.DOWN ? false : direction.getAxis() != facing.getAxis();
      }
   }
}
