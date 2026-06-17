package dev.simulated_team.simulated.content.entities.launched_plunger;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.entity.EntitySubLevelUtil;
import dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis;
import dev.ryanhcode.sable.api.physics.constraint.FreeConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintHandle;
import dev.ryanhcode.sable.api.physics.force.ForceTotal;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.simulated_team.simulated.index.SimEntityDataSerializers;
import dev.simulated_team.simulated.index.SimItems;
import dev.simulated_team.simulated.index.SimSoundEvents;
import dev.simulated_team.simulated.mixin_interface.PlayerLaunchedPlungerExtension;
import dev.simulated_team.simulated.service.SimConfigService;
import java.util.Optional;
import java.util.UUID;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class LaunchedPlungerEntity extends ThrowableProjectile {
   public static final EntityDataAccessor<Optional<UUID>> OTHER_PLUNGER = SynchedEntityData.defineId(
      LaunchedPlungerEntity.class, EntityDataSerializers.OPTIONAL_UUID
   );
   public static final EntityDataAccessor<Integer> OTHER_PLUNGER_ID = SynchedEntityData.defineId(LaunchedPlungerEntity.class, EntityDataSerializers.INT);
   public static final EntityDataAccessor<Direction> PLUNGED_DIRECTION = SynchedEntityData.defineId(
      LaunchedPlungerEntity.class, EntityDataSerializers.DIRECTION
   );
   public static final EntityDataAccessor<BlockPos> PLUNGED_BLOCK_POS = SynchedEntityData.defineId(LaunchedPlungerEntity.class, EntityDataSerializers.BLOCK_POS);
   public static final EntityDataAccessor<Boolean> IS_PLUNGED = SynchedEntityData.defineId(LaunchedPlungerEntity.class, EntityDataSerializers.BOOLEAN);
   public static final EntityDataAccessor<Boolean> IS_FIRST = SynchedEntityData.defineId(LaunchedPlungerEntity.class, EntityDataSerializers.BOOLEAN);
   public static final EntityDataAccessor<Vec3> TARGET_POS = SynchedEntityData.defineId(LaunchedPlungerEntity.class, SimEntityDataSerializers.VEC3);
   private static final double CLIENT_VELOCITY_SMOOTHING_ALPHA = 0.7;
   private final float animationOffset = (float)(Math.random() * 50.0);
   private final ForceTotal forceTotal = new ForceTotal();
   private final ForceTotal otherForceTotal = new ForceTotal();
   private Vec3 previousClientPosition = Vec3.ZERO;
   private Vec3 previousClientSmoothedVelocity = Vec3.ZERO;
   private Vec3 clientSmoothedVelocity = Vec3.ZERO;
   private Vec3 prevTargetPos = Vec3.ZERO;
   private LaunchedPlungerEntity cachedOtherPlunger;
   private int plungedTime = 0;
   private boolean addedToPlungerHandler = false;
   private PhysicsConstraintHandle constraint;
   private static final ProjectileDeflection DEFLECTION = (projectile, entity, randomSource) -> {
      Vec3 target = Vec3.ZERO;
      if (entity instanceof LaunchedPlungerEntity launchedPlungerEntity) {
         target = launchedPlungerEntity.getData(TARGET_POS);
         target = target.subtract(entity.position());
      }

      projectile.setDeltaMovement(projectile.getDeltaMovement().scale(0.8).add(target.normalize().scale(0.5)));
   };

   public LaunchedPlungerEntity(EntityType<? extends LaunchedPlungerEntity> entityType, Level level) {
      super(entityType, level);
   }

   public static LaunchedPlungerEntity create(EntityType<? extends LaunchedPlungerEntity> entityType, Level world) {
      return new LaunchedPlungerEntity(entityType, world);
   }

   protected void defineSynchedData(Builder builder) {
      builder.define(OTHER_PLUNGER_ID, -1);
      builder.define(TARGET_POS, Vec3.ZERO);
      builder.define(OTHER_PLUNGER, Optional.empty());
      builder.define(PLUNGED_BLOCK_POS, BlockPos.ZERO);
      builder.define(IS_FIRST, Boolean.FALSE);
      builder.define(PLUNGED_DIRECTION, Direction.UP);
      builder.define(IS_PLUNGED, false);
   }

   public void tick() {
      Level level = this.level();
      if (!level.isClientSide && !this.addedToPlungerHandler) {
         LaunchedPlungerServerHandler.addLaunchedPlunger(level, this);
         this.addedToPlungerHandler = true;
      }

      super.tick();
      Entity owner = this.getOwner();
      if (owner == null) {
         this.discard();
      } else {
         LaunchedPlungerEntity other = this.getOther();
         if (!level.isClientSide && other != null) {
            this.setData(TARGET_POS, other.position());
            double distance = Math.sqrt(Sable.HELPER.distanceSquaredWithSubLevels(level, this.position(), other.position()));
            if (distance > (double)((Integer)SimConfigService.INSTANCE.server().equipment.maxPlungerLauncherRange.get()).intValue()) {
               this.discard();
            }
         } else {
            this.setData(TARGET_POS, Vec3.ZERO);
            label69:
            if (!level.isClientSide) {
               label66:
               if (!((Optional)this.getEntityData().get(OTHER_PLUNGER)).isPresent()) {
                  if (((Optional)this.getEntityData().get(OTHER_PLUNGER)).isEmpty()
                     && owner instanceof Player player
                     && !player.isHolding((Item)SimItems.PLUNGER_LAUNCHER.get())) {
                     break label66;
                  }

                  if (((Optional)this.getEntityData().get(OTHER_PLUNGER)).isEmpty()
                     && owner instanceof Player player
                     && player.isHolding((Item)SimItems.PLUNGER_LAUNCHER.get())) {
                     double distance = Math.sqrt(Sable.HELPER.distanceSquaredWithSubLevels(this.level(), this.position(), player.position()));
                     if (distance > (double)((Integer)SimConfigService.INSTANCE.server().equipment.maxPlungerLauncherRange.get()).intValue()) {
                        this.discard();
                     }
                  }
                  break label69;
               }

               this.discard();
            } else if (owner instanceof Player playerx) {
               PlayerLaunchedPlungerExtension duck = (PlayerLaunchedPlungerExtension)playerx;
               duck.simulated$setLaunchedPlunger(this);
            }
         }

         if (this.<Boolean>getData(IS_PLUNGED)) {
            this.setDeltaMovement(Vec3.ZERO);
            this.lookAt(Anchor.FEET, this.position().add(Vec3.atLowerCornerOf(this.<Direction>getData(PLUNGED_DIRECTION).getNormal()).scale(0.05F)));
            if (this.firstTick) {
               this.plungedTime = 20;
            } else if (this.plungedTime < 20) {
               this.plungedTime++;
            }

            BlockPos plungedPos = this.getData(PLUNGED_BLOCK_POS);
            if (level.isLoaded(plungedPos) && level.getBlockState(plungedPos).isAir()) {
               SubLevel containing = Sable.HELPER.getContaining(this);
               if (containing != null) {
                  EntitySubLevelUtil.kickEntity(containing, this);
               }

               this.resetPlunged();
               level.playSound(null, this.getX(), this.getY(), this.getZ(), SimSoundEvents.PLUNGER_RELEASE.event(), SoundSource.PLAYERS, 1.0F, 1.0F);
            }
         }

         if (level.isClientSide) {
            this.tickSmoothing();
         }
      }
   }

   private void tickSmoothing() {
      Vec3 movement = this.position().subtract(this.previousClientPosition);
      this.previousClientPosition = this.position();
      this.previousClientSmoothedVelocity = this.clientSmoothedVelocity;
      this.clientSmoothedVelocity = this.clientSmoothedVelocity.add(movement.subtract(this.clientSmoothedVelocity).scale(0.7));
   }

   public Vec3 getAttachmentPos(float partialTick) {
      Vec3 pos = this.getPosition(partialTick);
      if (this.isPlunged()) {
         pos = pos.add(Vec3.atLowerCornerOf(this.<Direction>getData(PLUNGED_DIRECTION).getNormal()).scale(0.6));
      }

      return pos;
   }

   public Vec3 getAttachmentPos() {
      Vec3 pos = this.position();
      if (this.isPlunged()) {
         pos = pos.add(Vec3.atLowerCornerOf(this.<Direction>getData(PLUNGED_DIRECTION).getNormal()).scale(0.6));
      }

      return pos;
   }

   public void physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double timeStep) {
      LaunchedPlungerEntity otherPlunger = this.getOther();
      if (otherPlunger != null && this.isPlunged() && otherPlunger.isPlunged()) {
         ServerSubLevel otherSubLevel = (ServerSubLevel)Sable.HELPER.getContaining(otherPlunger);
         if (subLevel == otherSubLevel) {
            return;
         }

         RigidBodyHandle otherHandle = otherSubLevel != null ? RigidBodyHandle.of(otherSubLevel) : null;
         if (otherSubLevel == null || this.getId() > otherPlunger.getId()) {
            Vec3 attachmentPos = subLevel.logicalPose().transformPosition(this.getAttachmentPos());
            Vec3 otherAttachmentPos = otherPlunger.getAttachmentPos();
            if (otherSubLevel != null) {
               otherAttachmentPos = otherSubLevel.logicalPose().transformPosition(otherAttachmentPos);
            }

            Vector3d force = JOMLConversion.toJOML(otherAttachmentPos.subtract(attachmentPos));
            double maxLength = 12.0;
            if (force.lengthSquared() > 144.0) {
               force.normalize(12.0);
            }

            force.mul(40.0);
            if (force.lengthSquared() < 1.0E-6) {
               return;
            }

            Vector3dc localAttachmentPos1 = JOMLConversion.toJOML(this.getAttachmentPos());
            Vector3d velocity = Sable.HELPER.getVelocity(this.level(), subLevel, localAttachmentPos1, new Vector3d());
            if (otherHandle != null) {
               Vector3d localAttachmentPos2 = JOMLConversion.toJOML(otherPlunger.getAttachmentPos());
               velocity.add(Sable.HELPER.getVelocity(this.level(), otherSubLevel, localAttachmentPos2));
            }

            Vector3d localForce1 = subLevel.logicalPose().transformNormalInverse(force, new Vector3d());
            double inverseNormalMass = subLevel.getMassTracker().getInverseNormalMass(localAttachmentPos1, localForce1);
            if (otherHandle != null) {
               Vector3d localAttachmentPos2 = JOMLConversion.toJOML(otherPlunger.getAttachmentPos());
               Vector3d localForce2 = otherSubLevel.logicalPose().transformNormalInverse(force, new Vector3d());
               inverseNormalMass = Math.max(inverseNormalMass, otherSubLevel.getMassTracker().getInverseNormalMass(localAttachmentPos2, localForce2));
            }

            double scaleFactor = 1.0 / inverseNormalMass * 0.07 * timeStep;
            this.forceTotal.applyImpulseAtPoint(subLevel, localAttachmentPos1, localForce1.mul(scaleFactor));
            handle.applyForcesAndReset(this.forceTotal);
            if (otherHandle != null) {
               Vector3d localForce2 = otherSubLevel.logicalPose().transformNormalInverse(force, new Vector3d()).mul(-scaleFactor);
               this.otherForceTotal.applyImpulseAtPoint(otherSubLevel, JOMLConversion.toJOML(otherPlunger.getAttachmentPos()), localForce2);
               otherHandle.applyForcesAndReset(this.otherForceTotal);
            }
         }

         if (this.constraint == null && otherPlunger.constraint == null) {
            FreeConstraintConfiguration constraintConfig = new FreeConstraintConfiguration(
               JOMLConversion.toJOML(this.getAttachmentPos()), JOMLConversion.toJOML(otherPlunger.getAttachmentPos()), new Quaterniond()
            );
            SubLevelPhysicsSystem physicsSystem = SubLevelPhysicsSystem.get(subLevel.getLevel());
            this.constraint = physicsSystem.getPipeline().addConstraint(subLevel, (ServerSubLevel)Sable.HELPER.getContaining(otherPlunger), constraintConfig);

            for (ConstraintJointAxis axis : ConstraintJointAxis.LINEAR) {
               this.constraint.setMotor(axis, 0.0, 0.0, 1.0, false, 50.0);
            }

            for (ConstraintJointAxis axis : ConstraintJointAxis.ANGULAR) {
               this.constraint.setMotor(axis, 0.0, 0.0, 0.25, false, 50.0);
            }
         }
      }
   }

   public Vec3 getLightProbePosition(float partialTicks) {
      return this.getAttachmentPos(partialTicks);
   }

   protected AABB makeBoundingBox() {
      AABB bb = this.getDimensions(this.getPose()).makeBoundingBox(this.position());
      return bb.move(0.0, -bb.getYsize() / 2.0, 0.0);
   }

   private void removeConstraint() {
      if (this.constraint != null) {
         this.constraint.remove();
      }
   }

   public int getPlungedTime() {
      return this.plungedTime;
   }

   public float getAnimationOffset() {
      return this.animationOffset;
   }

   protected boolean canHitEntity(Entity entity) {
      return false;
   }

   protected void onHitBlock(@NotNull BlockHitResult blockHitResult) {
      super.onHitBlock(blockHitResult);
      this.removeConstraint();
      this.noPhysics = true;
      SubLevel subLevel = Sable.HELPER.getContaining(this.level(), blockHitResult.getLocation());
      Vec3 selfPos = this.position();
      Vec3 diff = blockHitResult.getLocation().subtract(this.position());
      if (subLevel != null) {
         selfPos = subLevel.logicalPose().transformPositionInverse(selfPos);
         diff = blockHitResult.getLocation().subtract(selfPos);
      }

      this.setDeltaMovement(diff);
      Vec3 nudge = diff.normalize().scale(0.05F);
      this.setPosRaw(selfPos.x() - nudge.x(), selfPos.y() - nudge.y(), selfPos.z() - nudge.z());
      this.setData(IS_PLUNGED, true);
      this.setData(PLUNGED_DIRECTION, blockHitResult.getDirection());
      this.setData(PLUNGED_BLOCK_POS, blockHitResult.getBlockPos());
      this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SimSoundEvents.PLUNGER_PLACE.event(), SoundSource.PLAYERS, 1.0F, 1.0F);
   }

   public void remove(RemovalReason removalReason) {
      this.removeConstraint();
      if (!this.level().isClientSide) {
         this.playSound(SimSoundEvents.PLUNGER_RELEASE.event(), 1.0F, 0.9F + 0.2F * this.level().random.nextFloat());
         LaunchedPlungerServerHandler.removeLaunchedPlunger(this.level(), this);
      }

      super.remove(removalReason);
      LaunchedPlungerEntity other = this.getOther();
      if (other != null && !other.isRemoved()) {
         other.discard();
         this.setOther(null);
      }
   }

   protected void addAdditionalSaveData(CompoundTag compoundTag) {
      Optional<UUID> other = this.getData(OTHER_PLUNGER);
      other.ifPresent(value -> compoundTag.putUUID("OtherPlunger", value));
      compoundTag.put("PlungedBlockPos", NbtUtils.writeBlockPos(this.getData(PLUNGED_BLOCK_POS)));
      compoundTag.put("TargetPos", VecHelper.writeNBT(this.getData(TARGET_POS)));
      NBTHelper.writeEnum(compoundTag, "PlungedDir", (Direction)this.getData(PLUNGED_DIRECTION));
      compoundTag.putBoolean("IsPlunged", this.<Boolean>getData(IS_PLUNGED));
      compoundTag.putBoolean("IsFirst", this.<Boolean>getData(IS_FIRST));
      super.addAdditionalSaveData(compoundTag);
   }

   protected void readAdditionalSaveData(CompoundTag compoundTag) {
      this.setData(IS_PLUNGED, compoundTag.getBoolean("IsPlunged"));
      this.setData(PLUNGED_DIRECTION, (Direction)NBTHelper.readEnum(compoundTag, "PlungedDir", Direction.class));
      this.setData(PLUNGED_BLOCK_POS, (BlockPos)NbtUtils.readBlockPos(compoundTag, "PlungedBlockPos").get());
      this.setData(TARGET_POS, VecHelper.readNBT((ListTag)compoundTag.get("TargetPos")));
      this.setData(IS_FIRST, compoundTag.getBoolean("IsFirst"));
      if (compoundTag.contains("OtherPlunger")) {
         this.setData(OTHER_PLUNGER, Optional.of(compoundTag.getUUID("OtherPlunger")));
      }

      super.readAdditionalSaveData(compoundTag);
   }

   public void resetPlunged() {
      this.setData(IS_PLUNGED, false);
      this.setData(PLUNGED_DIRECTION, Direction.UP);
      this.setData(PLUNGED_BLOCK_POS, BlockPos.ZERO);
      this.noPhysics = false;
   }

   public ProjectileDeflection deflection(Projectile projectile) {
      this.discard();
      return DEFLECTION;
   }

   public boolean skipAttackInteraction(Entity entity) {
      if (entity instanceof Player) {
         this.discard();
      }

      return true;
   }

   public boolean isPickable() {
      return true;
   }

   public float getPickRadius() {
      return 0.0F;
   }

   @Nullable
   public ItemStack getPickResult() {
      return null;
   }

   public boolean isShiftKeyDown() {
      return true;
   }

   public LaunchedPlungerEntity getOther() {
      if (this.cachedOtherPlunger != null && this.cachedOtherPlunger.isAlive()) {
         return this.cachedOtherPlunger;
      } else {
         if (!this.level().isClientSide) {
            Optional<UUID> otherID = this.getData(OTHER_PLUNGER);
            if (otherID.isPresent()) {
               Entity entity = ((ServerLevel)this.level()).getEntity(otherID.get());
               if (entity instanceof LaunchedPlungerEntity) {
                  this.setData(OTHER_PLUNGER_ID, entity.getId());
                  this.cachedOtherPlunger = (LaunchedPlungerEntity)entity;
               } else {
                  this.setData(OTHER_PLUNGER_ID, -1);
               }
            }
         } else {
            int otherID = this.<Integer>getData(OTHER_PLUNGER_ID);
            if (otherID != -1) {
               Entity entity = this.level().getEntity(otherID);
               if (entity instanceof LaunchedPlungerEntity) {
                  this.cachedOtherPlunger = (LaunchedPlungerEntity)entity;
               }
            }
         }

         return this.cachedOtherPlunger;
      }
   }

   public void setOther(LaunchedPlungerEntity other) {
      this.cachedOtherPlunger = other;
      this.setData(OTHER_PLUNGER, Optional.ofNullable(other == null ? null : other.getUUID()));
      this.setData(OTHER_PLUNGER_ID, other == null ? -1 : other.getId());
   }

   @NotNull
   public Vec3 getTarget() {
      return this.getClientTarget(1.0F);
   }

   @NotNull
   public Vec3 getClientTarget(float pt) {
      if (this.level().isClientSide()) {
         if (this.firstTick) {
            this.prevTargetPos = this.getData(TARGET_POS);
         }

         return this.prevTargetPos = VecHelper.lerp(pt, this.prevTargetPos, this.getData(TARGET_POS));
      } else {
         return this.getData(TARGET_POS);
      }
   }

   public void load(CompoundTag compound) {
      super.load(compound);
      this.setOwner(null);
      this.ownerUUID = null;
   }

   public boolean isPlunged() {
      return this.<Boolean>getData(IS_PLUNGED);
   }

   public <T> T getData(EntityDataAccessor<T> accessor) {
      return (T)this.entityData.get(accessor);
   }

   public <T> void setData(EntityDataAccessor<T> accessor, T value) {
      this.entityData.set(accessor, value);
   }

   public Vec3 getClientSmoothedVelocity(float partialTicks) {
      return this.previousClientSmoothedVelocity.lerp(this.clientSmoothedVelocity, (double)partialTicks);
   }
}
