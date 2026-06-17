package dev.simulated_team.simulated.content.blocks.physics_assembler;

import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.IDisplayAssemblyExceptions;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis;
import dev.ryanhcode.sable.api.physics.constraint.FreeConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.FreeConstraintHandle;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.config.server.blocks.SimAssembly;
import dev.simulated_team.simulated.content.blocks.behaviour.HoldTipBehaviour;
import dev.simulated_team.simulated.content.blocks.physics_assembler.assembly_preventer.DisassemblyPrevention;
import dev.simulated_team.simulated.content.physics_staff.PhysicsStaffServerHandler;
import dev.simulated_team.simulated.data.SimLang;
import dev.simulated_team.simulated.mixin_interface.assembly_preventer.PrimaryAssemblerExtension;
import dev.simulated_team.simulated.network.packets.physics_assembler.PhysicsAssemblerFailedPacket;
import dev.simulated_team.simulated.network.packets.physics_assembler.PhysicsAssemblerFlickAndHoldLeverPacket;
import dev.simulated_team.simulated.service.SimConfigService;
import dev.simulated_team.simulated.util.SimAssemblyHelper;
import dev.simulated_team.simulated.util.SimMathUtils;
import dev.simulated_team.simulated.util.assembly.SimAssemblyException;
import foundry.veil.api.network.VeilPacketManager;
import java.util.List;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;

public class PhysicsAssemblerBlockEntity extends SmartBlockEntity implements IDisplayAssemblyExceptions {
   private static final float FLICKED_ANGLE_DEGREES = 45.0F;
   private static final double LEVER_CHASE_SPEED = 0.75;
   private static final double LINEAR_STIFFNESS = 1000.0;
   private static final double LINEAR_DAMPING = 50.0;
   private static final double ANGULAR_STIFFNESS = 13000.0;
   private static final double ANGULAR_DAMPING = 1000.0;
   private static final MutableComponent ASSEMBLE_TIP = SimLang.translate("gui.hold_tip.hold_to_assemble").component();
   private static final MutableComponent DISASSEMBLE_TIP = SimLang.translate("gui.hold_tip.hold_to_disassemble").component();
   protected AssemblyException lastException;
   protected boolean primaryAssembler;
   protected LerpedFloat visualAngle = LerpedFloat.linear();
   protected boolean holdingLever = false;
   private boolean leverInitialized = false;
   private boolean disassembling = false;
   private int disassemblingTicks = 0;
   private int disassemblyReadyTicks = 0;
   private int disassemblyAngle = 0;
   private Quaterniondc disassemblyOrientation;
   private boolean controlledByPlayer = false;
   private float playerAngle = 0.0F;
   @Nullable
   private FreeConstraintHandle alignmentConstraint;
   private HoldTipBehaviour holdTipBehaviour;

   public PhysicsAssemblerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      behaviours.add(this.holdTipBehaviour = new HoldTipBehaviour(this, ASSEMBLE_TIP));
   }

   public void initialize() {
      super.initialize();
      if (this.primaryAssembler) {
         this.setParent(this.level);
      }

      if (!this.isVirtual()) {
         this.initializeLeverPosition();
         this.holdTipBehaviour.setHoverTip(this.getSubLevel() != null ? DISASSEMBLE_TIP : ASSEMBLE_TIP);
      }
   }

   protected void initializeLeverPosition() {
      if (!this.leverInitialized) {
         this.clientFlickLeverTo(this.getSubLevel() != null);
         this.jerkLever();
         this.leverInitialized = true;
      }
   }

   @Nullable
   private SubLevel getSubLevel() {
      return Sable.HELPER.getContaining(this);
   }

   public void tick() {
      super.tick();
      if (this.disassembling) {
         this.tickDisassembling();
      }

      if (this.holdingLever) {
         this.visualAngle.setValue((double)this.visualAngle.getValue());
      } else if (this.controlledByPlayer) {
         this.visualAngle.setValue((double)this.visualAngle.getValue());
         this.visualAngle.setValueNoUpdate((double)this.playerAngle);
      } else {
         this.visualAngle.tickChaser();
      }
   }

   private void tickDisassembling() {
      this.disassemblingTicks++;
      SimAssembly config = SimConfigService.INSTANCE.server().assembly;
      if (this.disassemblingTicks >= (Integer)config.maxDisassemblyTicks.get() * 5) {
         this.assemblyFailed(SimAssemblyException.couldNotAlign());
         this.stopDisassembling();
      } else {
         SubLevel subLevel = this.getSubLevel();
         if (subLevel instanceof ServerSubLevel) {
            Pose3d pose = subLevel.logicalPose();
            double angle = pose.orientation().div(this.disassemblyOrientation, new Quaterniond()).angle();
            Vector3d current = pose.transformPosition(new Vector3d(pose.rotationPoint()).floor().add(0.5, 0.5, 0.5));
            Vector3d goal = current.floor(new Vector3d()).add(0.5, 0.5, 0.5);
            Vector3d localGoal = this.disassemblyOrientation.transformInverse(goal, new Vector3d());
            this.alignmentConstraint.setMotor(ConstraintJointAxis.LINEAR_X, localGoal.x, 1000.0, 50.0, false, 0.0);
            this.alignmentConstraint.setMotor(ConstraintJointAxis.LINEAR_Y, localGoal.y, 1000.0, 50.0, false, 0.0);
            this.alignmentConstraint.setMotor(ConstraintJointAxis.LINEAR_Z, localGoal.z, 1000.0, 50.0, false, 0.0);
            if (Math.toDegrees(Math.abs(angle)) <= (Double)config.disassemblyDegreeTolerance.get() && current.distance(goal) < 0.2) {
               this.disassemblyReadyTicks++;
            } else {
               this.disassemblyReadyTicks = 0;
            }

            if (this.disassemblyReadyTicks > 5) {
               this.placeIntoWorld();
            }
         }
      }
   }

   private void placeIntoWorld() {
      SubLevel subLevel = this.getSubLevel();

      assert subLevel != null;

      try {
         this.throwDisassemblyExceptions((ServerSubLevel)subLevel);
      } catch (AssemblyException var4) {
         this.assemblyFailed(var4);
         this.stopDisassembling();
         return;
      }

      BlockPos goal = BlockPos.containing(subLevel.logicalPose().transformPosition(Vec3.atCenterOf(this.getBlockPos())));
      Rotation rotation = SimAssemblyHelper.rotationFrom90DegRots(this.disassemblyAngle);
      SimAssemblyHelper.disassembleSubLevel(this.level, subLevel, this.getBlockPos(), goal, rotation, true);
      this.stopDisassembling();
   }

   private void throwDisassemblyExceptions(ServerSubLevel subLevel) throws AssemblyException {
      BoundingBox3dc bounds = subLevel.boundingBox();
      if (!(bounds.maxY() > (double)this.level.getMaxBuildHeight()) && !(bounds.minY() < (double)this.level.getMinBuildHeight())) {
         SimAssembly config = SimConfigService.INSTANCE.server().assembly;
         RigidBodyHandle handle = RigidBodyHandle.of(subLevel);
         if (!(handle.getLinearVelocity(new Vector3d()).lengthSquared() > (double)Mth.square(config.disassemblyMaxVelocity.getF()))
            && !(handle.getAngularVelocity(new Vector3d()).lengthSquared() > (double)Mth.square(config.disassemblyMaxAngularVelocity.getF()))) {
            BoundingBox3i chunkBounds = new BoundingBox3i(
               (Mth.floor(bounds.minX()) >> 4) - 1,
               (Mth.floor(bounds.minY()) >> 4) - 1,
               (Mth.floor(bounds.minZ()) >> 4) - 1,
               (Mth.floor(bounds.maxX()) >> 4) + 1,
               (Mth.floor(bounds.maxY()) >> 4) + 1,
               (Mth.floor(bounds.maxZ()) >> 4) + 1
            );
            if ((Boolean)config.disallowMidAirDisassembly.get()) {
               boolean nearGround = false;

               label48:
               for (int x = chunkBounds.minX(); x <= chunkBounds.maxX(); x++) {
                  for (int z = chunkBounds.minZ(); z <= chunkBounds.maxZ(); z++) {
                     LevelChunk chunk = this.level.getChunk(x, z);

                     for (int y = chunkBounds.minY(); y <= chunkBounds.maxY(); y++) {
                        int index = chunk.getSectionIndexFromSectionY(y);
                        if (index >= 0 && index < chunk.getSectionsCount() && !chunk.getSection(index).hasOnlyAir()) {
                           nearGround = true;
                           break label48;
                        }
                     }
                  }
               }

               if (!nearGround) {
                  throw SimAssemblyException.tooFarFromGround();
               }
            }
         } else {
            throw SimAssemblyException.tooFast();
         }
      } else {
         throw SimAssemblyException.outOfWorld();
      }
   }

   private void stopDisassembling() {
      if (this.alignmentConstraint != null && this.alignmentConstraint.isValid()) {
         this.alignmentConstraint.remove();
         this.alignmentConstraint = null;
      }

      this.disassemblingTicks = 0;
      this.disassembling = false;
   }

   public void setClientHoldLeverInPlace(boolean holding) {
      this.holdingLever = holding;
   }

   public void updateControlledByPlayer(float angle) {
      if (!this.controlledByPlayer) {
         this.controlledByPlayer = true;
      }

      this.playerAngle = angle;
   }

   public boolean stopControllingPlayer() {
      if (!this.controlledByPlayer) {
         return false;
      } else {
         this.controlledByPlayer = false;
         return true;
      }
   }

   public void clientFlickLeverTo(boolean flicked) {
      this.visualAngle.chase(flicked ? 45.0 : 0.0, 0.75, Chaser.EXP);
   }

   public void jerkLever() {
      this.visualAngle.setValue((double)this.visualAngle.getChaseTarget());
      this.visualAngle.setValue((double)this.visualAngle.getChaseTarget());
   }

   public void assembleOrDisassemble() {
      SubLevel subLevel = Sable.HELPER.getContaining(this);
      Level level = this.getLevel();

      assert level != null;

      try {
         VeilPacketManager.tracking(this)
            .sendPacket(new CustomPacketPayload[]{new PhysicsAssemblerFlickAndHoldLeverPacket(this.worldPosition, subLevel == null)});
         if (subLevel instanceof ServerSubLevel serverSubLevel) {
            if (DisassemblyPrevention.checkSubLevelForPrimary(level, this.getBlockPos())) {
               this.throwDisassemblyExceptions(serverSubLevel);
               this.startDisassembling(serverSubLevel, (ServerLevel)level, subLevel);
               this.disassembling = true;
            }
         } else {
            this.primaryAssembler = true;
            BlockPos toAssemble = this.getBlockPos().relative(PhysicsAssemblerBlock.getStickyFacing(this.getBlockState()));
            SimAssemblyHelper.assembleFromSingleBlock(level, this.getBlockPos(), toAssemble, true, true);
            this.lastException = null;
            this.sendData();
         }
      } catch (AssemblyException var5) {
         if (!(subLevel instanceof ServerSubLevel)) {
            this.primaryAssembler = false;
         }

         this.assemblyFailed(var5);
      }
   }

   private void assemblyFailed(AssemblyException exception) {
      this.lastException = exception;
      VeilPacketManager.tracking(this).sendPacket(new CustomPacketPayload[]{new PhysicsAssemblerFailedPacket(this.worldPosition)});
      this.sendData();
   }

   private void startDisassembling(ServerSubLevel serverSubLevel, ServerLevel level, SubLevel subLevel) {
      ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
      PhysicsPipeline pipeline = container.physicsSystem().getPipeline();
      MassData massTracker = serverSubLevel.getMassTracker();
      double closestYRotation = SimMathUtils.getClosestYaw(subLevel.logicalPose().orientation());
      double ninety = Math.PI / 2;
      int turns = -Mth.floor(closestYRotation / (Math.PI / 2) + 0.5);
      this.disassemblyAngle = turns;
      FreeConstraintConfiguration config = new FreeConstraintConfiguration(
         new Vector3d(),
         new Vector3d(massTracker.getCenterOfMass()).floor().add(0.5, 0.5, 0.5),
         this.disassemblyOrientation = new Quaterniond().rotateY((double)turns * (Math.PI / 2))
      );
      this.alignmentConstraint = (FreeConstraintHandle)pipeline.addConstraint(null, serverSubLevel, config);
      this.alignmentConstraint.setMotor(ConstraintJointAxis.ANGULAR_X, 0.0, 13000.0, 1000.0, false, 0.0);
      this.alignmentConstraint.setMotor(ConstraintJointAxis.ANGULAR_Z, 0.0, 13000.0, 1000.0, false, 0.0);
      this.alignmentConstraint.setMotor(ConstraintJointAxis.ANGULAR_Y, 0.0, 13000.0, 1000.0, false, 0.0);
      this.alignmentConstraint.setMotor(ConstraintJointAxis.LINEAR_X, 0.0, 1.0E-6, 50.0, false, 0.0);
      this.alignmentConstraint.setMotor(ConstraintJointAxis.LINEAR_Y, 0.0, 1.0E-6, 50.0, false, 0.0);
      this.alignmentConstraint.setMotor(ConstraintJointAxis.LINEAR_Z, 0.0, 1.0E-6, 50.0, false, 0.0);
      this.disassembling = true;
      this.disassemblingTicks = 0;
      PhysicsStaffServerHandler.get(level).removeLock(serverSubLevel);
   }

   public void remove() {
      if (this.primaryAssembler && !this.level.isClientSide && this.getSubLevel() instanceof ServerSubLevel ssb) {
         ((PrimaryAssemblerExtension)ssb).simulated$setPrimaryAssembler(null);
      }

      this.stopDisassembling();
      super.remove();
   }

   public void write(CompoundTag compound, Provider registries, boolean clientPacket) {
      super.write(compound, registries, clientPacket);
      AssemblyException.write(compound, registries, this.lastException);
      compound.putBoolean("IsPrimary", this.primaryAssembler);
   }

   protected void read(CompoundTag tag, Provider registries, boolean clientPacket) {
      super.read(tag, registries, clientPacket);
      this.lastException = AssemblyException.read(tag, registries);
      this.primaryAssembler = tag.getBoolean("IsPrimary");
   }

   public AssemblyException getLastAssemblyException() {
      return this.lastException;
   }

   public boolean isPrimaryAssembler() {
      return this.primaryAssembler;
   }

   protected void setParent(Level level) {
      this.lastException = null;
      SubLevel subLevel = Sable.HELPER.getContaining(level, this.getBlockPos());
      if (!level.isClientSide && this.primaryAssembler && subLevel instanceof ServerSubLevel) {
         PrimaryAssemblerExtension duck = (PrimaryAssemblerExtension)subLevel;
         if (duck.simulated$getPrimaryAssembler() == null) {
            duck.simulated$setPrimaryAssembler(this.getBlockPos());
         }
      } else {
         this.primaryAssembler = false;
      }
   }

   public float getClientAngle(float partialTicks) {
      return this.visualAngle.getValue(partialTicks);
   }
}
