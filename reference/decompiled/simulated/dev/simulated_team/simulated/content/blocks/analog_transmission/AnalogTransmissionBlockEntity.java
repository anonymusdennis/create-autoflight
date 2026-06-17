package dev.simulated_team.simulated.content.blocks.analog_transmission;

import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.infrastructure.config.AllConfigs;
import dev.simulated_team.simulated.data.SimLang;
import dev.simulated_team.simulated.mixin_interface.extra_kinetics.KineticBlockEntityExtension;
import dev.simulated_team.simulated.util.extra_kinetics.ExtraBlockPos;
import dev.simulated_team.simulated.util.extra_kinetics.ExtraKinetics;
import java.util.List;
import net.createmod.catnip.lang.FontHelper.Palette;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

public class AnalogTransmissionBlockEntity extends KineticBlockEntity implements ExtraKinetics {
   private final AnalogTransmissionBlockEntity.AnalogTransmissionCogwheel extraWheel;
   private int signal = 0;
   private boolean oversaturated = false;
   boolean alreadySentEffects = false;

   public AnalogTransmissionBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
      super(typeIn, pos, state);
      this.extraWheel = new AnalogTransmissionBlockEntity.AnalogTransmissionCogwheel(typeIn, new ExtraBlockPos(pos), state, this);
   }

   public void tick() {
      int bestNeighborSignal = this.getLevel().getBestNeighborSignal(this.getBlockPos());
      if (!this.getLevel().isClientSide) {
         if (bestNeighborSignal != this.signal) {
            this.detachKinetics();
            this.extraWheel.detachKinetics();
            this.removeSource();
            this.extraWheel.removeSource();
            this.signal = bestNeighborSignal;
            this.getLevel().setBlockAndUpdate(this.getBlockPos(), (BlockState)this.getBlockState().setValue(AnalogTransmissionBlock.POWERED, this.signal > 0));
            if (((KineticBlockEntityExtension)this).simulated$getConnectedToExtraKinetics()) {
               this.attachKinetics();
               this.extraWheel.attachKinetics();
            } else {
               this.extraWheel.attachKinetics();
               this.attachKinetics();
            }
         }
      } else if (this.oversaturated) {
         if (!this.alreadySentEffects) {
            this.alreadySentEffects = true;
            this.effects.triggerOverStressedEffect();
         }
      } else {
         this.alreadySentEffects = false;
      }

      this.extraWheel.tick();
      super.tick();
   }

   @VisibleForTesting
   public float getRotationModifier() {
      return 1.0F - (float)(this.signal + 1) / 16.0F;
   }

   public float propagateRotationTo(
      KineticBlockEntity target, BlockState stateFrom, BlockState stateTo, BlockPos diff, boolean connectedViaAxes, boolean connectedViaCogs
   ) {
      float gatheredRotationModifier = 0.0F;
      if (this.signal != 15) {
         if (target == this.extraWheel) {
            gatheredRotationModifier = this.signal == 0 ? 1.0F : this.getRotationModifier();
            if (this.oversaturated) {
               return 0.0F;
            }
         } else if (target == this) {
            gatheredRotationModifier = this.signal == 0 ? 1.0F : 1.0F / this.getRotationModifier();
            if (Math.abs(this.extraWheel.getTheoreticalSpeed() * gatheredRotationModifier)
               > (float)((Integer)AllConfigs.server().kinetics.maxRotationSpeed.get()).intValue()) {
               this.oversaturated = true;
               return 0.0F;
            }

            this.oversaturated = false;
         }
      } else {
         this.oversaturated = false;
      }

      return gatheredRotationModifier;
   }

   protected void write(CompoundTag compound, Provider registries, boolean clientPacket) {
      super.write(compound, registries, clientPacket);
      compound.putInt("Signal", this.signal);
      compound.putBoolean("Oversaturated", this.oversaturated);
   }

   protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
      super.read(compound, registries, clientPacket);
      this.signal = compound.getInt("Signal");
      this.oversaturated = compound.getBoolean("Oversaturated");
   }

   public boolean isOverStressed() {
      return !this.level.isClientSide ? super.isOverStressed() : this.oversaturated || this.overStressed;
   }

   @NotNull
   @Override
   public KineticBlockEntity getExtraKinetics() {
      return this.extraWheel;
   }

   @Override
   public boolean shouldConnectExtraKinetics() {
      return true;
   }

   @Override
   public String getExtraKineticsSaveName() {
      return "ExtraCogwheel";
   }

   public boolean addToTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
      if (this.oversaturated) {
         SimLang.translate("analog_transmission.too_fast").style(ChatFormatting.GOLD).forGoggles(tooltip);
         MutableComponent component = SimLang.translate("analog_transmission.too_fast_error").component();
         List<Component> cutString = TooltipHelper.cutTextComponent(component, Palette.GRAY_AND_WHITE);
         tooltip.addAll(cutString);
         return true;
      } else {
         return super.addToTooltip(tooltip, isPlayerSneaking);
      }
   }

   public static class AnalogTransmissionCogwheel extends KineticBlockEntity implements ExtraKinetics.ExtraKineticsBlockEntity {
      public static final ICogWheel EXTRA_COGWHEEL_CONFIG = new ICogWheel() {
         public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
            return false;
         }

         public Axis getRotationAxis(BlockState state) {
            return (Axis)state.getValue(AnalogTransmissionBlock.AXIS);
         }
      };
      private final KineticBlockEntity parentBlockEntity;

      public AnalogTransmissionCogwheel(BlockEntityType<?> typeIn, ExtraBlockPos pos, BlockState state, KineticBlockEntity parentBlockEntity) {
         super(typeIn, pos, state);
         this.parentBlockEntity = parentBlockEntity;
      }

      public float propagateRotationTo(
         KineticBlockEntity target, BlockState stateFrom, BlockState stateTo, BlockPos diff, boolean connectedViaAxes, boolean connectedViaCogs
      ) {
         return this.parentBlockEntity.propagateRotationTo(target, stateFrom, stateTo, diff, connectedViaAxes, connectedViaCogs);
      }

      protected boolean canPropagateDiagonally(IRotate block, BlockState state) {
         return true;
      }

      @Override
      public KineticBlockEntity getParentBlockEntity() {
         return this.parentBlockEntity;
      }
   }
}
