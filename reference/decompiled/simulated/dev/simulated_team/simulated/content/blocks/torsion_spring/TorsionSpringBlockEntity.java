package dev.simulated_team.simulated.content.blocks.torsion_spring;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.transmission.sequencer.SequencerInstructions;
import com.simibubi.create.content.kinetics.transmission.sequencer.SequencedGearshiftBlockEntity.SequenceContext;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform.Sided;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour.ValueSettings;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.infrastructure.config.AllConfigs;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.simulated_team.simulated.data.SimLang;
import dev.simulated_team.simulated.data.advancements.SimAdvancements;
import dev.simulated_team.simulated.mixin_interface.extra_kinetics.KineticBlockEntityExtension;
import dev.simulated_team.simulated.util.extra_kinetics.ExtraBlockPos;
import dev.simulated_team.simulated.util.extra_kinetics.ExtraKinetics;
import java.util.List;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class TorsionSpringBlockEntity extends KineticBlockEntity implements ExtraKinetics {
   private final TorsionSpringBlockEntity.Output springOutput;
   public ScrollValueBehaviour angleInput;
   protected double sequencedAngleLimit;

   public TorsionSpringBlockEntity(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
      super(blockEntityType, blockPos, blockState);
      this.springOutput = new TorsionSpringBlockEntity.Output(blockEntityType, new ExtraBlockPos(blockPos), blockState, this);
      this.sequencedAngleLimit = -1.0;
   }

   public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
      return super.addToGoggleTooltip(tooltip, isPlayerSneaking);
   }

   public boolean isSpringStatic() {
      return this.springOutput.angle == this.springOutput.oldAngle;
   }

   public float interpolatedSpring(float pt) {
      return (float)(this.springOutput.oldAngle + (this.springOutput.angle - this.springOutput.oldAngle) * (double)pt);
   }

   public float getAngle() {
      return (float)this.springOutput.angle;
   }

   public void setAngle(float angle) {
      this.springOutput.angle = (double)angle;
   }

   public void onSignalChanged() {
   }

   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      super.addBehaviours(behaviours);
      behaviours.add(this.angleInput = new TorsionSpringBlockEntity.TorsionSpringScrollValueBehaviour(this).between(1, 360));
      this.angleInput.onlyActiveWhen(this::showValue);
      this.angleInput.setValue(90);
   }

   public boolean showValue() {
      return true;
   }

   public void tick() {
      super.tick();
      this.springOutput.tick();
   }

   public void onSpeedChanged(float previousSpeed) {
      super.onSpeedChanged(previousSpeed);
      this.sequencedAngleLimit = -1.0;
      if (this.sequenceContext != null && this.sequenceContext.instruction() == SequencerInstructions.TURN_ANGLE) {
         this.sequencedAngleLimit = this.sequenceContext.getEffectiveValue((double)this.getTheoreticalSpeed());
      }

      this.springOutput.updateParentSpeed(previousSpeed, this.getSpeed());
   }

   protected void copySequenceContextFrom(KineticBlockEntity sourceBE) {
      super.copySequenceContextFrom(sourceBE);
   }

   public float calculateAddedStressCapacity() {
      return 0.0F;
   }

   @Override
   public String getExtraKineticsSaveName() {
      return "TorsionSpringOutput";
   }

   @Override
   public KineticBlockEntity getExtraKinetics() {
      return this.springOutput;
   }

   @Override
   public boolean shouldConnectExtraKinetics() {
      return false;
   }

   protected void write(CompoundTag compound, Provider registries, boolean clientPacket) {
      super.write(compound, registries, clientPacket);
      if (this.sequencedAngleLimit >= 0.0) {
         compound.putDouble("SequencedAngleLimit", this.sequencedAngleLimit);
      }
   }

   protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
      super.read(compound, registries, clientPacket);
      this.sequencedAngleLimit = compound.contains("SequencedAngleLimit") ? compound.getDouble("SequencedAngleLimit") : -1.0;
   }

   public static class Output extends GeneratingKineticBlockEntity implements ExtraKinetics.ExtraKineticsBlockEntity {
      public static final IRotate CONFIG = new IRotate() {
         public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
            return face == state.getValue(TorsionSpringBlock.FACING);
         }

         public Axis getRotationAxis(BlockState state) {
            return ((Direction)state.getValue(TorsionSpringBlock.FACING)).getAxis();
         }
      };
      private final TorsionSpringBlockEntity parent;
      protected double oldAngle = 0.0;
      protected double angle = 0.0;
      private int rotationDurationTicks = 0;
      private int rotationProgressTicks = 0;
      private double sequencedAngleLimit = -1.0;
      private float lastSpringSpeed = 0.0F;
      private float generatedSpeed;
      private double targetAngle = 0.0;
      private TorsionSpringBlockEntity.Output.State currentState = TorsionSpringBlockEntity.Output.State.STOPPED;
      private float queuedSpeed;
      private int customValidationCountdown;

      public Output(BlockEntityType<?> type, ExtraBlockPos pos, BlockState state, TorsionSpringBlockEntity parentBlockEntity) {
         super(type, pos, state);
         this.parent = parentBlockEntity;
      }

      @Override
      public Component getKey() {
         return SimLang.translate("extra_kinetics.torsion_output").component();
      }

      public void initialize() {
         super.initialize();
         this.reActivateSource = true;
         this.updateSpeed = true;
      }

      public void tick() {
         ((KineticBlockEntityExtension)this).simulated$setValidationCountdown(Integer.MAX_VALUE);
         if (this.customValidationCountdown-- <= 0) {
            this.customValidationCountdown = (Integer)AllConfigs.server().kinetics.kineticValidationFrequency.get();
            this.customValidateKinetics();
         }

         this.generatedSpeed = this.queuedSpeed;
         super.tick();
         this.oldAngle = this.angle;
         if (this.rotationDurationTicks >= 0 && this.rotationProgressTicks <= this.rotationDurationTicks) {
            this.rotationProgressTicks++;
            float angularSpeed = KineticBlockEntity.convertToAngular(this.speed);
            if (this.sequencedAngleLimit >= 0.0) {
               angularSpeed = (float)Mth.clamp((double)angularSpeed, -this.sequencedAngleLimit, this.sequencedAngleLimit);
            }

            if (this.sequencedAngleLimit >= 0.0) {
               this.sequencedAngleLimit = Math.max(0.0, this.sequencedAngleLimit - (double)Math.abs(angularSpeed));
            }

            this.angle += (double)angularSpeed;
            this.level.updateNeighborsAt(this.getBlockPos(), this.getParentBlockEntity().getBlockState().getBlock());
            if (this.rotationProgressTicks == this.rotationDurationTicks) {
               this.sequenceContext = null;
               this.rotationProgressTicks = -1;
               this.rotationDurationTicks = -1;
               this.queuedSpeed = 0.0F;
               this.reActivateSource = true;
               this.updateSpeed = true;
               this.currentState = TorsionSpringBlockEntity.Output.State.STOPPED;
            }
         }

         boolean powered = (Boolean)this.getBlockState().getValue(TorsionSpringBlock.POWERED);
         boolean parentStopped = this.parent.getSpeed() == 0.0F;
         if (this.currentState == TorsionSpringBlockEntity.Output.State.TURNING && parentStopped) {
            if (this.targetAngle != 0.0 || powered) {
               this.stopTurning();
            }
         } else if (this.currentState == TorsionSpringBlockEntity.Output.State.STOPPED && parentStopped && !powered) {
            if (this.targetAngle != 0.0) {
               this.beginTurnTo(0.0);
               SimAdvancements.REWIND_TIME.awardToNearby(this.parent.getBlockPos(), this.parent.getLevel());
            }
         } else if (this.currentState == TorsionSpringBlockEntity.Output.State.TURNING) {
            double targetAngle = (double)((float)this.parent.angleInput.getValue() * Math.signum(this.parent.getSpeed()));
            if (this.targetAngle != targetAngle || this.lastSpringSpeed != this.generatedSpeed) {
               this.stopTurning();
            }
         } else if (!parentStopped && this.currentState == TorsionSpringBlockEntity.Output.State.STOPPED) {
            double targetAngle = (double)((float)this.parent.angleInput.getValue() * Math.signum(this.parent.getSpeed()));
            this.beginTurnTo(targetAngle);
         }
      }

      private void customValidateKinetics() {
         if (this.hasSource()) {
            if (!this.hasNetwork()) {
               this.removeSource();
               return;
            }

            if (!this.level.isLoaded(this.source)) {
               return;
            }

            BlockEntity blockEntity = this.level.getBlockEntity(this.source);
            if (blockEntity instanceof ExtraKinetics ek && ((KineticBlockEntityExtension)this).simulated$getConnectedToExtraKinetics()) {
               blockEntity = ek.getExtraKinetics();
            }

            KineticBlockEntity sourceBE = blockEntity instanceof KineticBlockEntity ? (KineticBlockEntity)blockEntity : null;
            if (sourceBE == null || sourceBE.getTheoreticalSpeed() == 0.0F) {
               this.removeSource();
               this.detachKinetics();
            }
         }
      }

      private void updateParentSpeed(float previousSpeed, float newParentSpeed) {
         if (newParentSpeed != 0.0F) {
            this.lastSpringSpeed = newParentSpeed;
         } else if (previousSpeed != 0.0F) {
            this.lastSpringSpeed = previousSpeed;
         }
      }

      private void stopTurning() {
         this.sequenceContext = null;
         this.rotationProgressTicks = -1;
         this.rotationDurationTicks = -1;
         this.sequencedAngleLimit = -1.0;
         this.targetAngle = Double.MAX_VALUE;
         this.reActivateSource = true;
         this.updateSpeed = true;
         this.queuedSpeed = 0.0F;
         this.currentState = TorsionSpringBlockEntity.Output.State.STOPPED;
      }

      private void beginTurnTo(double targetAngle) {
         double relativeAngle = targetAngle - this.angle;
         if (relativeAngle != 0.0) {
            if (this.currentState != TorsionSpringBlockEntity.Output.State.TURNING || this.targetAngle != targetAngle) {
               this.lastSpringSpeed = (float)((double)Math.abs(this.lastSpringSpeed) * Math.signum(relativeAngle));
               if (this.parent.sequencedAngleLimit >= 0.0) {
                  relativeAngle = (double)((float)Mth.clamp(relativeAngle, -this.parent.sequencedAngleLimit, this.parent.sequencedAngleLimit));
               }

               this.detachKinetics();
               this.targetAngle = targetAngle;
               this.sequenceContext = new SequenceContext(SequencerInstructions.TURN_ANGLE, relativeAngle / (double)this.lastSpringSpeed);
               double degreesPerTick = (double)KineticBlockEntity.convertToAngular(Math.abs(this.lastSpringSpeed));
               this.rotationDurationTicks = (int)Math.ceil(Math.abs(relativeAngle) / degreesPerTick) + 2;
               this.rotationProgressTicks = 0;
               this.sequencedAngleLimit = this.sequenceContext.getEffectiveValue((double)this.lastSpringSpeed);
               this.currentState = TorsionSpringBlockEntity.Output.State.TURNING;
               this.queuedSpeed = this.lastSpringSpeed;
               this.generatedSpeed = this.queuedSpeed;
               this.reActivateSource = true;
               this.updateSpeed = true;
            }
         }
      }

      public float getGeneratedSpeed() {
         return this.generatedSpeed;
      }

      public float calculateStressApplied() {
         return 0.0F;
      }

      protected void write(CompoundTag compound, Provider registries, boolean clientPacket) {
         super.write(compound, registries, clientPacket);
         compound.putDouble("OldAngle", this.oldAngle);
         compound.putDouble("Angle", this.angle);
         compound.putDouble("TargetAngle", this.targetAngle);
         compound.putFloat("LastSpringSpeed", this.lastSpringSpeed);
         compound.putInt("CurrentState", this.currentState.ordinal());
         compound.putInt("RotationProgressTicks", this.rotationProgressTicks);
         compound.putInt("RotationDurationTicks", this.rotationDurationTicks);
         compound.putFloat("GeneratedSpeed", this.generatedSpeed);
         compound.putFloat("QueuedSpeed", this.queuedSpeed);
         if (this.sequencedAngleLimit >= 0.0) {
            compound.putDouble("SequencedAngleLimit", this.sequencedAngleLimit);
         }
      }

      protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
         super.read(compound, registries, clientPacket);
         this.oldAngle = compound.getDouble("OldAngle");
         this.angle = compound.getDouble("Angle");
         this.targetAngle = compound.getDouble("TargetAngle");
         this.lastSpringSpeed = compound.getFloat("LastSpringSpeed");
         this.sequencedAngleLimit = compound.contains("SequencedAngleLimit") ? compound.getDouble("SequencedAngleLimit") : -1.0;
         this.rotationProgressTicks = compound.getInt("RotationProgressTicks");
         this.rotationDurationTicks = compound.getInt("RotationDurationTicks");
         this.generatedSpeed = compound.getFloat("GeneratedSpeed");
         this.queuedSpeed = compound.getFloat("QueuedSpeed");
         if (compound.contains("CurrentState")) {
            this.currentState = TorsionSpringBlockEntity.Output.State.values()[compound.getInt("CurrentState")];
         }
      }

      @Override
      public KineticBlockEntity getParentBlockEntity() {
         return this.parent;
      }

      private static enum State {
         STOPPED,
         TURNING;
      }
   }

   public static class TorsionSpringScrollValueBehaviour extends ScrollValueBehaviour {
      public TorsionSpringScrollValueBehaviour(SmartBlockEntity be) {
         super(SimLang.translate("torsion_spring.angle_limit").component(), be, new TorsionSpringBlockEntity.TorsionSpringValueBox());
         this.withFormatter(v -> Math.max(1, v) + CreateLang.translateDirect("generic.unit.degrees", new Object[0]).getString());
      }

      public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
         return new ValueSettingsBoard(
            this.label, 360, 45, ImmutableList.of(Component.literal("⟳").withStyle(ChatFormatting.BOLD)), new ValueSettingsFormatter(this::formatValue)
         );
      }

      public MutableComponent formatValue(ValueSettings settings) {
         return SimLang.number((double)Math.max(1, settings.value())).add(CreateLang.translateDirect("generic.unit.degrees", new Object[0])).component();
      }
   }

   public static class TorsionSpringValueBox extends Sided {
      protected Vec3 getSouthLocation() {
         return VecHelper.voxelSpace(8.0, 8.0, 15.5);
      }

      public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
         return super.getLocalOffset(level, pos, state)
            .add(Vec3.atLowerCornerOf(((Direction)state.getValue(TorsionSpringBlock.FACING)).getNormal()).scale(-0.3125));
      }

      public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
         if (!this.getSide().getAxis().isHorizontal()) {
            TransformStack.of(ms)
               .rotateY((AngleHelper.horizontalAngle((Direction)state.getValue(TorsionSpringBlock.FACING)) + 180.0F) * (float) Math.PI / 180.0F);
         }

         super.rotate(level, pos, state, ms);
      }

      public boolean testHit(LevelAccessor level, BlockPos pos, BlockState state, Vec3 localHit) {
         Vec3 offset = this.getLocalOffset(level, pos, state);
         return offset == null ? false : localHit.distanceTo(offset) < (double)(this.scale / 1.5F);
      }

      protected boolean isSideActive(BlockState state, Direction direction) {
         return direction.getAxis() != ((Direction)state.getValue(TorsionSpringBlock.FACING)).getAxis();
      }
   }
}
