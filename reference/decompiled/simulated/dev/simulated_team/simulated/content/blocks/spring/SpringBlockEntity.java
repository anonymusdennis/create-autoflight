package dev.simulated_team.simulated.content.blocks.spring;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.force.ForceTotal;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.schematic.SubLevelSchematicSerializationContext;
import dev.ryanhcode.sable.api.schematic.SubLevelSchematicSerializationContext.SchematicMapping;
import dev.ryanhcode.sable.api.schematic.SubLevelSchematicSerializationContext.Type;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.simulated_team.simulated.util.SimLevelUtil;
import java.util.List;
import java.util.UUID;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class SpringBlockEntity extends SmartBlockEntity implements BlockEntitySubLevelActor {
   private static final Vector3d frictionForce = new Vector3d();
   private static final Vector3d frictionTorque = new Vector3d();
   private static final Vector3d localLinearVelocity = new Vector3d();
   private static final Vector3d localAngularVelocity = new Vector3d();
   private static final Vector3d expectedVelocity = new Vector3d();
   private static final Vector3d localDampingPointForce = new Vector3d();
   private static final double TIME_TO_SNAP = 0.75;
   protected LerpedFloat renderLength = LerpedFloat.linear();
   protected double desiredLength;
   protected boolean isController;
   protected boolean assembling;
   protected BlockPos partnerPos;
   @Nullable
   private UUID partnerSubLevel;
   private float ticksWithoutPartner = 0.0F;
   private ForceTotal forceTotal;
   private ForceTotal partnerForceTotal;
   private double snappingTime;

   public SpringBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
      this.renderLength.chase(0.0, 0.2, Chaser.EXP);
   }

   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
   }

   public Vector3d getCenter() {
      BlockState state = this.getBlockState();
      Direction facing = (Direction)state.getValue(SpringBlock.FACING);
      Vec3i facingVec = facing.getNormal();
      double scale = 0.25;
      return JOMLConversion.atCenterOf(this.worldPosition)
         .sub((double)facingVec.getX() * scale, (double)facingVec.getY() * scale, (double)facingVec.getZ() * scale);
   }

   @Nullable
   public String tryChangeLengthOrError(Level level, double delta) {
      if (delta > 0.0 && this.desiredLength >= 9.0) {
         return "max_length";
      } else if (delta < 0.0 && this.desiredLength <= 1.0) {
         return "min_length";
      } else {
         double newDesiredLength = Math.clamp(this.desiredLength + delta, 1.0, 9.0);
         newDesiredLength = (double)Math.round(newDesiredLength / 0.25) * 0.25;
         double currentLength = Sable.HELPER.distanceSquaredWithSubLevels(level, this.worldPosition.getCenter(), this.partnerPos.getCenter()) + 1.0;
         if (delta < 0.0 && currentLength > newDesiredLength * newDesiredLength * 4.0) {
            return "too_stretched";
         } else if (delta > 0.0 && currentLength < newDesiredLength * newDesiredLength / 4.0) {
            return "too_compressed";
         } else {
            this.desiredLength = newDesiredLength;
            this.setChanged();
            this.sendData();
            if (level.getBlockEntity(this.partnerPos) instanceof SpringBlockEntity partnerBE) {
               partnerBE.desiredLength = this.desiredLength;
               partnerBE.setChanged();
               partnerBE.sendData();
            }

            return null;
         }
      }
   }

   public double getRenderLength(float pt) {
      return (double)this.renderLength.getValue(pt);
   }

   public void tick() {
      super.tick();
      if (this.level.isClientSide) {
         this.renderLength.updateChaseTarget((float)this.desiredLength);
         this.renderLength.tickChaser();
      } else {
         if (this.snappingTime > 0.75) {
            this.level.destroyBlock(this.getBlockPos(), true);
         }

         if (this.partnerPos != null) {
            if (SimLevelUtil.isAreaActuallyLoaded(this.getLevel(), this.partnerPos, 1)) {
               if (this.getPairedSpring() == null && this.ticksWithoutPartner++ > 20.0F) {
                  this.level.destroyBlock(this.getBlockPos(), true);
               } else {
                  this.ticksWithoutPartner = 0.0F;
               }
            }
         } else {
            this.level.destroyBlock(this.getBlockPos(), true);
         }
      }
   }

   public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double timeStep) {
      SpringBlockEntity partner = this.getPairedSpring();
      if (this.partnerPos != null
         && SimLevelUtil.isAreaActuallyLoaded(this.getLevel(), this.partnerPos, 1)
         && partner != null
         && this.ticksWithoutPartner == 0.0F) {
         ServerSubLevelContainer container = SubLevelContainer.getContainer(subLevel.getLevel());
         SubLevelPhysicsSystem system = container.physicsSystem();
         system.updatePose(subLevel);
         ServerSubLevel partnerSubLevel = this.partnerSubLevel != null ? (ServerSubLevel)container.getSubLevel(this.partnerSubLevel) : null;
         if (this.partnerSubLevel == null || partnerSubLevel != null) {
            if (partnerSubLevel == null || this.isController) {
               if (partnerSubLevel != subLevel) {
                  if (partnerSubLevel != null) {
                     system.updatePose(partnerSubLevel);
                  }

                  BlockState state = this.getBlockState();
                  SpringBlock.Size size = (SpringBlock.Size)state.getValue(SpringBlock.SIZE);
                  Vector3dc center = this.getCenter();
                  Vector3dc partnerCenter = partner.getCenter();
                  Vector3d velo1 = Sable.HELPER.getVelocity(this.level, center, new Vector3d());
                  Vector3d velo2 = Sable.HELPER.getVelocity(this.level, partnerCenter, new Vector3d());
                  Vector3d positionA = subLevel.logicalPose().transformPosition(center, new Vector3d());
                  Vector3d positionB = partnerSubLevel != null
                     ? partnerSubLevel.logicalPose().transformPosition(JOMLConversion.atCenterOf(this.partnerPos))
                     : JOMLConversion.atCenterOf(this.partnerPos);
                  Vector3d relativeVelo = velo1.sub(velo2);
                  Vector3d dampingPointForce = new Vector3d(relativeVelo);
                  dampingPointForce.mul(-4.5);
                  double desiredLength = (this.isController ? this.desiredLength : partner.desiredLength) - 0.75;
                  if (positionA.distanceSquared(positionB) > Mth.square(this.getSnappingDistance())) {
                     this.snappingTime += timeStep;
                  } else {
                     this.snappingTime = 0.0;
                  }

                  Vector3d globalNormalA = JOMLConversion.atLowerCornerOf(((Direction)state.getValue(SpringBlock.FACING)).getNormal());
                  Vector3d globalNormalB = JOMLConversion.atLowerCornerOf(((Direction)partner.getBlockState().getValue(SpringBlock.FACING)).getNormal());
                  subLevel.logicalPose().transformNormal(globalNormalA);
                  if (partnerSubLevel != null) {
                     partnerSubLevel.logicalPose().transformNormal(globalNormalB);
                  }

                  Vector3d torque = globalNormalA.cross(globalNormalB.negate(), new Vector3d()).mul(20.0).mul(timeStep);
                  Vector3d mediumNormal = globalNormalA.lerp(globalNormalB, 0.5);
                  Vector3dc middle = new Vector3d(positionA.x, positionA.y, positionA.z).lerp(positionB, 0.5);
                  Vector3d desireA = middle.fma(-desiredLength / 2.0, mediumNormal, new Vector3d());
                  Vector3d alignmentForce = desireA.sub(positionA.x, positionA.y, positionA.z);
                  Vector3d hookesPointForce = alignmentForce.mul(145.0);
                  Vector3d angVelo1 = new Vector3d();
                  Vector3d angVelo2 = new Vector3d();
                  handle.getAngularVelocity(angVelo1);
                  if (partnerSubLevel != null) {
                     RigidBodyHandle otherHandle = RigidBodyHandle.of(partnerSubLevel);
                     otherHandle.getAngularVelocity(angVelo2);
                  }

                  Vector3d relativeAngVelo = angVelo1.sub(angVelo2);
                  Vector3d dampingTorque = new Vector3d();
                  if (mediumNormal.lengthSquared() > 0.0) {
                     mediumNormal.normalize();
                     double dot = mediumNormal.dot(relativeAngVelo);
                     relativeAngVelo.set(mediumNormal).mul(dot);
                     dampingTorque.fma(-2.0, relativeAngVelo);
                  }
                  double sizeScale = switch (size) {
                     case LARGE -> 8.0;
                     case MEDIUM -> 1.0;
                     case SMALL -> 0.5;
                  };
                  hookesPointForce.mul(sizeScale);
                  torque.mul(sizeScale);
                  dampingTorque.mul(sizeScale);
                  dampingPointForce.mul(sizeScale);
                  if (this.forceTotal == null || this.partnerForceTotal == null) {
                     this.forceTotal = new ForceTotal();
                     this.partnerForceTotal = new ForceTotal();
                  }

                  this.applyLocalDamping(subLevel, handle, this.forceTotal, center, dampingPointForce, dampingTorque, timeStep);
                  this.forceTotal
                     .applyImpulseAtPoint(subLevel, center, subLevel.logicalPose().transformNormalInverse(new Vector3d(hookesPointForce)).mul(timeStep));
                  this.forceTotal.applyLinearAndAngularImpulse(JOMLConversion.ZERO, subLevel.logicalPose().transformNormalInverse(torque, new Vector3d()));
                  handle.applyForcesAndReset(this.forceTotal);
                  if (partnerSubLevel != null) {
                     RigidBodyHandle partnerHandle = RigidBodyHandle.of(partnerSubLevel);
                     this.applyLocalDamping(
                        partnerSubLevel, partnerHandle, this.partnerForceTotal, partnerCenter, dampingPointForce.negate(), dampingTorque.negate(), timeStep
                     );
                     this.partnerForceTotal
                        .applyImpulseAtPoint(
                           partnerSubLevel, partnerCenter, partnerSubLevel.logicalPose().transformNormalInverse(hookesPointForce).mul(-timeStep)
                        );
                     this.partnerForceTotal
                        .applyLinearAndAngularImpulse(JOMLConversion.ZERO, partnerSubLevel.logicalPose().transformNormalInverse(torque.negate()));
                     partnerHandle.applyForcesAndReset(this.partnerForceTotal);
                  }
               }
            }
         }
      }
   }

   private void applyLocalDamping(
      ServerSubLevel subLevel,
      RigidBodyHandle handle,
      ForceTotal forceTotal,
      Vector3dc worldSpringPos,
      Vector3dc dampingPointForce,
      Vector3dc dampingTorque,
      double timeStep
   ) {
      Pose3d pose = subLevel.logicalPose();
      handle.getAngularVelocity(localAngularVelocity);
      handle.getLinearVelocity(localLinearVelocity);
      pose.orientation().transformInverse(localAngularVelocity);
      pose.orientation().transformInverse(localLinearVelocity);
      Vector3dc centerOfMass = subLevel.getMassTracker().getCenterOfMass();
      pose.orientation().transformInverse(dampingPointForce, localDampingPointForce);
      Vector3d angularDamping = new Vector3d();
      angularDamping.add(dampingTorque);
      pose.orientation().transformInverse(angularDamping);
      angularDamping.add(worldSpringPos.sub(centerOfMass, new Vector3d()).cross(localDampingPointForce));
      Vector3d linearDamping = new Vector3d();
      linearDamping.add(localDampingPointForce);
      frictionForce.set(linearDamping);
      frictionTorque.set(angularDamping);
      expectedVelocity.set(frictionForce);
      expectedVelocity.mul(subLevel.getMassTracker().getInverseMass());
      expectedVelocity.mul(timeStep);
      double forceScale = this.getClampingFactor(localLinearVelocity, expectedVelocity);
      expectedVelocity.set(frictionTorque);
      subLevel.getMassTracker().getInverseInertiaTensor().transform(expectedVelocity);
      expectedVelocity.mul(timeStep);
      double torqueScale = this.getClampingFactor(localAngularVelocity, expectedVelocity);
      frictionForce.mul(forceScale * timeStep);
      frictionTorque.mul(torqueScale * timeStep);
      forceTotal.applyLinearAndAngularImpulse(frictionForce, frictionTorque);
   }

   private double getClampingFactor(Vector3dc currentVelocity, Vector3dc expectedVelocityChange) {
      double k = -currentVelocity.dot(expectedVelocityChange);
      double v = currentVelocity.lengthSquared();
      if (k < 0.0) {
         return 0.0;
      } else if (10.0 * k < v) {
         return 1.0;
      } else {
         return v < 1.0E-10 ? v / (k + 1.0E-10) : v * (1.0 - Math.exp(-k / v)) / k;
      }
   }

   @Nullable
   public Iterable<SubLevel> sable$getConnectionDependencies() {
      if (this.partnerSubLevel != null) {
         SubLevel subLevel = SubLevelContainer.getContainer(this.level).getSubLevel(this.partnerSubLevel);
         if (subLevel != null) {
            return List.of(subLevel);
         }
      }

      return List.of();
   }

   public void remove() {
      if (!this.level.isClientSide && this.partnerPos != null && !this.assembling) {
         this.level.destroyBlock(this.partnerPos, false);
      }

      this.partnerPos = null;
   }

   protected void write(CompoundTag tag, Provider registries, boolean clientPacket) {
      super.write(tag, registries, clientPacket);
      tag.putBoolean("Controller", this.isController);
      tag.putDouble("DesiredLength", this.desiredLength);
      if (this.partnerPos != null) {
         SubLevelSchematicSerializationContext schematicContext = SubLevelSchematicSerializationContext.getCurrentContext();
         if (schematicContext != null && schematicContext.getType() != Type.PLACE) {
            BlockPos partnerPos = this.partnerPos;
            UUID id = this.partnerSubLevel;
            if (id != null) {
               SchematicMapping mapping = schematicContext.getMapping(id);
               if (mapping != null) {
                  id = mapping.newUUID();
                  partnerPos = (BlockPos)mapping.transform().apply(partnerPos);
               } else {
                  id = null;
                  partnerPos = null;
               }
            } else if (schematicContext.getBoundingBox().contains(partnerPos.getX(), partnerPos.getY(), partnerPos.getZ())) {
               partnerPos = (BlockPos)schematicContext.getPlaceTransform().apply(partnerPos);
            } else {
               partnerPos = null;
            }

            if (partnerPos != null) {
               tag.putLong("Goal", partnerPos.asLong());
            }

            if (id != null) {
               tag.putUUID("GoalSubLevel", id);
            }
         } else {
            if (this.partnerSubLevel != null) {
               tag.putUUID("GoalSubLevel", this.partnerSubLevel);
            }

            BlockPos partnerPosx = this.partnerPos;
            if (partnerPosx != null) {
               if (schematicContext != null && this.partnerSubLevel == null) {
                  partnerPosx = (BlockPos)schematicContext.getSetupTransform().apply(partnerPosx);
               }

               tag.putLong("Goal", partnerPosx.asLong());
            }
         }
      }
   }

   protected void read(CompoundTag tag, Provider registries, boolean clientPacket) {
      super.read(tag, registries, clientPacket);
      this.isController = tag.getBoolean("Controller");
      this.desiredLength = tag.getDouble("DesiredLength");
      if (this.renderLength.getValue() == 0.0F) {
         this.renderLength.setValue(this.desiredLength);
      }

      SubLevelSchematicSerializationContext schematicContext = SubLevelSchematicSerializationContext.getCurrentContext();
      boolean isPlacingFromSchematic = schematicContext != null && schematicContext.getType() == Type.PLACE;
      SchematicMapping mapping = null;
      if (tag.hasUUID("GoalSubLevel")) {
         UUID subLevelID = tag.getUUID("GoalSubLevel");
         if (isPlacingFromSchematic) {
            mapping = schematicContext.getMapping(subLevelID);
            if (mapping == null) {
               this.partnerSubLevel = null;
               this.partnerPos = null;
               return;
            }

            subLevelID = mapping.newUUID();
         }

         this.partnerSubLevel = subLevelID;
      }

      if (tag.contains("Goal")) {
         BlockPos blockPos = BlockPos.of(tag.getLong("Goal"));
         if (isPlacingFromSchematic) {
            if (mapping != null) {
               blockPos = (BlockPos)mapping.transform().apply(blockPos);
            } else {
               blockPos = (BlockPos)schematicContext.getPlaceTransform().apply(blockPos);
            }
         }

         this.partnerPos = blockPos;
      }
   }

   public void setPartnerPos(BlockPos pos, UUID subLevel) {
      this.partnerPos = pos;
      this.partnerSubLevel = subLevel;
      this.sendData();
   }

   public boolean isController() {
      return this.isController;
   }

   public void setController(boolean b) {
      this.isController = b;
   }

   public double getSnappingDistance() {
      return this.desiredLength * 4.0 + 2.0;
   }

   public void lazyTick() {
      super.lazyTick();
      this.invalidateRenderBoundingBox();
   }

   @Nullable
   public SpringBlockEntity getPairedSpring() {
      if (this.partnerPos == null) {
         return null;
      } else {
         BlockEntity be = this.level.getBlockEntity(this.partnerPos);
         return be instanceof SpringBlockEntity ? (SpringBlockEntity)be : null;
      }
   }

   public AABB getRenderBoundingBox() {
      SpringBlockEntity goal = this.getPairedSpring();
      if (goal == null) {
         return new AABB(this.getBlockPos());
      } else {
         Vec3 center = this.getBlockPos().getCenter();
         Vec3 partnerPos = this.partnerPos.getCenter();
         SubLevel subLevel = Sable.HELPER.getContaining(this);
         SubLevel partnerSubLevel = Sable.HELPER.getContaining(this.level, this.partnerPos);
         if (partnerSubLevel != null) {
            partnerPos = partnerSubLevel.logicalPose().transformPosition(partnerPos);
         }

         if (subLevel != null) {
            partnerPos = subLevel.logicalPose().transformPositionInverse(partnerPos);
         }

         return new AABB(center, partnerPos).inflate(3.0);
      }
   }

   public void setDesiredLength(double desiredLength) {
      this.desiredLength = desiredLength;
   }

   public UUID getPartnerSubLevelID() {
      return this.partnerSubLevel;
   }
}
