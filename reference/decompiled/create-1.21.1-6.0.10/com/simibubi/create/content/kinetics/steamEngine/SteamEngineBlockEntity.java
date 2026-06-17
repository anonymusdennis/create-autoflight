package com.simibubi.create.content.kinetics.steamEngine;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.contraptions.bearing.WindmillBearingBlockEntity;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.advancement.CreateAdvancement;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import java.lang.ref.WeakReference;
import java.util.List;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

public class SteamEngineBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
   protected ScrollOptionBehaviour<WindmillBearingBlockEntity.RotationDirection> movementDirection;
   public WeakReference<PoweredShaftBlockEntity> target;
   public WeakReference<FluidTankBlockEntity> source;
   float prevAngle = 0.0F;

   public SteamEngineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
      this.source = new WeakReference<>(null);
      this.target = new WeakReference<>(null);
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      this.movementDirection = new ScrollOptionBehaviour<>(
         WindmillBearingBlockEntity.RotationDirection.class,
         CreateLang.translateDirect("contraptions.windmill.rotation_direction"),
         this,
         new SteamEngineValueBox()
      );
      this.movementDirection.onlyActiveWhen(() -> {
         PoweredShaftBlockEntity shaft = this.getShaft();
         return shaft == null || !shaft.hasSource();
      });
      this.movementDirection.withCallback($ -> this.onDirectionChanged());
      behaviours.add(this.movementDirection);
      this.registerAwardables(behaviours, new CreateAdvancement[]{AllAdvancements.STEAM_ENGINE});
   }

   private void onDirectionChanged() {
   }

   @Override
   public void tick() {
      super.tick();
      FluidTankBlockEntity tank = this.getTank();
      PoweredShaftBlockEntity shaft = this.getShaft();
      if (tank != null && shaft != null && this.isValid()) {
         boolean verticalTarget = false;
         BlockState shaftState = shaft.getBlockState();
         Axis targetAxis = Axis.X;
         if (shaftState.getBlock() instanceof IRotate ir) {
            targetAxis = ir.getRotationAxis(shaftState);
         }

         verticalTarget = targetAxis == Axis.Y;
         BlockState blockState = this.getBlockState();
         if (AllBlocks.STEAM_ENGINE.has(blockState)) {
            Direction facing = SteamEngineBlock.getFacing(blockState);
            if (facing.getAxis() == Axis.Y) {
               facing = (Direction)blockState.getValue(SteamEngineBlock.FACING);
            }

            float efficiency = Mth.clamp(tank.boiler.getEngineEfficiency(tank.getTotalTankSize()), 0.0F, 1.0F);
            if (efficiency > 0.0F) {
               this.award(AllAdvancements.STEAM_ENGINE);
            }

            int conveyedSpeedLevel = efficiency == 0.0F ? 1 : (verticalTarget ? 1 : (int)GeneratingKineticBlockEntity.convertToDirection(1.0F, facing));
            if (targetAxis == Axis.Z) {
               conveyedSpeedLevel *= -1;
            }

            if (this.movementDirection.get() == WindmillBearingBlockEntity.RotationDirection.COUNTER_CLOCKWISE) {
               conveyedSpeedLevel *= -1;
            }

            float shaftSpeed = shaft.getTheoreticalSpeed();
            if (shaft.hasSource() && shaftSpeed != 0.0F && conveyedSpeedLevel != 0 && shaftSpeed > 0.0F != conveyedSpeedLevel > 0) {
               this.movementDirection.setValue(1 - this.movementDirection.get().ordinal());
               conveyedSpeedLevel *= -1;
            }

            shaft.update(this.worldPosition, conveyedSpeedLevel, efficiency);
            if (this.level.isClientSide) {
               CatnipServices.PLATFORM.executeOnClientOnly(() -> this::spawnParticles);
            }
         }
      } else if (!this.level.isClientSide()) {
         if (shaft != null) {
            if (shaft.getBlockPos().subtract(this.worldPosition).equals(shaft.enginePos)) {
               if (shaft.engineEfficiency != 0.0F) {
                  Direction facingx = SteamEngineBlock.getFacing(this.getBlockState());
                  if (this.level.isLoaded(this.worldPosition.relative(facingx.getOpposite()))) {
                     shaft.update(this.worldPosition, 0, 0.0F);
                  }
               }
            }
         }
      }
   }

   @Override
   public void remove() {
      PoweredShaftBlockEntity shaft = this.getShaft();
      if (shaft != null) {
         shaft.remove(this.worldPosition);
      }

      super.remove();
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   protected AABB createRenderBoundingBox() {
      return super.createRenderBoundingBox().inflate(2.0);
   }

   public PoweredShaftBlockEntity getShaft() {
      PoweredShaftBlockEntity shaft = this.target.get();
      if (shaft == null || shaft.isRemoved() || !shaft.canBePoweredBy(this.worldPosition)) {
         if (shaft != null) {
            this.target = new WeakReference<>(null);
         }

         Direction facing = SteamEngineBlock.getFacing(this.getBlockState());
         if (this.level.getBlockEntity(this.worldPosition.relative(facing, 2)) instanceof PoweredShaftBlockEntity ps && ps.canBePoweredBy(this.worldPosition)) {
            shaft = ps;
            this.target = new WeakReference<>(ps);
         }
      }

      return shaft;
   }

   public FluidTankBlockEntity getTank() {
      FluidTankBlockEntity tank = this.source.get();
      if (tank == null || tank.isRemoved()) {
         if (tank != null) {
            this.source = new WeakReference<>(null);
         }

         Direction facing = SteamEngineBlock.getFacing(this.getBlockState());
         if (this.level.getBlockEntity(this.worldPosition.relative(facing.getOpposite())) instanceof FluidTankBlockEntity tankBe) {
            tank = tankBe;
            this.source = new WeakReference<>(tankBe);
         }
      }

      return tank == null ? null : tank.getControllerBE();
   }

   public boolean isValid() {
      Direction dir = SteamEngineBlock.getConnectedDirection(this.getBlockState()).getOpposite();
      Level level = this.getLevel();
      return level == null ? false : level.getBlockState(this.getBlockPos().relative(dir)).is((Block)AllBlocks.FLUID_TANK.get());
   }

   @OnlyIn(Dist.CLIENT)
   private void spawnParticles() {
      Float targetAngle = this.getTargetAngle();
      PoweredShaftBlockEntity ste = this.target.get();
      if (ste != null) {
         if (ste.isPoweredBy(this.worldPosition) && ste.engineEfficiency != 0.0F) {
            if (targetAngle != null) {
               float angle = AngleHelper.deg((double)targetAngle.floatValue());
               angle += angle < 0.0F ? -105.0F : 285.0F;
               angle %= 360.0F;
               PoweredShaftBlockEntity shaft = this.getShaft();
               if (shaft != null && shaft.getSpeed() != 0.0F) {
                  if (!(angle >= 0.0F) || this.prevAngle > 180.0F && angle < 180.0F) {
                     if (!(angle < 0.0F) || this.prevAngle < -180.0F && angle > -180.0F) {
                        FluidTankBlockEntity sourceBE = this.source.get();
                        if (sourceBE != null) {
                           FluidTankBlockEntity controller = sourceBE.getControllerBE();
                           if (controller != null && controller.boiler != null) {
                              controller.boiler.queueSoundOnSide(this.worldPosition, SteamEngineBlock.getFacing(this.getBlockState()));
                           }
                        }

                        Direction facing = SteamEngineBlock.getFacing(this.getBlockState());
                        Vec3 offset = VecHelper.rotate(
                           new Vec3(0.0, 0.0, 1.0)
                              .add(VecHelper.offsetRandomly(Vec3.ZERO, this.level.random, 1.0F).multiply(1.0, 1.0, 0.0).normalize().scale(0.5)),
                           (double)AngleHelper.verticalAngle(facing),
                           Axis.X
                        );
                        offset = VecHelper.rotate(offset, (double)AngleHelper.horizontalAngle(facing), Axis.Y);
                        Vec3 v = offset.scale(0.5).add(Vec3.atCenterOf(this.worldPosition));
                        Vec3 m = offset.subtract(Vec3.atLowerCornerOf(facing.getNormal()).scale(0.75));
                        this.level.addParticle(new SteamJetParticleData(1.0F), v.x, v.y, v.z, m.x, m.y, m.z);
                        this.prevAngle = angle;
                     } else {
                        this.prevAngle = angle;
                     }
                  } else {
                     this.prevAngle = angle;
                  }
               }
            }
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   @Nullable
   public Float getTargetAngle() {
      float angle = 0.0F;
      BlockState blockState = this.getBlockState();
      if (!AllBlocks.STEAM_ENGINE.has(blockState)) {
         return null;
      } else {
         Direction facing = SteamEngineBlock.getFacing(blockState);
         PoweredShaftBlockEntity shaft = this.getShaft();
         Axis facingAxis = facing.getAxis();
         Axis axis = Axis.Y;
         if (shaft == null) {
            return null;
         } else {
            axis = KineticBlockEntityRenderer.getRotationAxisOf(shaft);
            angle = KineticBlockEntityRenderer.getAngleForBe(shaft, shaft.getBlockPos(), axis);
            if (axis == facingAxis) {
               return null;
            } else {
               if (axis.isHorizontal() && facingAxis == Axis.X ^ facing.getAxisDirection() == AxisDirection.POSITIVE) {
                  angle *= -1.0F;
               }

               if (axis == Axis.X && facing == Direction.DOWN) {
                  angle *= -1.0F;
               }

               return angle;
            }
         }
      }
   }

   @Override
   public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
      PoweredShaftBlockEntity shaft = this.getShaft();
      return shaft == null ? false : shaft.addToEngineTooltip(tooltip, isPlayerSneaking);
   }
}
