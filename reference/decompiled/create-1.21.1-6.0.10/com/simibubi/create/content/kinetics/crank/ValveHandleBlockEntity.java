package com.simibubi.create.content.kinetics.crank;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.transmission.sequencer.SequencedGearshiftBlockEntity;
import com.simibubi.create.content.kinetics.transmission.sequencer.SequencerInstructions;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import java.util.List;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class ValveHandleBlockEntity extends HandCrankBlockEntity {
   public ScrollValueBehaviour angleInput;
   public int cooldown;
   protected int startAngle;
   protected int targetAngle;
   protected int totalUseTicks;

   public ValveHandleBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      super.addBehaviours(behaviours);
      behaviours.add(this.angleInput = new ValveHandleBlockEntity.ValveHandleScrollValueBehaviour(this).between(-180, 180));
      this.angleInput.onlyActiveWhen(this::showValue);
      this.angleInput.setValue(45);
   }

   @Override
   protected boolean clockwise() {
      return this.angleInput.getValue() < 0 ^ this.backwards;
   }

   @Override
   public void write(CompoundTag compound, Provider registries, boolean clientPacket) {
      super.write(compound, registries, clientPacket);
      compound.putInt("TotalUseTicks", this.totalUseTicks);
      compound.putInt("StartAngle", this.startAngle);
      compound.putInt("TargetAngle", this.targetAngle);
   }

   @Override
   protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
      super.read(compound, registries, clientPacket);
      this.totalUseTicks = compound.getInt("TotalUseTicks");
      this.startAngle = compound.getInt("StartAngle");
      this.targetAngle = compound.getInt("TargetAngle");
   }

   @Override
   public void tick() {
      super.tick();
      if (this.inUse == 0 && this.cooldown > 0) {
         this.cooldown--;
      }

      this.independentAngle = this.level.isClientSide() ? this.getIndependentAngle(0.0F) : 0.0F;
   }

   @Override
   public float getIndependentAngle(float partialTicks) {
      if (this.inUse == 0 && this.source != null && this.getSpeed() != 0.0F) {
         return AngleHelper.deg((double)KineticBlockEntityRenderer.getAngleForBe(this, this.worldPosition, KineticBlockEntityRenderer.getRotationAxisOf(this)));
      } else {
         int step = this.getBlockState().getOptionalValue(ValveHandleBlock.FACING).orElse(Direction.SOUTH).getAxisDirection().getStep();
         return (
               this.inUse > 0 && this.totalUseTicks > 0
                  ? Mth.lerp(
                     Math.min((float)this.totalUseTicks, (float)(this.totalUseTicks - this.inUse) + partialTicks) / (float)this.totalUseTicks,
                     (float)this.startAngle,
                     (float)this.targetAngle
                  )
                  : (float)this.targetAngle
            )
            * (float)(this.backwards ? -1 : 1)
            * (float)step;
      }
   }

   public boolean showValue() {
      return this.inUse == 0;
   }

   public boolean activate(boolean sneak) {
      if (this.getTheoreticalSpeed() != 0.0F) {
         return false;
      } else if (this.inUse > 0 || this.cooldown > 0) {
         return false;
      } else if (this.level.isClientSide) {
         return true;
      } else {
         int value = this.angleInput.getValue();
         int target = Math.abs(value);
         int rotationSpeed = ((ValveHandleBlock)AllBlocks.COPPER_VALVE_HANDLE.get()).getRotationSpeed();
         double degreesPerTick = (double)KineticBlockEntity.convertToAngular((float)rotationSpeed);
         this.inUse = (int)Math.ceil((double)target / degreesPerTick) + 2;
         this.startAngle = (int)(this.independentAngle % 90.0F + 360.0F) % 90;
         this.targetAngle = Math.round((float)(this.startAngle + (target > 135 ? 180 : 90) * Mth.sign((double)value)) / 90.0F) * 90;
         this.totalUseTicks = this.inUse;
         this.backwards = sneak;
         this.sequenceContext = SequencedGearshiftBlockEntity.SequenceContext.fromGearshift(SequencerInstructions.TURN_ANGLE, (double)rotationSpeed, target);
         this.updateGeneratedRotation();
         this.cooldown = 4;
         return true;
      }
   }

   @Override
   protected void copySequenceContextFrom(KineticBlockEntity sourceBE) {
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public SuperByteBuffer getRenderedHandle() {
      return CachedBuffers.block(this.getBlockState());
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public boolean shouldRenderShaft() {
      return false;
   }

   public static class ValveHandleScrollValueBehaviour extends ScrollValueBehaviour {
      public ValveHandleScrollValueBehaviour(SmartBlockEntity be) {
         super(CreateLang.translateDirect("kinetics.valve_handle.rotated_angle"), be, new ValveHandleBlockEntity.ValveHandleValueBox());
         this.withFormatter(v -> Math.abs(v) + CreateLang.translateDirect("generic.unit.degrees").getString());
      }

      @Override
      public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
         ImmutableList<Component> rows = ImmutableList.of(
            Component.literal("⟳").withStyle(ChatFormatting.BOLD), Component.literal("⟲").withStyle(ChatFormatting.BOLD)
         );
         return new ValueSettingsBoard(this.label, 180, 45, rows, new ValueSettingsFormatter(this::formatValue));
      }

      @Override
      public void setValueSettings(Player player, ValueSettingsBehaviour.ValueSettings valueSetting, boolean ctrlHeld) {
         int value = Math.max(1, valueSetting.value());
         if (!valueSetting.equals(this.getValueSettings())) {
            this.playFeedbackSound(this);
         }

         this.setValue(valueSetting.row() == 0 ? -value : value);
      }

      @Override
      public ValueSettingsBehaviour.ValueSettings getValueSettings() {
         return new ValueSettingsBehaviour.ValueSettings(this.value < 0 ? 0 : 1, Math.abs(this.value));
      }

      public MutableComponent formatValue(ValueSettingsBehaviour.ValueSettings settings) {
         return CreateLang.number((double)Math.max(1, Math.abs(settings.value()))).add(CreateLang.translateDirect("generic.unit.degrees")).component();
      }

      @Override
      public void onShortInteract(Player player, InteractionHand hand, Direction side, BlockHitResult hitResult) {
         if (!this.getWorld().isClientSide) {
            BlockState blockState = this.blockEntity.getBlockState();
            if (blockState.getBlock() instanceof ValveHandleBlock vhb) {
               vhb.clicked(this.getWorld(), this.getPos(), blockState, player, hand);
            }
         }
      }
   }

   public static class ValveHandleValueBox extends ValueBoxTransform.Sided {
      @Override
      protected boolean isSideActive(BlockState state, Direction direction) {
         return direction == state.getValue(ValveHandleBlock.FACING);
      }

      @Override
      protected Vec3 getSouthLocation() {
         return VecHelper.voxelSpace(8.0, 8.0, 4.5);
      }

      @Override
      public boolean testHit(LevelAccessor level, BlockPos pos, BlockState state, Vec3 localHit) {
         Vec3 offset = this.getLocalOffset(level, pos, state);
         return offset == null ? false : localHit.distanceTo(offset) < (double)(this.scale / 1.5F);
      }
   }
}
