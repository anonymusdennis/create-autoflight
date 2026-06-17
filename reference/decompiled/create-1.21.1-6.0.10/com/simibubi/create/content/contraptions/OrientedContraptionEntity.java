package com.simibubi.create.content.contraptions;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllAttachmentTypes;
import com.simibubi.create.AllEntityTypes;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageWrapper;
import com.simibubi.create.content.contraptions.bearing.StabilizedContraption;
import com.simibubi.create.content.contraptions.minecart.MinecartSim2020;
import com.simibubi.create.content.contraptions.minecart.capability.CapabilityMinecartController;
import com.simibubi.create.content.contraptions.minecart.capability.MinecartController;
import com.simibubi.create.content.contraptions.mounted.CartAssemblerBlockEntity;
import com.simibubi.create.content.contraptions.mounted.MountedContraption;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.mixin.accessor.MinecartFurnaceAccessor;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import java.util.Optional;
import java.util.UUID;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.MinecartFurnace;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

public class OrientedContraptionEntity extends AbstractContraptionEntity {
   private static final Ingredient FUEL_ITEMS = Ingredient.of(new ItemLike[]{Items.COAL, Items.CHARCOAL});
   private static final EntityDataAccessor<Optional<UUID>> COUPLING = SynchedEntityData.defineId(
      OrientedContraptionEntity.class, EntityDataSerializers.OPTIONAL_UUID
   );
   private static final EntityDataAccessor<Direction> INITIAL_ORIENTATION = SynchedEntityData.defineId(
      OrientedContraptionEntity.class, EntityDataSerializers.DIRECTION
   );
   protected Vec3 motionBeforeStall = Vec3.ZERO;
   protected boolean forceAngle;
   private boolean attachedExtraInventories = false;
   private boolean manuallyPlaced;
   public float prevYaw;
   public float yaw;
   public float targetYaw;
   public float prevPitch;
   public float pitch;
   public int nonDamageTicks = 10;

   public OrientedContraptionEntity(EntityType<?> type, Level world) {
      super(type, world);
   }

   public static OrientedContraptionEntity create(Level world, Contraption contraption, Direction initialOrientation) {
      OrientedContraptionEntity entity = new OrientedContraptionEntity((EntityType<?>)AllEntityTypes.ORIENTED_CONTRAPTION.get(), world);
      entity.setContraption(contraption);
      entity.setInitialOrientation(initialOrientation);
      entity.startAtInitialYaw();
      return entity;
   }

   public static OrientedContraptionEntity createAtYaw(Level world, Contraption contraption, Direction initialOrientation, float initialYaw) {
      OrientedContraptionEntity entity = create(world, contraption, initialOrientation);
      entity.startAtYaw(initialYaw);
      entity.manuallyPlaced = true;
      return entity;
   }

   public void setInitialOrientation(Direction direction) {
      this.entityData.set(INITIAL_ORIENTATION, direction);
   }

   public Direction getInitialOrientation() {
      return (Direction)this.entityData.get(INITIAL_ORIENTATION);
   }

   @Override
   public float getYawOffset() {
      return this.getInitialYaw();
   }

   public float getInitialYaw() {
      return (this.isInitialOrientationPresent() ? (Direction)this.entityData.get(INITIAL_ORIENTATION) : Direction.SOUTH).toYRot();
   }

   @Override
   protected void defineSynchedData(Builder builder) {
      super.defineSynchedData(builder);
      builder.define(COUPLING, Optional.empty());
      builder.define(INITIAL_ORIENTATION, Direction.UP);
   }

   @Override
   public AbstractContraptionEntity.ContraptionRotationState getRotationState() {
      AbstractContraptionEntity.ContraptionRotationState crs = new AbstractContraptionEntity.ContraptionRotationState();
      float yawOffset = this.getYawOffset();
      crs.zRotation = this.pitch;
      crs.yRotation = -this.yaw + yawOffset;
      if (this.pitch != 0.0F && this.yaw != 0.0F) {
         crs.secondYRotation = -this.yaw;
         crs.yRotation = yawOffset;
      }

      return crs;
   }

   public void stopRiding() {
      if (!this.level().isClientSide && this.isAlive()) {
         this.disassemble();
      }

      super.stopRiding();
   }

   @Override
   protected void readAdditional(CompoundTag compound, boolean spawnPacket) {
      super.readAdditional(compound, spawnPacket);
      if (compound.contains("InitialOrientation")) {
         this.setInitialOrientation((Direction)NBTHelper.readEnum(compound, "InitialOrientation", Direction.class));
      }

      this.yaw = compound.getFloat("Yaw");
      this.pitch = compound.getFloat("Pitch");
      this.manuallyPlaced = compound.getBoolean("Placed");
      if (compound.contains("ForceYaw")) {
         this.startAtYaw(compound.getFloat("ForceYaw"));
      }

      ListTag vecNBT = compound.getList("CachedMotion", 6);
      if (!vecNBT.isEmpty()) {
         this.motionBeforeStall = new Vec3(vecNBT.getDouble(0), vecNBT.getDouble(1), vecNBT.getDouble(2));
         if (!this.motionBeforeStall.equals(Vec3.ZERO)) {
            this.targetYaw = this.prevYaw = this.yaw = this.yaw + yawFromVector(this.motionBeforeStall);
         }

         this.setDeltaMovement(Vec3.ZERO);
      }

      this.setCouplingId(compound.contains("OnCoupling") ? compound.getUUID("OnCoupling") : null);
   }

   @Override
   protected void writeAdditional(CompoundTag compound, Provider registries, boolean spawnPacket) {
      super.writeAdditional(compound, registries, spawnPacket);
      if (this.motionBeforeStall != null) {
         compound.put("CachedMotion", this.newDoubleList(new double[]{this.motionBeforeStall.x, this.motionBeforeStall.y, this.motionBeforeStall.z}));
      }

      Direction optional = (Direction)this.entityData.get(INITIAL_ORIENTATION);
      if (optional.getAxis().isHorizontal()) {
         NBTHelper.writeEnum(compound, "InitialOrientation", optional);
      }

      if (this.forceAngle) {
         compound.putFloat("ForceYaw", this.yaw);
         this.forceAngle = false;
      }

      compound.putBoolean("Placed", this.manuallyPlaced);
      compound.putFloat("Yaw", this.yaw);
      compound.putFloat("Pitch", this.pitch);
      if (this.getCouplingId() != null) {
         compound.putUUID("OnCoupling", this.getCouplingId());
      }
   }

   public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
      super.onSyncedDataUpdated(key);
      if (INITIAL_ORIENTATION.equals(key) && this.isInitialOrientationPresent() && !this.manuallyPlaced) {
         this.startAtInitialYaw();
      }
   }

   public boolean isInitialOrientationPresent() {
      return ((Direction)this.entityData.get(INITIAL_ORIENTATION)).getAxis().isHorizontal();
   }

   public void startAtInitialYaw() {
      this.startAtYaw(this.getInitialYaw());
   }

   public void startAtYaw(float yaw) {
      this.targetYaw = this.yaw = this.prevYaw = yaw;
      this.forceAngle = true;
   }

   @Override
   public Vec3 applyRotation(Vec3 localPos, float partialTicks) {
      localPos = VecHelper.rotate(localPos, (double)this.getInitialYaw(), Axis.Y);
      localPos = VecHelper.rotate(localPos, (double)this.getViewXRot(partialTicks), Axis.Z);
      return VecHelper.rotate(localPos, (double)this.getViewYRot(partialTicks), Axis.Y);
   }

   @Override
   public Vec3 reverseRotation(Vec3 localPos, float partialTicks) {
      localPos = VecHelper.rotate(localPos, (double)(-this.getViewYRot(partialTicks)), Axis.Y);
      localPos = VecHelper.rotate(localPos, (double)(-this.getViewXRot(partialTicks)), Axis.Z);
      return VecHelper.rotate(localPos, (double)(-this.getInitialYaw()), Axis.Y);
   }

   public float getViewYRot(float partialTicks) {
      return -(partialTicks == 1.0F ? this.yaw : AngleHelper.angleLerp((double)partialTicks, (double)this.prevYaw, (double)this.yaw));
   }

   public float getViewXRot(float partialTicks) {
      return partialTicks == 1.0F ? this.pitch : AngleHelper.angleLerp((double)partialTicks, (double)this.prevPitch, (double)this.pitch);
   }

   @Override
   protected void tickContraption() {
      if (this.nonDamageTicks > 0) {
         this.nonDamageTicks--;
      }

      Entity e = this.getVehicle();
      if (e != null) {
         boolean rotationLock = false;
         boolean pauseWhileRotating = false;
         boolean wasStalled = this.isStalled();
         if (this.contraption instanceof MountedContraption mountedContraption) {
            rotationLock = mountedContraption.rotationMode == CartAssemblerBlockEntity.CartMovementMode.ROTATION_LOCKED;
            pauseWhileRotating = mountedContraption.rotationMode == CartAssemblerBlockEntity.CartMovementMode.ROTATE_PAUSED;
         }

         Entity riding = e;

         while (riding.getVehicle() != null && !(this.contraption instanceof StabilizedContraption)) {
            riding = riding.getVehicle();
         }

         boolean isOnCoupling = false;
         UUID couplingId = this.getCouplingId();
         isOnCoupling = couplingId != null && riding instanceof AbstractMinecart;
         if (!this.attachedExtraInventories) {
            this.attachInventoriesFromRidingCarts(riding, isOnCoupling, couplingId);
            this.attachedExtraInventories = true;
         }

         boolean rotating = this.updateOrientation(rotationLock, wasStalled, riding, isOnCoupling);
         if (!rotating || !pauseWhileRotating) {
            this.tickActors();
         }

         boolean isStalled = this.isStalled();
         MinecartController controller = (MinecartController)riding.getData(AllAttachmentTypes.MINECART_CONTROLLER);
         if (controller.isPresent()) {
            if (!this.level().isClientSide()) {
               controller.setStalledExternally(isStalled);
            }
         } else {
            if (isStalled) {
               if (!wasStalled) {
                  this.motionBeforeStall = riding.getDeltaMovement();
               }

               riding.setDeltaMovement(0.0, 0.0, 0.0);
            }

            if (wasStalled && !isStalled) {
               riding.setDeltaMovement(this.motionBeforeStall);
               this.motionBeforeStall = Vec3.ZERO;
            }
         }

         if (!this.level().isClientSide) {
            if (!this.isStalled()) {
               if (isOnCoupling) {
                  Couple<MinecartController> coupledCarts = this.getCoupledCartsIfPresent();
                  if (coupledCarts == null) {
                     return;
                  }

                  coupledCarts.map(MinecartController::cart).forEach(this::powerFurnaceCartWithFuelFromStorage);
                  return;
               }

               this.powerFurnaceCartWithFuelFromStorage(riding);
            }
         }
      }
   }

   protected boolean updateOrientation(boolean rotationLock, boolean wasStalled, Entity riding, boolean isOnCoupling) {
      if (isOnCoupling) {
         Couple<MinecartController> coupledCarts = this.getCoupledCartsIfPresent();
         if (coupledCarts == null) {
            return false;
         } else {
            Vec3 positionVec = ((MinecartController)coupledCarts.getFirst()).cart().position();
            Vec3 coupledVec = ((MinecartController)coupledCarts.getSecond()).cart().position();
            double diffX = positionVec.x - coupledVec.x;
            double diffY = positionVec.y - coupledVec.y;
            double diffZ = positionVec.z - coupledVec.z;
            this.prevYaw = this.yaw;
            this.prevPitch = this.pitch;
            this.yaw = (float)(Mth.atan2(diffZ, diffX) * 180.0 / Math.PI);
            this.pitch = (float)(Math.atan2(diffY, Math.sqrt(diffX * diffX + diffZ * diffZ)) * 180.0 / Math.PI);
            if (this.getCouplingId().equals(riding.getUUID())) {
               this.pitch *= -1.0F;
               this.yaw += 180.0F;
            }

            return false;
         }
      } else if (this.contraption instanceof StabilizedContraption stabilized) {
         if (riding instanceof OrientedContraptionEntity parent) {
            Direction facing = stabilized.getFacing();
            if (facing.getAxis().isVertical()) {
               return false;
            } else {
               this.prevYaw = this.yaw;
               this.yaw = AngleHelper.wrapAngle180(this.getInitialYaw() - parent.getInitialYaw()) - parent.getViewYRot(1.0F);
               return false;
            }
         } else {
            return false;
         }
      } else {
         this.prevYaw = this.yaw;
         if (wasStalled) {
            return false;
         } else {
            boolean rotating = false;
            Vec3 movementVector = riding.getDeltaMovement();
            Vec3 locationDiff = riding.position().subtract(riding.xo, riding.yo, riding.zo);
            if (!(riding instanceof AbstractMinecart)) {
               movementVector = locationDiff;
            }

            Vec3 motion = movementVector.normalize();
            if (!rotationLock) {
               if (riding instanceof AbstractMinecart minecartEntity) {
                  BlockPos railPosition = minecartEntity.getCurrentRailPosition();
                  BlockState blockState = this.level().getBlockState(railPosition);
                  if (blockState.getBlock() instanceof BaseRailBlock abstractRailBlock) {
                     RailShape railDirection = abstractRailBlock.getRailDirection(blockState, this.level(), railPosition, minecartEntity);
                     motion = VecHelper.project(motion, MinecartSim2020.getRailVec(railDirection));
                  }
               }

               if (motion.length() > 0.0) {
                  this.targetYaw = yawFromVector(motion);
                  if (this.targetYaw < 0.0F) {
                     this.targetYaw += 360.0F;
                  }

                  if (this.yaw < 0.0F) {
                     this.yaw += 360.0F;
                  }
               }

               this.prevYaw = this.yaw;
               float maxApproachSpeed = (float)(motion.length() * 12.0 / Math.max(1.0, this.getBoundingBox().getXsize() / 6.0));
               float yawHint = AngleHelper.getShortestAngleDiff((double)this.yaw, (double)yawFromVector(locationDiff));
               float approach = AngleHelper.getShortestAngleDiff((double)this.yaw, (double)this.targetYaw, yawHint);
               approach = Mth.clamp(approach, -maxApproachSpeed, maxApproachSpeed);
               this.yaw += approach;
               if (Math.abs(AngleHelper.getShortestAngleDiff((double)this.yaw, (double)this.targetYaw)) < 1.0F) {
                  this.yaw = this.targetYaw;
               } else {
                  rotating = true;
               }
            }

            return rotating;
         }
      }
   }

   protected void powerFurnaceCartWithFuelFromStorage(Entity riding) {
      if (riding instanceof MinecartFurnace furnaceCart) {
         if (riding instanceof MinecartFurnaceAccessor furnaceCartAccessor) {
            int fuel = furnaceCartAccessor.create$getFuel();
            int fuelBefore = fuel;
            double pushX = furnaceCart.xPush;
            double pushZ = furnaceCart.zPush;
            int i = Mth.floor(furnaceCart.getX());
            int j = Mth.floor(furnaceCart.getY());
            int k = Mth.floor(furnaceCart.getZ());
            if (furnaceCart.level().getBlockState(new BlockPos(i, j - 1, k)).is(BlockTags.RAILS)) {
               j--;
            }

            BlockPos blockpos = new BlockPos(i, j, k);
            BlockState blockstate = this.level().getBlockState(blockpos);
            if (furnaceCart.canUseRail() && blockstate.is(BlockTags.RAILS) && fuel > 1) {
               riding.setDeltaMovement(riding.getDeltaMovement().normalize().scale(1.0));
            }

            if (fuel < 5 && this.contraption != null) {
               MountedItemStorageWrapper fuelItems = this.contraption.getStorage().getFuelItems();
               if (fuelItems != null) {
                  ItemStack coal = ItemHelper.extract(fuelItems, FUEL_ITEMS, 1, false);
                  if (!coal.isEmpty()) {
                     fuel += 3600;
                  }
               }
            }

            if (fuel != fuelBefore || pushX != 0.0 || pushZ != 0.0) {
               furnaceCart.xPush = pushX;
               furnaceCart.zPush = pushZ;
               furnaceCartAccessor.create$setFuel(fuel);
            }
         }
      }
   }

   @Nullable
   public Couple<MinecartController> getCoupledCartsIfPresent() {
      UUID couplingId = this.getCouplingId();
      if (couplingId == null) {
         return null;
      } else {
         MinecartController controller = CapabilityMinecartController.getIfPresent(this.level(), couplingId);
         if (controller != null && controller.isPresent()) {
            UUID coupledCart = controller.getCoupledCart(true);
            MinecartController coupledController = CapabilityMinecartController.getIfPresent(this.level(), coupledCart);
            return coupledController != null && coupledController.isPresent() ? Couple.create(controller, coupledController) : null;
         } else {
            return null;
         }
      }
   }

   protected void attachInventoriesFromRidingCarts(Entity riding, boolean isOnCoupling, UUID couplingId) {
      if (this.contraption instanceof MountedContraption mc) {
         if (!isOnCoupling) {
            mc.addExtraInventories(riding);
         } else {
            Couple<MinecartController> coupledCarts = this.getCoupledCartsIfPresent();
            if (coupledCarts != null) {
               coupledCarts.map(MinecartController::cart).forEach(mc::addExtraInventories);
            }
         }
      }
   }

   @Nullable
   public UUID getCouplingId() {
      Optional<UUID> uuid = (Optional<UUID>)this.entityData.get(COUPLING);
      return uuid == null ? null : (uuid.isPresent() ? uuid.get() : null);
   }

   public void setCouplingId(UUID id) {
      this.entityData.set(COUPLING, Optional.ofNullable(id));
   }

   public Vec3 getVehicleAttachmentPoint(Entity entity) {
      return entity instanceof AbstractContraptionEntity ? Vec3.ZERO : new Vec3(0.0, 0.19, 0.0);
   }

   @Override
   public Vec3 getAnchorVec() {
      Vec3 anchorVec = super.getAnchorVec();
      return anchorVec.subtract(0.5, 0.0, 0.5);
   }

   @Override
   public Vec3 getPrevAnchorVec() {
      Vec3 prevAnchorVec = super.getPrevAnchorVec();
      return prevAnchorVec.subtract(0.5, 0.0, 0.5);
   }

   @Override
   protected StructureTransform makeStructureTransform() {
      BlockPos offset = BlockPos.containing(this.getAnchorVec().add(0.5, 0.5, 0.5));
      return new StructureTransform(offset, 0.0F, -this.yaw + this.getInitialYaw(), 0.0F);
   }

   @Override
   protected float getStalledAngle() {
      return this.yaw;
   }

   @Override
   protected void handleStallInformation(double x, double y, double z, float angle) {
      this.yaw = angle;
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void applyLocalTransforms(PoseStack matrixStack, float partialTicks) {
      float angleInitialYaw = this.getInitialYaw();
      float angleYaw = this.getViewYRot(partialTicks);
      float anglePitch = this.getViewXRot(partialTicks);
      matrixStack.translate(-0.5F, 0.0F, -0.5F);
      Entity ridingEntity = this.getVehicle();
      if (ridingEntity instanceof AbstractMinecart) {
         this.repositionOnCart(matrixStack, partialTicks, ridingEntity);
      } else if (ridingEntity instanceof AbstractContraptionEntity) {
         if (ridingEntity.getVehicle() instanceof AbstractMinecart) {
            this.repositionOnCart(matrixStack, partialTicks, ridingEntity.getVehicle());
         } else {
            this.repositionOnContraption(matrixStack, partialTicks, ridingEntity);
         }
      }

      ((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)TransformStack.of(matrixStack)
                        .nudge(this.getId()))
                     .center())
                  .rotateYDegrees(angleYaw))
               .rotateZDegrees(anglePitch))
            .rotateYDegrees(angleInitialYaw))
         .uncenter();
   }

   @OnlyIn(Dist.CLIENT)
   private void repositionOnContraption(PoseStack matrixStack, float partialTicks, Entity ridingEntity) {
      Vec3 pos = this.getContraptionOffset(partialTicks, ridingEntity);
      matrixStack.translate(pos.x, pos.y, pos.z);
   }

   @OnlyIn(Dist.CLIENT)
   private void repositionOnCart(PoseStack matrixStack, float partialTicks, Entity ridingEntity) {
      Vec3 cartPos = this.getCartOffset(partialTicks, ridingEntity);
      if (cartPos != Vec3.ZERO) {
         matrixStack.translate(cartPos.x, cartPos.y, cartPos.z);
      }
   }

   @OnlyIn(Dist.CLIENT)
   private Vec3 getContraptionOffset(float partialTicks, Entity ridingEntity) {
      AbstractContraptionEntity parent = (AbstractContraptionEntity)ridingEntity;
      Vec3 passengerPosition = parent.getPassengerPosition(this, partialTicks);
      if (passengerPosition == null) {
         return Vec3.ZERO;
      } else {
         double x = passengerPosition.x - Mth.lerp((double)partialTicks, this.xOld, this.getX());
         double y = passengerPosition.y - Mth.lerp((double)partialTicks, this.yOld, this.getY());
         double z = passengerPosition.z - Mth.lerp((double)partialTicks, this.zOld, this.getZ());
         return new Vec3(x, y, z);
      }
   }

   @OnlyIn(Dist.CLIENT)
   private Vec3 getCartOffset(float partialTicks, Entity ridingEntity) {
      AbstractMinecart cart = (AbstractMinecart)ridingEntity;
      double cartX = Mth.lerp((double)partialTicks, cart.xOld, cart.getX());
      double cartY = Mth.lerp((double)partialTicks, cart.yOld, cart.getY());
      double cartZ = Mth.lerp((double)partialTicks, cart.zOld, cart.getZ());
      Vec3 cartPos = cart.getPos(cartX, cartY, cartZ);
      if (cartPos != null) {
         Vec3 cartPosFront = cart.getPosOffs(cartX, cartY, cartZ, 0.3F);
         Vec3 cartPosBack = cart.getPosOffs(cartX, cartY, cartZ, -0.3F);
         if (cartPosFront == null) {
            cartPosFront = cartPos;
         }

         if (cartPosBack == null) {
            cartPosBack = cartPos;
         }

         cartX = cartPos.x - cartX;
         cartY = (cartPosFront.y + cartPosBack.y) / 2.0 - cartY;
         cartZ = cartPos.z - cartZ;
         return new Vec3(cartX, cartY, cartZ);
      } else {
         return Vec3.ZERO;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static void handleRelocationPacket(ContraptionRelocationPacket packet) {
      if (Minecraft.getInstance().level.getEntity(packet.entityId()) instanceof OrientedContraptionEntity oce) {
         oce.nonDamageTicks = 10;
      }
   }
}
