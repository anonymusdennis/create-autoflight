package dev.simulated_team.simulated.content.blocks.merging_glue;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.constraint.FixedConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.FixedConstraintHandle;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.util.SimAssemblyHelper;
import dev.simulated_team.simulated.util.SimMathUtils;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Vector3d;

public class MergingGlueBlockEntity extends SmartBlockEntity implements BlockEntitySubLevelActor {
   private static final int DURATION = 10;
   private static final int WAIT_ASSEMBLE = 2;
   private final Quaterniond startPartnerOrientation = new Quaterniond();
   private final Quaterniond endPartnerOrientation = new Quaterniond();
   private final Vector3d endPartnerPosition = new Vector3d();
   private final Vector3d startPartnerPosition = new Vector3d();
   private boolean hasControllingValues = false;
   private Rotation endRotation;
   @Nullable
   private BlockPos partnerPosition;
   private boolean isController;
   private int ageTicks;
   private FixedConstraintHandle lastConstraintHandle = null;

   public MergingGlueBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   public MergingGlueBlockEntity getPartnerGlue() {
      if (this.partnerPosition == null) {
         return null;
      } else {
         BlockEntity be = this.level.getBlockEntity(this.partnerPosition);
         return be instanceof MergingGlueBlockEntity ? (MergingGlueBlockEntity)be : null;
      }
   }

   public void tick() {
      super.tick();
      boolean serverSide = !this.level.isClientSide();
      if (serverSide && this.isController && this.ageTicks > 12) {
         SubLevel subLevel = Sable.HELPER.getContaining(this);
         if (Sable.HELPER.getContaining(this.level, this.partnerPosition) instanceof ServerSubLevel partnerServerSubLevel
            && partnerServerSubLevel.getMassTracker().getMass() < ((ServerSubLevel)subLevel).getMassTracker().getMass()) {
            MergingGlueBlockEntity partner = this.getPartnerGlue();
            if (partner != null) {
               this.breakGlue();

               partner.disassembleToPartner(switch (this.endRotation) {
                  case NONE -> Rotation.NONE;
                  case CLOCKWISE_90 -> Rotation.COUNTERCLOCKWISE_90;
                  case COUNTERCLOCKWISE_90 -> Rotation.CLOCKWISE_90;
                  case CLOCKWISE_180 -> Rotation.CLOCKWISE_180;
                  default -> throw new MatchException(null, null);
               });
               return;
            }
         }

         this.breakGlue();
         this.disassembleToPartner(this.endRotation);
      } else {
         if (serverSide && (this.ageTicks > 200 || this.partnerPosition == null || !this.hasControllingValues && this.isController)) {
            this.breakGlue();
         }

         this.ageTicks++;
      }
   }

   private void disassembleToPartner(Rotation rotation) {
      assert this.level != null;

      assert this.partnerPosition != null;

      SubLevel subLevel = Sable.HELPER.getContaining(this);
      BlockPos pos = this.getBlockPos().relative(((Direction)this.getBlockState().getValue(MergingGlueBlock.FACING)).getOpposite());
      SimAssemblyHelper.disassembleSubLevel(this.level, subLevel, pos, this.partnerPosition, rotation, false);
   }

   private void breakGlue() {
      if (this.level.getBlockState(this.getBlockPos()).is(SimBlocks.MERGING_GLUE)) {
         this.level.destroyBlock(this.getBlockPos(), true);
      }

      if (this.partnerPosition != null && this.level.getBlockState(this.partnerPosition).is(SimBlocks.MERGING_GLUE)) {
         this.level.destroyBlock(this.partnerPosition, true);
      }
   }

   public Vector3d getCenter(Vector3d dest) {
      BlockState state = this.getBlockState();
      Direction facing = (Direction)state.getValue(MergingGlueBlock.FACING);
      return JOMLConversion.atCenterOf(this.worldPosition, dest)
         .sub((double)facing.getStepX() * 0.5, (double)facing.getStepY() * 0.5, (double)facing.getStepZ() * 0.5);
   }

   public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double timeStep) {
      if (this.isController) {
         if (this.hasControllingValues) {
            ServerSubLevelContainer container = SubLevelContainer.getContainer(subLevel.getLevel());
            SubLevelPhysicsSystem physicsSystem = container.physicsSystem();
            double partialPhysicsTick = physicsSystem.getPartialPhysicsTick();
            double physicsTime = (double)this.ageTicks + partialPhysicsTick;
            double lerpFactor = Mth.clamp(Math.pow(physicsTime / 10.0, 5.0), 0.0, 1.0);
            double rotationLerpFactor = Mth.clamp(lerpFactor * 2.0, 0.0, 1.0);
            this.removeConstraint();
            MergingGlueBlockEntity partner = this.getPartnerGlue();
            if (partner != null) {
               if (Sable.HELPER.getContaining(partner) instanceof ServerSubLevel partnerServerSubLevel) {
                  RigidBodyHandle partnerHandle = RigidBodyHandle.of(partnerServerSubLevel);
                  Vector3d localPartnerCenter = partner.getCenter(new Vector3d());
                  this.lastConstraintHandle = (FixedConstraintHandle)physicsSystem.getPipeline()
                     .addConstraint(
                        subLevel,
                        partnerServerSubLevel,
                        new FixedConstraintConfiguration(
                           this.startPartnerPosition.lerp(this.endPartnerPosition, lerpFactor, new Vector3d()),
                           localPartnerCenter,
                           this.startPartnerOrientation.slerp(this.endPartnerOrientation, rotationLerpFactor, new Quaterniond())
                        )
                     );
               }
            }
         }
      }
   }

   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
   }

   protected void write(CompoundTag tag, Provider registries, boolean clientPacket) {
      super.write(tag, registries, clientPacket);
      tag.putBoolean("Controller", this.isController);
      if (this.partnerPosition != null) {
         tag.putLong("PartnerPosition", this.partnerPosition.asLong());
      }
   }

   protected void read(CompoundTag tag, Provider registries, boolean clientPacket) {
      super.read(tag, registries, clientPacket);
      this.isController = tag.getBoolean("Controller");
      if (tag.contains("PartnerPosition")) {
         this.partnerPosition = BlockPos.of(tag.getLong("PartnerPosition"));
      }
   }

   public void removeConstraint() {
      if (this.lastConstraintHandle != null) {
         this.lastConstraintHandle.remove();
      }

      this.lastConstraintHandle = null;
   }

   public void remove() {
      super.remove();
      this.removeConstraint();
      this.breakGlue();
   }

   public boolean isController() {
      return this.isController;
   }

   public void setPartnerPos(BlockPos partnerPos) {
      this.partnerPosition = partnerPos;
   }

   public void startControlling(MergingGlueBlockEntity partner) {
      SubLevel subLevel = Sable.HELPER.getContaining(this);
      SubLevel otherSubLevel = Sable.HELPER.getContaining(partner);
      if (subLevel != null && otherSubLevel != null) {
         Vector3d center = this.getCenter(new Vector3d());
         Vector3d partnerCenter = partner.getCenter(new Vector3d());
         otherSubLevel.logicalPose().transformPosition(partnerCenter);
         subLevel.logicalPose().transformPositionInverse(partnerCenter);
         this.startPartnerPosition.set(partnerCenter);
         this.endPartnerPosition.set(center);
         Quaterniond startOrientation = otherSubLevel.logicalPose().orientation();
         subLevel.logicalPose().orientation().conjugate(new Quaterniond()).mul(startOrientation, startOrientation);
         this.startPartnerOrientation.set(startOrientation);
         this.endPartnerOrientation.set(new Quaterniond());
         Direction direction = (Direction)this.getBlockState().getValue(MergingGlueBlock.FACING);
         Direction partnerDirection = (Direction)partner.getBlockState().getValue(MergingGlueBlock.FACING);
         if (direction.getAxis().isVertical()) {
            double yRotation = SimMathUtils.getClosestYaw(startOrientation);
            double ninety = Math.PI / 2;
            int turns = -Mth.floor(yRotation / (Math.PI / 2) + 0.5);
            this.endPartnerOrientation.rotateY((double)turns * (Math.PI / 2));
            this.endRotation = SimAssemblyHelper.rotationFrom90DegRots(-turns);
         } else {
            Vec3i normal = direction.getNormal();
            Vec3i partnerNormal = partnerDirection.getNormal();
            double angle = Math.atan2((double)partnerNormal.getX(), (double)partnerNormal.getZ()) - Math.atan2((double)normal.getX(), (double)normal.getZ());
            if (direction.getAxis() == partnerDirection.getAxis()) {
               angle += Math.PI;
            }

            double ninety = Math.PI / 2;
            int turns = -Mth.floor(angle / (Math.PI / 2) + 0.5);
            this.endPartnerOrientation.rotateY(angle);
            this.endRotation = SimAssemblyHelper.rotationFrom90DegRots(turns);
         }

         this.isController = true;
         this.hasControllingValues = true;
      }
   }
}
