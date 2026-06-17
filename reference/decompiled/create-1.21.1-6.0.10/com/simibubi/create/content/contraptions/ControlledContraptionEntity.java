package com.simibubi.create.content.contraptions;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllEntityTypes;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.bearing.BearingContraption;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class ControlledContraptionEntity extends AbstractContraptionEntity {
   protected BlockPos controllerPos;
   protected Axis rotationAxis;
   protected float prevAngle;
   protected float angle;
   protected float angleDelta;

   public ControlledContraptionEntity(EntityType<?> type, Level world) {
      super(type, world);
   }

   public static ControlledContraptionEntity create(Level world, IControlContraption controller, Contraption contraption) {
      ControlledContraptionEntity entity = new ControlledContraptionEntity((EntityType<?>)AllEntityTypes.CONTROLLED_CONTRAPTION.get(), world);
      entity.controllerPos = controller.getBlockPosition();
      entity.setContraption(contraption);
      return entity;
   }

   @Override
   public void setPos(double x, double y, double z) {
      super.setPos(x, y, z);
      if (this.level().isClientSide()) {
         for (Entity entity : this.getPassengers()) {
            this.positionRider(entity);
         }
      }
   }

   @Override
   public Vec3 getContactPointMotion(Vec3 globalContactPoint) {
      return this.contraption instanceof TranslatingContraption ? this.getDeltaMovement() : super.getContactPointMotion(globalContactPoint);
   }

   @Override
   protected void setContraption(Contraption contraption) {
      super.setContraption(contraption);
      if (contraption instanceof BearingContraption) {
         this.rotationAxis = ((BearingContraption)contraption).getFacing().getAxis();
      }
   }

   @Override
   protected void readAdditional(CompoundTag compound, boolean spawnPacket) {
      super.readAdditional(compound, spawnPacket);
      if (compound.contains("ControllerRelative")) {
         this.controllerPos = NBTHelper.readBlockPos(compound, "ControllerRelative").offset(this.blockPosition());
      }

      if (compound.contains("Axis")) {
         this.rotationAxis = (Axis)NBTHelper.readEnum(compound, "Axis", Axis.class);
      }

      this.angle = compound.getFloat("Angle");
   }

   @Override
   protected void writeAdditional(CompoundTag compound, Provider registries, boolean spawnPacket) {
      super.writeAdditional(compound, registries, spawnPacket);
      compound.put("ControllerRelative", NbtUtils.writeBlockPos(this.controllerPos.subtract(this.blockPosition())));
      if (this.rotationAxis != null) {
         NBTHelper.writeEnum(compound, "Axis", this.rotationAxis);
      }

      compound.putFloat("Angle", this.angle);
   }

   @Override
   public AbstractContraptionEntity.ContraptionRotationState getRotationState() {
      AbstractContraptionEntity.ContraptionRotationState crs = new AbstractContraptionEntity.ContraptionRotationState();
      if (this.rotationAxis == Axis.X) {
         crs.xRotation = this.angle;
      }

      if (this.rotationAxis == Axis.Y) {
         crs.yRotation = this.angle;
      }

      if (this.rotationAxis == Axis.Z) {
         crs.zRotation = this.angle;
      }

      return crs;
   }

   @Override
   public Vec3 applyRotation(Vec3 localPos, float partialTicks) {
      return VecHelper.rotate(localPos, (double)this.getAngle(partialTicks), this.rotationAxis);
   }

   @Override
   public Vec3 reverseRotation(Vec3 localPos, float partialTicks) {
      return VecHelper.rotate(localPos, (double)(-this.getAngle(partialTicks)), this.rotationAxis);
   }

   public void setAngle(float angle) {
      this.angle = angle;
      if (this.level().isClientSide()) {
         for (Entity entity : this.getPassengers()) {
            this.positionRider(entity);
         }
      }
   }

   public float getAngle(float partialTicks) {
      return partialTicks == 1.0F ? this.angle : AngleHelper.angleLerp((double)partialTicks, (double)this.prevAngle, (double)this.angle);
   }

   public void setRotationAxis(Axis rotationAxis) {
      this.rotationAxis = rotationAxis;
   }

   public Axis getRotationAxis() {
      return this.rotationAxis;
   }

   public void teleportTo(double p_70634_1_, double p_70634_3_, double p_70634_5_) {
   }

   @OnlyIn(Dist.CLIENT)
   public void lerpTo(double pX, double pY, double pZ, float pYRot, float pXRot, int pSteps) {
   }

   @Override
   protected void tickContraption() {
      this.angleDelta = this.angle - this.prevAngle;
      this.prevAngle = this.angle;
      this.tickActors();
      if (this.controllerPos != null) {
         if (this.level().isLoaded(this.controllerPos)) {
            IControlContraption controller = this.getController();
            if (controller == null) {
               this.discard();
            } else {
               if (!controller.isAttachedTo(this)) {
                  controller.attach(this);
                  if (this.level().isClientSide) {
                     this.setPos(this.getX(), this.getY(), this.getZ());
                  }
               }
            }
         }
      }
   }

   @Override
   protected boolean shouldActorTrigger(
      MovementContext context, StructureBlockInfo blockInfo, MovementBehaviour actor, Vec3 actorPosition, BlockPos gridPosition
   ) {
      if (super.shouldActorTrigger(context, blockInfo, actor, actorPosition, gridPosition)) {
         return true;
      } else if (this.contraption instanceof BearingContraption bc) {
         Direction var10 = bc.getFacing();
         Vec3 activeAreaOffset = actor.getActiveAreaOffset(context);
         if (!activeAreaOffset.multiply(VecHelper.axisAlingedPlaneOf(Vec3.atLowerCornerOf(var10.getNormal()))).equals(Vec3.ZERO)) {
            return false;
         } else if (!VecHelper.onSameAxis(blockInfo.pos(), BlockPos.ZERO, var10.getAxis())) {
            return false;
         } else {
            context.motion = Vec3.atLowerCornerOf(var10.getNormal()).scale((double)this.angleDelta / 360.0);
            context.relativeMotion = context.motion;
            int timer = context.data.getInt("StationaryTimer");
            if (timer > 0) {
               context.data.putInt("StationaryTimer", timer - 1);
               return false;
            } else {
               context.data.putInt("StationaryTimer", 20);
               return true;
            }
         }
      } else {
         return false;
      }
   }

   protected IControlContraption getController() {
      if (this.controllerPos == null) {
         return null;
      } else if (!this.level().isLoaded(this.controllerPos)) {
         return null;
      } else {
         BlockEntity be = this.level().getBlockEntity(this.controllerPos);
         return !(be instanceof IControlContraption) ? null : (IControlContraption)be;
      }
   }

   @Override
   protected StructureTransform makeStructureTransform() {
      BlockPos offset = BlockPos.containing(this.getAnchorVec().add(0.5, 0.5, 0.5));
      float xRot = this.rotationAxis == Axis.X ? this.angle : 0.0F;
      float yRot = this.rotationAxis == Axis.Y ? this.angle : 0.0F;
      float zRot = this.rotationAxis == Axis.Z ? this.angle : 0.0F;
      return new StructureTransform(offset, xRot, yRot, zRot);
   }

   @Override
   protected void onContraptionStalled() {
      IControlContraption controller = this.getController();
      if (controller != null) {
         controller.onStall();
      }

      super.onContraptionStalled();
   }

   @Override
   protected float getStalledAngle() {
      return this.angle;
   }

   @Override
   protected void handleStallInformation(double x, double y, double z, float angle) {
      this.setPosRaw(x, y, z);
      this.angle = this.prevAngle = angle;
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void applyLocalTransforms(PoseStack matrixStack, float partialTicks) {
      float angle = this.getAngle(partialTicks);
      Axis axis = this.getRotationAxis();
      TransformStack.of(matrixStack).nudge(this.getId());
      if (axis != null) {
         ((PoseTransformStack)((PoseTransformStack)TransformStack.of(matrixStack).center()).rotateDegrees(angle, axis)).uncenter();
      }
   }
}
