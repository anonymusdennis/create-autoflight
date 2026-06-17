package dev.simulated_team.simulated.content.blocks.velocity_sensor;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform.Sided;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour.ValueSettings;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.content.blocks.util.AbstractDirectionalAxisBlock;
import dev.simulated_team.simulated.data.SimLang;
import java.lang.ref.WeakReference;
import java.util.List;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class VelocitySensorBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
   private float adjustedVelocity;
   private int signedRedstoneStrength;
   private Vector3dc currentNormal;
   private WeakReference<SubLevel> subLevelReference;
   private VelocitySensorBlockEntity.VelocitySensorScrollValueBehaviour maxSpeed;
   private final LerpedFloat fanSpeed = LerpedFloat.linear().chase(0.0, 0.5, Chaser.EXP);
   private float fanAngle = 0.0F;
   private float oldFanAngle = 0.0F;

   public VelocitySensorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
      this.subLevelReference = new WeakReference<>(null);
      this.adjustedVelocity = 0.0F;
      this.signedRedstoneStrength = 0;
      this.currentNormal = new Vector3d();
   }

   public void addBehaviours(List<BlockEntityBehaviour> list) {
      this.maxSpeed = new VelocitySensorBlockEntity.VelocitySensorScrollValueBehaviour(
         SimLang.translate("velocity_sensor.description").component(), this, new VelocitySensorBlockEntity.VelocitySensorValueBoxTransform()
      );
      this.maxSpeed.between(1, 50);
      this.maxSpeed.value = 10;
      this.maxSpeed.withFormatter(value -> value + " m/s");
      list.add(this.maxSpeed);
   }

   public void initialize() {
      super.initialize();
      this.subLevelReference = new WeakReference<>(Sable.HELPER.getContaining(this.getLevel(), this.worldPosition));
   }

   public void tick() {
      this.currentNormal = JOMLConversion.toJOML(Vec3.atLowerCornerOf(AbstractDirectionalAxisBlock.getDirectionOfAxis(this.getBlockState()).getNormal()));
      super.tick();
      SubLevel subLevel = this.subLevelReference.get();
      int redstoneStrengthBefore = this.signedRedstoneStrength;
      if (!this.level.isClientSide) {
         if (subLevel != null) {
            float dot = (float)this.getGlobalVelocity().dot(subLevel.logicalPose().transformNormal(this.currentNormal, new Vector3d()));
            if ((double)Math.abs(dot) > 0.05) {
               this.adjustedVelocity = dot;
            } else {
               this.adjustedVelocity = 0.0F;
            }

            this.signedRedstoneStrength = (int)Math.clamp(this.getAdjustedVelocity() / (float)this.maxSpeed.getValue() * 15.0F, -15.0F, 15.0F);
         } else {
            this.adjustedVelocity = 0.0F;
            this.signedRedstoneStrength = 0;
         }

         if (redstoneStrengthBefore != this.signedRedstoneStrength) {
            int power;
            if (this.signedRedstoneStrength == 0) {
               power = 0;
            } else if (this.signedRedstoneStrength < 0) {
               power = 1;
            } else {
               power = 2;
            }

            this.level.setBlockAndUpdate(this.worldPosition, (BlockState)this.getBlockState().setValue(VelocitySensorBlock.POWERED, power));
            Direction axisDir = AbstractDirectionalAxisBlock.getDirectionOfAxis(this.getBlockState());
            this.level.updateNeighborsAt(this.worldPosition, this.getBlockState().getBlock());
            this.level.updateNeighborsAt(this.worldPosition.relative(axisDir), this.getBlockState().getBlock());
            this.level.updateNeighborsAt(this.worldPosition.relative(axisDir.getOpposite()), this.getBlockState().getBlock());
         }

         this.sendData();
      } else {
         this.fanSpeed.updateChaseTarget(Mth.clamp(this.getAdjustedVelocity() / (float)this.maxSpeed.getValue(), -1.0F, 1.0F));
         this.fanSpeed.tickChaser();
         this.oldFanAngle = this.fanAngle;
         this.fanAngle = this.fanAngle + this.fanSpeed.getValue();
      }
   }

   public ScrollValueBehaviour getMaxSpeed() {
      return this.maxSpeed;
   }

   public float getFanAngle(float pt) {
      return Mth.lerp(pt, this.oldFanAngle, this.fanAngle);
   }

   private Vector3d getGlobalVelocity() {
      SubLevel subLevel = this.subLevelReference.get();
      if (subLevel == null) {
         return new Vector3d();
      } else {
         Vector3d jomlPos = JOMLConversion.toJOML(this.worldPosition.getCenter());
         return subLevel.logicalPose()
            .transformPosition(jomlPos, new Vector3d())
            .sub(subLevel.lastPose().transformPosition(jomlPos, new Vector3d()), jomlPos)
            .mul(20.0);
      }
   }

   protected void write(CompoundTag tag, Provider registries, boolean clientPacket) {
      super.write(tag, registries, clientPacket);
      tag.putFloat("AdjustedVelocity", this.getAdjustedVelocity());
      tag.putInt("SignedRedstoneStrength", this.signedRedstoneStrength);
   }

   protected void read(CompoundTag tag, Provider registries, boolean clientPacket) {
      super.read(tag, registries, clientPacket);
      this.adjustedVelocity = tag.getFloat("AdjustedVelocity");
      this.signedRedstoneStrength = Mth.clamp(-15, 15, tag.getInt("SignedRedstoneStrength"));
   }

   public Vector3dc getCurrentNormal() {
      return this.currentNormal;
   }

   public int getRedstoneStrength() {
      return Mth.abs(this.signedRedstoneStrength);
   }

   public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
      if (this.subLevelReference.get() != null) {
         SimLang.number((double)Math.abs(this.getAdjustedVelocity())).text(" m/s").forGoggles(tooltip);
      }

      return super.addToGoggleTooltip(tooltip, isPlayerSneaking);
   }

   public float getAdjustedVelocity() {
      return this.adjustedVelocity;
   }

   public static class VelocitySensorScrollValueBehaviour extends ScrollValueBehaviour {
      private boolean towards;

      public VelocitySensorScrollValueBehaviour(Component label, SmartBlockEntity be, ValueBoxTransform slot) {
         super(label, be, slot);
      }

      public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
         ImmutableList<Component> rows = ImmutableList.of(
            SimLang.translate("velocity_sensor.selection.away").component(), SimLang.translate("velocity_sensor.selection.towards").component()
         );
         return new ValueSettingsBoard(this.label, this.max, 10, rows, new ValueSettingsFormatter(this::formatValue));
      }

      public MutableComponent formatValue(ValueSettings settings) {
         return SimLang.number((double)settings.value()).component().append(" m/s");
      }

      public void setValueSettings(Player player, ValueSettings valueSetting, boolean ctrlDown) {
         super.setValueSettings(player, valueSetting, ctrlDown);
         this.towards = valueSetting.row() == 1;
      }

      public int getValue() {
         return super.getValue() * (this.towards ? 1 : -1);
      }

      public ValueSettings getValueSettings() {
         return new ValueSettings(this.towards ? 1 : 0, this.value);
      }

      public boolean isTowards() {
         return this.towards;
      }

      public void read(CompoundTag nbt, Provider registries, boolean clientPacket) {
         this.towards = nbt.getBoolean("ScrollValueTowards");
         super.read(nbt, registries, clientPacket);
      }

      public void write(CompoundTag nbt, Provider registries, boolean clientPacket) {
         nbt.putBoolean("ScrollValueTowards", this.towards);
         super.write(nbt, registries, clientPacket);
      }
   }

   private static class VelocitySensorValueBoxTransform extends Sided {
      protected Vec3 getSouthLocation() {
         return VecHelper.voxelSpace(8.0, 8.0, 12.8);
      }

      public float getScale() {
         return 0.35F;
      }

      protected boolean isSideActive(BlockState state, Direction direction) {
         return AbstractDirectionalAxisBlock.getAxis(state) == direction.getAxis();
      }
   }
}
