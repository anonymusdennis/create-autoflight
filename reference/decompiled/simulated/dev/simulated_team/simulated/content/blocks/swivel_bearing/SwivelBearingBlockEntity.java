package dev.simulated_team.simulated.content.blocks.swivel_bearing;

import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.IDisplayAssemblyExceptions;
import com.simibubi.create.content.contraptions.bearing.BearingBlock;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.content.kinetics.transmission.sequencer.SequencerInstructions;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.item.TooltipHelper;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.constraint.RotaryConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.RotaryConstraintHandle;
import dev.ryanhcode.sable.api.schematic.SubLevelSchematicSerializationContext;
import dev.ryanhcode.sable.api.schematic.SubLevelSchematicSerializationContext.SchematicMapping;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.simulated_team.simulated.config.server.physics.SimPhysics;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.link_block.SwivelBearingPlateBlock;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.link_block.SwivelBearingPlateBlockEntity;
import dev.simulated_team.simulated.data.SimLang;
import dev.simulated_team.simulated.data.advancements.SimAdvancements;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.index.SimSoundEvents;
import dev.simulated_team.simulated.service.SimConfigService;
import dev.simulated_team.simulated.util.SimAssemblyHelper;
import dev.simulated_team.simulated.util.SimLevelUtil;
import dev.simulated_team.simulated.util.assembly.SimAssemblyException;
import dev.simulated_team.simulated.util.extra_kinetics.ExtraBlockPos;
import dev.simulated_team.simulated.util.extra_kinetics.ExtraKinetics;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiPredicate;
import net.createmod.catnip.lang.FontHelper.Palette;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class SwivelBearingBlockEntity extends KineticBlockEntity implements ExtraKinetics, IDisplayAssemblyExceptions, BlockEntitySubLevelActor {
   private static final MutableComponent SCROLL_OPTION_TITLE = Component.translatable("simulated.scroll_option.swivel_default_locked");
   @NotNull
   private final SwivelBearingBlockEntity.SwivelBearingCogwheelBlockEntity cogwheel;
   public boolean assembleNextTick;
   protected AssemblyException lastException;
   private double lastTargetAngleDegrees = 0.0;
   private double targetAngleDegrees = 0.0;
   private double sequencedAngleLimit = -1.0;
   @Nullable
   private UUID subLevelID;
   @Nullable
   private BlockPos swivelPlatePos;
   @Nullable
   private RotaryConstraintHandle handle;
   private boolean assembling;
   private ScrollOptionBehaviour<SwivelBearingBlockEntity.LockingSetting> lockedDefaultOption;

   public SwivelBearingBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
      super(typeIn, pos, state);
      this.assembleNextTick = false;
      this.cogwheel = new SwivelBearingBlockEntity.SwivelBearingCogwheelBlockEntity(typeIn, pos, state, this);
   }

   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      super.addBehaviours(behaviours);
      this.lockedDefaultOption = new ScrollOptionBehaviour(
         SwivelBearingBlockEntity.LockingSetting.class,
         SCROLL_OPTION_TITLE,
         this,
         new SwivelBearingBlockEntity.SelectionModeValueBox(this::isValidForOptionPanel)
      );
      this.lockedDefaultOption.value = 1;
      behaviours.add(this.lockedDefaultOption);
   }

   private boolean isValidForOptionPanel(BlockState state, Direction direction) {
      Direction facing = (Direction)state.getValue(SwivelBearingBlock.FACING);
      Axis currentAxis = facing.getAxis();
      return direction.getAxis() != currentAxis;
   }

   public void tick() {
      Level level = this.getLevel();
      super.tick();
      this.cogwheel.tick();
      if (level.isClientSide) {
         if (this.isTooFast()) {
            this.playGrindingEffect();
         }
      } else {
         if (this.assembleNextTick) {
            if (!this.isAssembled()) {
               this.assemble();
            } else {
               this.disassemble();
            }
         }

         ServerSubLevel attached = (ServerSubLevel)this.getAttachedSubLevel();
         int bestSignal = this.level.getBestNeighborSignal(this.getBlockPos());
         boolean shouldLock = ((SwivelBearingBlockEntity.LockingSetting)this.lockedDefaultOption.get()).shouldLock(bestSignal);
         if (shouldLock && !this.isLocking()) {
            this.level.setBlockAndUpdate(this.getBlockPos(), (BlockState)this.getBlockState().setValue(BlockStateProperties.POWERED, true));
            if (this.handle != null) {
               this.reattachConstraint(attached, false);
            }

            if (attached != null && this.getPlatePos() != null) {
               BlockState plateBlock = this.level.getBlockState(this.getPlatePos());
               if (plateBlock.is(SimBlocks.SWIVEL_BEARING_LINK_BLOCK)) {
                  this.setTargetAngleFromCurrentOrientation(plateBlock, attached);
               }
            }
         } else if (!shouldLock && this.isLocking()) {
            this.level.setBlockAndUpdate(this.getBlockPos(), (BlockState)this.getBlockState().setValue(BlockStateProperties.POWERED, false));
            if (this.handle != null) {
               this.reattachConstraint(attached, false);
            }
         }

         if (this.getSubLevelID() != null) {
            this.checkPersistence(this.getSubLevelID());
         }

         this.lastTargetAngleDegrees = this.targetAngleDegrees;
         float angularSpeed = convertToAngular(this.limitCogSpeed(this.cogwheel.getSpeed()));
         boolean shouldUpdateAngle = this.isAssembled();
         if (this.sequencedAngleLimit >= 0.0) {
            angularSpeed = (float)Mth.clamp((double)angularSpeed, -this.sequencedAngleLimit, this.sequencedAngleLimit);
            this.sequencedAngleLimit = Math.max(0.0, this.sequencedAngleLimit - (double)Math.abs(angularSpeed));
         } else {
            SubLevelPhysicsSystem physicsSystem = SubLevelPhysicsSystem.get(this.level);
            if (physicsSystem == null || physicsSystem.getPaused()) {
               shouldUpdateAngle = false;
            }
         }

         if (shouldUpdateAngle) {
            if (((Direction)this.getBlockState().getValue(SwivelBearingBlock.FACING)).getAxisDirection() == AxisDirection.NEGATIVE) {
               angularSpeed *= -1.0F;
            }

            this.targetAngleDegrees += (double)angularSpeed;
            this.targetAngleDegrees %= 360.0;
            if (attached != null && this.isAssembled() && this.handle != null) {
               SubLevel containing = this.getContainingSubLevel();
               if ((double)angularSpeed != 0.0) {
                  PhysicsPipeline pipeline = ((ServerSubLevelContainer)SubLevelContainer.getContainer(this.level)).physicsSystem().getPipeline();
                  if (containing instanceof ServerSubLevel serverSubLevel) {
                     pipeline.wakeUp(serverSubLevel);
                  }

                  if (attached instanceof ServerSubLevel) {
                     pipeline.wakeUp(attached);
                  }
               }
            }
         }

         this.assembleNextTick = false;
      }
   }

   private void playGrindingEffect() {
      Direction facing = (Direction)this.getBlockState().getValue(SwivelBearingBlock.FACING);
      RandomSource random = this.level.random;
      int stepX = facing.getStepX();
      int stepY = facing.getStepY();
      int stepZ = facing.getStepZ();

      for (int i = 0; i < 2; i++) {
         Vec3 particlePos = this.getBlockPos()
            .getCenter()
            .add((double)stepX * 7.0 / 16.0, (double)stepY * 7.0 / 16.0, (double)stepZ * 7.0 / 16.0)
            .add(
               (double)((random.nextFloat() - 0.5F) * (float)(stepX == 0 ? 1 : 0)),
               (double)((random.nextFloat() - 0.5F) * (float)(stepY == 0 ? 1 : 0)),
               (double)((random.nextFloat() - 0.5F) * (float)(stepZ == 0 ? 1 : 0))
            );
         this.level.addParticle(ParticleTypes.CRIT, particlePos.x, particlePos.y, particlePos.z, 0.0, 0.0, 0.0);
      }
   }

   public boolean addToTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
      if (super.addToTooltip(tooltip, isPlayerSneaking)) {
         return true;
      } else if (isPlayerSneaking) {
         return false;
      } else if (this.cogwheel.getSpeed() == 0.0F) {
         return false;
      } else if (this.isAssembled()) {
         if (this.isTooFast()) {
            SimLang.translate("swivel_bearing.too_fast").style(ChatFormatting.GOLD).forGoggles(tooltip);
            MutableComponent component = SimLang.translate("swivel_bearing.too_fast_error").component();
            List<Component> cutString = TooltipHelper.cutTextComponent(component, Palette.GRAY_AND_WHITE);
            tooltip.addAll(cutString);
            return true;
         } else {
            return false;
         }
      } else {
         BlockState state = this.getBlockState();
         if (!(state.getBlock() instanceof SwivelBearingBlock)) {
            return false;
         } else {
            BlockState attachedState = this.level.getBlockState(this.worldPosition.relative((Direction)state.getValue(BearingBlock.FACING)));
            if (attachedState.canBeReplaced()) {
               return false;
            } else {
               TooltipHelper.addHint(tooltip, "hint.empty_bearing", new Object[0]);
               return true;
            }
         }
      }
   }

   private boolean isTooFast() {
      float maxSwivelRPM = SimConfigService.INSTANCE.server().blocks.maxSwivelBearingSpeed.getF();
      return Math.abs(this.cogwheel.getSpeed()) > maxSwivelRPM;
   }

   private float limitCogSpeed(float speed) {
      float maxSwivelRPM = SimConfigService.INSTANCE.server().blocks.maxSwivelBearingSpeed.getF();
      return Mth.clamp(speed, -maxSwivelRPM, maxSwivelRPM);
   }

   private void setTargetAngleFromCurrentOrientation(BlockState attachedState, SubLevel attached) {
      assert attached != null : "Attached sub-level is null!";

      Quaterniond orientationA = new Quaterniond();
      Quaterniond blockOrientationA = new Quaterniond(((Direction)this.getBlockState().getValue(SwivelBearingPlateBlock.FACING)).getRotation());
      Quaterniond blockOrientationB = new Quaterniond(((Direction)attachedState.getValue(SwivelBearingPlateBlock.FACING)).getRotation());
      Quaterniond orientationB = new Quaterniond(attached.logicalPose().orientation());
      SubLevel containing = this.getContainingSubLevel();
      if (containing != null) {
         orientationA.set(containing.logicalPose().orientation());
      }

      Quaterniond localB = new Quaterniond(orientationA).mul(blockOrientationA).conjugate().mul(new Quaterniond(orientationB).mul(blockOrientationB));
      double d = new Vec3(0.0, 1.0, 0.0).dot(new Vec3(localB.x(), localB.y(), localB.z()));
      double currentAngle = -2.0 * (double)((float)Math.toDegrees(Math.atan2(-d, localB.w())));
      this.targetAngleDegrees = currentAngle;
      this.lastTargetAngleDegrees = currentAngle;
   }

   public void updateServoCoefficients() {
      this.validateConstraintHandle();
      if (this.isAssembled() && this.handle != null) {
         SimPhysics config = SimConfigService.INSTANCE.server().physics;
         if (!this.isLocking()) {
            this.handle.setMotor(RotaryConstraintHandle.DEFAULT_AXIS, 0.0, 0.0, (Double)config.swivelBearingFriction.get(), false, 0.0);
         } else {
            SubLevel subLevelA = this.getContainingSubLevel();
            SubLevel subLevelB = this.getAttachedSubLevel();
            Vec3i facingVec3I = ((Direction)this.getBlockState().getValue(DirectionalKineticBlock.FACING)).getNormal();
            Vector3dc facingVec = new Vector3d((double)facingVec3I.getX(), (double)facingVec3I.getY(), (double)facingVec3I.getZ());
            double inertiaA = Double.MAX_VALUE;
            double inertiaB = Double.MAX_VALUE;
            Vector3d temp = new Vector3d();
            if (subLevelA instanceof ServerSubLevel serverSubLevel) {
               inertiaA = serverSubLevel.getMassTracker().getInertiaTensor().transform(facingVec, temp).dot(facingVec);
            }

            if (subLevelB instanceof ServerSubLevel serverSubLevel) {
               inertiaB = serverSubLevel.getMassTracker().getInertiaTensor().transform(facingVec, temp).dot(facingVec);
            }

            double totalInertia = Math.max(10.0, subLevelA != null && subLevelB != null ? Math.max(inertiaA, inertiaB) : Math.min(inertiaA, inertiaB));
            SubLevelPhysicsSystem physicsSystem = ((ServerSubLevelContainer)SubLevelContainer.getContainer(this.level)).physicsSystem();
            double kP = (Double)config.swivelBearingStiffness.get() * totalInertia;
            double kD = (Double)config.swivelBearingDamping.get() * totalInertia;
            float goal = AngleHelper.rad(
               (double)AngleHelper.angleLerp(physicsSystem.getPartialPhysicsTick(), this.lastTargetAngleDegrees, this.targetAngleDegrees)
            );
            this.handle.setMotor(RotaryConstraintHandle.DEFAULT_AXIS, (double)goal, kP, kD, false, 0.0);
            this.handle.setContactsEnabled(false);
         }
      }
   }

   private void validateConstraintHandle() {
      if (this.handle != null && !this.handle.isValid()) {
         this.handle = null;
      }
   }

   public void assemble() {
      BlockPos pos = this.getBlockPos();
      BlockPos toAssemble = pos.relative((Direction)this.getBlockState().getValue(SwivelBearingBlock.FACING));

      SimAssemblyHelper.AssemblyResult result;
      try {
         result = SimAssemblyHelper.assembleFromSingleBlock(this.level, pos, toAssemble, false, false);
         this.lastException = null;
      } catch (AssemblyException var17) {
         this.lastException = var17;
         this.sendData();
         return;
      }

      this.sendData();
      BlockState link = (BlockState)SimBlocks.SWIVEL_BEARING_LINK_BLOCK
         .getDefaultState()
         .setValue(SwivelBearingPlateBlock.FACING, (Direction)this.getBlockState().getValue(SwivelBearingBlock.FACING));
      ServerSubLevel assembledSubLevel;
      BlockPos assembleOffset;
      if (result != null) {
         assembledSubLevel = (ServerSubLevel)result.subLevel();
         assembleOffset = result.offset();
      } else {
         ServerSubLevelContainer container = (ServerSubLevelContainer)SubLevelContainer.getContainer(this.level);
         Pose3d pose = new Pose3d();
         pose.position().set((double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5);
         assembledSubLevel = (ServerSubLevel)container.allocateNewSubLevel(pose);
         LevelPlot plot = assembledSubLevel.getPlot();
         ChunkPos center = plot.getCenterChunk();
         plot.newEmptyChunk(center);
         plot.getEmbeddedLevelAccessor().setBlock(BlockPos.ZERO, link, 3);
         BlockPos plotAnchor = plot.getCenterBlock();
         Vector3dc centerOfMass = assembledSubLevel.getMassTracker().getCenterOfMass();
         Vector3d subLevelCenter = JOMLConversion.atLowerCornerOf(pos);
         if (centerOfMass != null) {
            subLevelCenter.add(
               centerOfMass.x() - (double)plotAnchor.getX(), centerOfMass.y() - (double)plotAnchor.getY(), centerOfMass.z() - (double)plotAnchor.getZ()
            );
         } else {
            assembledSubLevel.logicalPose()
               .rotationPoint()
               .set((double)plotAnchor.getX() + 0.5, (double)plotAnchor.getY() + 0.5, (double)plotAnchor.getZ() + 0.5);
         }

         assembledSubLevel.logicalPose().position().set(subLevelCenter.x, subLevelCenter.y, subLevelCenter.z);
         assembleOffset = plotAnchor.subtract(pos);
         SubLevelPhysicsSystem physicsSystem = container.physicsSystem();
         PhysicsPipeline pipeline = physicsSystem.getPipeline();
         SubLevel containingSubLevel = this.getContainingSubLevel();
         if (containingSubLevel != null) {
            SubLevelAssemblyHelper.kickFromContainingSubLevel((ServerLevel)this.level, physicsSystem, pipeline, assembledSubLevel, containingSubLevel);
            assembledSubLevel.logicalPose().orientation().set(containingSubLevel.logicalPose().orientation());
         }

         pipeline.teleport(assembledSubLevel, assembledSubLevel.logicalPose().position(), assembledSubLevel.logicalPose().orientation());
         assembledSubLevel.updateLastPose();
         this.level.playSound(null, pos, SimSoundEvents.SIMULATED_CONTRAPTION_MOVES.event(), SoundSource.BLOCKS, 1.0F, 1.0F);
      }

      this.getLevel().setBlockAndUpdate(pos, (BlockState)this.getBlockState().setValue(SwivelBearingBlock.ASSEMBLED, true));
      this.attachConstraints(assembledSubLevel, this.getConstraintPos(toAssemble, assembleOffset));
      this.setSubLevelID(assembledSubLevel.getUniqueId());
      BlockPos plotPos = pos.offset(assembleOffset);
      if (result != null) {
         this.getLevel().setBlockAndUpdate(plotPos, link);
      }

      if (this.getLevel().getBlockEntity(plotPos) instanceof SwivelBearingPlateBlockEntity plateBE) {
         plateBE.setParent(this);
         this.setPlatePos(plotPos);
      }

      SimAdvancements.YOU_SPIN_ME_RIGHT_ROUND.awardToNearby(pos, this.getLevel());
   }

   public void disassemble() {
      if (!this.isRemoved()) {
         this.removeHandle();
         SubLevel subLevel = SubLevelContainer.getContainer(this.level).getSubLevel(this.getSubLevelID());
         BlockPos platePos = this.getPlatePos();
         if (platePos != null) {
            this.destroyPlate();
            if (Objects.equals(subLevel, Sable.HELPER.getContaining(this.level, this.getBlockPos()))) {
               this.lastException = SimAssemblyException.sameSubLevel();
               this.level.playSound(null, platePos, SimSoundEvents.ASSEMBLER_FAIL.event(), SoundSource.BLOCKS, 1.0F, 1.0F);
            } else if (subLevel != null) {
               if (!subLevel.isRemoved()) {
                  SimAssemblyHelper.disassembleSubLevel(this.level, subLevel, platePos, this.getBlockPos(), Rotation.NONE, true);
               } else {
                  this.level.playSound(null, platePos, SimSoundEvents.SIMULATED_CONTRAPTION_STOPS.event(), SoundSource.BLOCKS, 1.0F, 1.0F);
               }
            }
         }

         this.getLevel().setBlockAndUpdate(this.getBlockPos(), (BlockState)this.getBlockState().setValue(SwivelBearingBlock.ASSEMBLED, false));
         this.setSubLevelID(null);
         this.setPlatePos(null);
         this.targetAngleDegrees = 0.0;
         this.sendData();
      }
   }

   private void checkPersistence(UUID id) {
      if (this.getPlatePos() == null
         || !SimLevelUtil.isAreaActuallyLoaded(this.getLevel(), this.getPlatePos(), 1)
         || this.getLevel().getBlockState(this.getPlatePos()).is(SimBlocks.SWIVEL_BEARING_LINK_BLOCK)) {
         SubLevel subLevel = SubLevelContainer.getContainer(this.getLevel()).getSubLevel(id);
         this.validateConstraintHandle();
         if (this.handle == null) {
            this.reattachConstraint((ServerSubLevel)subLevel, true);
         }
      }
   }

   public void reattachConstraint(@Nullable ServerSubLevel plateSubLevel, boolean updatePlate) {
      BlockPos platePos = this.getPlatePos();
      if (platePos != null) {
         if (this.handle != null) {
            this.handle.remove();
         }

         if (updatePlate) {
            this.associatePlateWithParent();
         }

         BlockState plateState = this.level.getBlockState(platePos);
         if (!plateState.is(SimBlocks.SWIVEL_BEARING_LINK_BLOCK)) {
            return;
         }

         Direction plateFacing = (Direction)plateState.getValue(SwivelBearingPlateBlock.FACING);
         this.attachConstraints(plateSubLevel, JOMLConversion.toJOML(platePos.relative(plateFacing).getCenter()));
      }
   }

   public void associatePlateWithParent() {
      if (this.getPlatePos() != null && this.getLevel().getBlockState(this.getPlatePos()).is(SimBlocks.SWIVEL_BEARING_LINK_BLOCK)) {
         SwivelBearingPlateBlockEntity plate = (SwivelBearingPlateBlockEntity)this.getLevel().getBlockEntity(this.getPlatePos());
         plate.setParent(this);
      }
   }

   private void attachConstraints(@Nullable ServerSubLevel plateSubLevel, Vector3d attachPos) {
      BlockPos platePos = this.getPlatePos();
      if (platePos != null) {
         BlockState plateState = this.level.getBlockState(platePos);
         if (plateState.is(SimBlocks.SWIVEL_BEARING_LINK_BLOCK)) {
            Vector3d anchorPos = JOMLConversion.toJOML(
               this.getBlockPos().relative((Direction)this.getBlockState().getValue(DirectionalKineticBlock.FACING)).getCenter()
            );
            Vec3 facingVec = Vec3.atLowerCornerOf(((Direction)this.getBlockState().getValue(DirectionalKineticBlock.FACING)).getNormal());
            Vec3 plateFacingVec = Vec3.atLowerCornerOf(((Direction)plateState.getValue(DirectionalKineticBlock.FACING)).getNormal());
            RotaryConstraintConfiguration constraint = new RotaryConstraintConfiguration(
               anchorPos,
               attachPos.sub(JOMLConversion.toJOML(plateFacingVec.scale(0.001F))),
               JOMLConversion.toJOML(facingVec),
               JOMLConversion.toJOML(plateFacingVec)
            );
            ServerSubLevelContainer container = SubLevelContainer.getContainer((ServerLevel)this.getLevel());
            ServerSubLevel containingSubLevel = (ServerSubLevel)Sable.HELPER.getContaining(this);
            PhysicsPipeline pipeline = container.physicsSystem().getPipeline();
            if (containingSubLevel != plateSubLevel) {
               this.handle = (RotaryConstraintHandle)pipeline.addConstraint(containingSubLevel, plateSubLevel, constraint);
            }
         }
      }
   }

   protected void write(CompoundTag compound, Provider registries, boolean clientPacket) {
      super.write(compound, registries, clientPacket);
      compound.putDouble("TargetAngle", this.targetAngleDegrees);
      BlockPos platePos = this.getPlatePos();
      UUID id = this.getSubLevelID();
      SubLevelSchematicSerializationContext schematicContext = SubLevelSchematicSerializationContext.getCurrentContext();
      if (id != null && schematicContext != null) {
         SchematicMapping mapping = schematicContext.getMapping(id);
         if (mapping != null) {
            id = mapping.newUUID();
            platePos = (BlockPos)mapping.transform().apply(platePos);
         } else {
            id = null;
            platePos = null;
         }
      }

      if (id != null) {
         compound.putUUID("SubLevelID", id);
      }

      if (platePos != null) {
         compound.put("SwivelPlate", NbtUtils.writeBlockPos(platePos));
      }

      if (this.sequencedAngleLimit >= 0.0) {
         compound.putDouble("SequencedAngleLimit", this.sequencedAngleLimit);
      }

      AssemblyException.write(compound, registries, this.lastException);
   }

   protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
      super.read(compound, registries, clientPacket);
      this.targetAngleDegrees = compound.getDouble("TargetAngle");
      SubLevelSchematicSerializationContext schematicContext = SubLevelSchematicSerializationContext.getCurrentContext();
      SchematicMapping mapping = null;
      if (compound.hasUUID("SubLevelID")) {
         UUID subLevelID = compound.getUUID("SubLevelID");
         if (schematicContext != null) {
            mapping = schematicContext.getMapping(subLevelID);
         }

         if (mapping != null) {
            subLevelID = mapping.newUUID();
         }

         this.setSubLevelID(subLevelID);
      }

      if (compound.contains("SwivelPlate")) {
         BlockPos blockPos = (BlockPos)NbtUtils.readBlockPos(compound, "SwivelPlate").orElseThrow();
         this.setPlatePos(blockPos);
      }

      this.sequencedAngleLimit = compound.contains("SequencedAngleLimit") ? compound.getDouble("SequencedAngleLimit") : -1.0;
      this.lastException = AssemblyException.read(compound, registries);
   }

   public void invalidate() {
      super.invalidate();
      this.removeHandle();
   }

   public void beforeAssembly() {
      this.assembling = true;
   }

   public void remove() {
      if (!this.level.isClientSide && !this.assembling) {
         this.destroyPlate();
      }

      super.remove();
   }

   public boolean isAssembled() {
      return (Boolean)this.getBlockState().getValue(SwivelBearingBlock.ASSEMBLED);
   }

   @Nullable
   private SubLevel getAttachedSubLevel() {
      SubLevelContainer container = SubLevelContainer.getContainer(this.level);
      return container.getSubLevel(this.subLevelID);
   }

   @Nullable
   private SubLevel getContainingSubLevel() {
      return Sable.HELPER.getContaining(this);
   }

   private boolean isLocking() {
      return (Boolean)this.getBlockState().getValue(BlockStateProperties.POWERED);
   }

   @NotNull
   private Vector3d getConstraintPos(BlockPos relative, BlockPos offset) {
      return JOMLConversion.toJOML(relative.offset(offset).getCenter());
   }

   private void destroyPlate() {
      BlockPos platePos = this.getPlatePos();
      if (platePos != null) {
         SubLevelContainer container = SubLevelContainer.getContainer(this.level);
         if (container == null) {
            return;
         }

         SubLevel subLevel = container.getSubLevel(this.subLevelID);
         if (this.subLevelID != null && subLevel == null) {
            return;
         }

         if (this.getLevel().getBlockState(platePos).is(SimBlocks.SWIVEL_BEARING_LINK_BLOCK)) {
            ((SwivelBearingPlateBlock)SimBlocks.SWIVEL_BEARING_LINK_BLOCK.get())
               .withBlockEntityDo(this.level, platePos, SwivelBearingPlateBlockEntity::beforeAssembly);
            this.getLevel().setBlock(platePos, Blocks.AIR.defaultBlockState(), 2);
         }
      }
   }

   private void removeHandle() {
      if (this.handle != null) {
         this.handle.remove();
         this.handle = null;
      }
   }

   public double getTargetAngleDegrees() {
      return this.targetAngleDegrees;
   }

   @NotNull
   @Override
   public KineticBlockEntity getExtraKinetics() {
      return this.cogwheel;
   }

   @Override
   public boolean shouldConnectExtraKinetics() {
      return false;
   }

   @Override
   public String getExtraKineticsSaveName() {
      return "SwivelCog";
   }

   public float propagateRotationTo(
      KineticBlockEntity target, BlockState stateFrom, BlockState stateTo, BlockPos diff, boolean connectedViaAxes, boolean connectedViaCogs
   ) {
      return this.getPlatePos() != null && stateTo.getBlock() instanceof SwivelBearingPlateBlock
         ? 1.0F
         : super.propagateRotationTo(target, stateFrom, stateTo, diff, connectedViaAxes, connectedViaCogs);
   }

   public boolean isCustomConnection(KineticBlockEntity other, BlockState state, BlockState otherState) {
      return this.getPlatePos() != null && otherState.getBlock() instanceof SwivelBearingPlateBlock;
   }

   public List<BlockPos> addPropagationLocations(IRotate block, BlockState state, List<BlockPos> neighbours) {
      if (this.getPlatePos() != null) {
         neighbours.add(this.getPlatePos());
      }

      return super.addPropagationLocations(block, state, neighbours);
   }

   @Nullable
   public BlockPos getPlatePos() {
      return this.swivelPlatePos;
   }

   public void setPlatePos(@Nullable BlockPos swivelPlatePos) {
      this.swivelPlatePos = swivelPlatePos;
   }

   @Nullable
   public UUID getSubLevelID() {
      return this.subLevelID;
   }

   public void setSubLevelID(@Nullable UUID subLevelID) {
      this.subLevelID = subLevelID;
   }

   public float calculateStressApplied() {
      return 0.0F;
   }

   public AssemblyException getLastAssemblyException() {
      return this.lastException;
   }

   @Nullable
   public Iterable<SubLevel> sable$getConnectionDependencies() {
      SubLevel attachedSubLevel = this.getAttachedSubLevel();
      return attachedSubLevel == null ? null : List.of(attachedSubLevel);
   }

   public static enum LockingSetting implements INamedIconOptions {
      LOCKED_ALWAYS(AllIcons.I_CONFIG_LOCKED, "swivel_default_always_locked"),
      LOCKED_DEFAULT(AllIcons.I_CONFIG_LOCKED, "swivel_default_locked"),
      UNLOCKED_DEFAULT(AllIcons.I_CONFIG_UNLOCKED, "swivel_default_unlocked"),
      UNLOCKED_ALWAYS(AllIcons.I_CONFIG_UNLOCKED, "swivel_default_always_unlocked");

      private final String translationKey;
      private final AllIcons icon;

      private LockingSetting(final AllIcons icon, final String name) {
         this.icon = icon;
         this.translationKey = "simulated.generic." + name;
      }

      public AllIcons getIcon() {
         return this.icon;
      }

      public String getTranslationKey() {
         return this.translationKey;
      }

      public boolean shouldLock(int signal) {
         if (this == UNLOCKED_ALWAYS) {
            return false;
         } else {
            return this == LOCKED_ALWAYS ? true : signal > 0 != (this == LOCKED_DEFAULT);
         }
      }
   }

   private static class SelectionModeValueBox extends CenteredSideValueBoxTransform {
      public SelectionModeValueBox(BiPredicate<BlockState, Direction> allowedDirections) {
         super(allowedDirections);
      }

      public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
         return super.getLocalOffset(level, pos, state)
            .subtract(Vec3.atLowerCornerOf(((Direction)state.getValue(SwivelBearingBlock.FACING)).getNormal()).scale(0.3125));
      }

      protected Vec3 getSouthLocation() {
         return VecHelper.voxelSpace(8.0, 8.0, 15.75);
      }

      public float getScale() {
         return 0.35F;
      }
   }

   public static class SwivelBearingCogwheelBlockEntity extends KineticBlockEntity implements ExtraKinetics.ExtraKineticsBlockEntity {
      public static final ICogWheel EXTRA_COGWHEEL_CONFIG = new ICogWheel() {
         public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
            return false;
         }

         public Axis getRotationAxis(BlockState state) {
            return ((Direction)state.getValue(SwivelBearingBlock.FACING)).getAxis();
         }
      };
      private final SwivelBearingBlockEntity parent;

      public SwivelBearingCogwheelBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state, SwivelBearingBlockEntity parent) {
         super(typeIn, new ExtraBlockPos(pos), state);
         this.parent = parent;
      }

      public void onSpeedChanged(float previousSpeed) {
         super.onSpeedChanged(previousSpeed);
         if ((double)this.speed != 0.0 && !this.parent.isAssembled()) {
            this.parent.assembleNextTick = true;
         }

         this.parent.sequencedAngleLimit = -1.0;
         if (this.sequenceContext != null && this.sequenceContext.instruction() == SequencerInstructions.TURN_ANGLE) {
            this.parent.sequencedAngleLimit = this.sequenceContext.getEffectiveValue((double)this.getTheoreticalSpeed());
         }
      }

      @Override
      public KineticBlockEntity getParentBlockEntity() {
         return this.parent;
      }

      protected void addStressImpactStats(List<Component> tooltip, float stressAtBase) {
         super.addStressImpactStats(tooltip, stressAtBase);
      }

      protected boolean canPropagateDiagonally(IRotate block, BlockState state) {
         return true;
      }

      @Override
      public Component getKey() {
         return SimLang.translate("extra_kinetics.extra_cogwheel").component();
      }
   }
}
