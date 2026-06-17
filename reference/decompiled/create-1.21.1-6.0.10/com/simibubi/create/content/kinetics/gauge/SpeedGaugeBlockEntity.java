package com.simibubi.create.content.kinetics.gauge;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.compat.Mods;
import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;
import com.simibubi.create.compat.computercraft.ComputerCraftProxy;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.infrastructure.config.AllConfigs;
import dan200.computercraft.api.peripheral.PeripheralCapability;
import java.util.List;
import net.createmod.catnip.theme.Color;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public class SpeedGaugeBlockEntity extends GaugeBlockEntity {
   public AbstractComputerBehaviour computerBehaviour;

   public SpeedGaugeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      super.addBehaviours(behaviours);
      behaviours.add(this.computerBehaviour = ComputerCraftProxy.behaviour(this));
   }

   public static void registerCapabilities(RegisterCapabilitiesEvent event) {
      if (Mods.COMPUTERCRAFT.isLoaded()) {
         event.registerBlockEntity(
            PeripheralCapability.get(), (BlockEntityType)AllBlockEntityTypes.SPEEDOMETER.get(), (be, context) -> be.computerBehaviour.getPeripheralCapability()
         );
      }
   }

   @Override
   public void onSpeedChanged(float prevSpeed) {
      super.onSpeedChanged(prevSpeed);
      if (this.computerBehaviour.hasAttachedComputer()) {
         this.computerBehaviour.prepareComputerEvent(this.makeComputerKineticsChangeEvent());
      }

      float speed = Math.abs(this.getSpeed());
      this.dialTarget = getDialTarget(speed);
      this.color = Color.mixColors(IRotate.SpeedLevel.of(speed).getColor(), 16777215, 0.25F);
      this.setChanged();
   }

   public static float getDialTarget(float speed) {
      speed = Math.abs(speed);
      float medium = ((Double)AllConfigs.server().kinetics.mediumSpeed.get()).floatValue();
      float fast = ((Double)AllConfigs.server().kinetics.fastSpeed.get()).floatValue();
      float max = ((Integer)AllConfigs.server().kinetics.maxRotationSpeed.get()).floatValue();
      float target = 0.0F;
      if (speed == 0.0F) {
         target = 0.0F;
      } else if (speed < medium) {
         target = Mth.lerp(speed / medium, 0.0F, 0.45F);
      } else if (speed < fast) {
         target = Mth.lerp((speed - medium) / (fast - medium), 0.45F, 0.75F);
      } else {
         target = Mth.lerp((speed - fast) / (max - fast), 0.75F, 1.125F);
      }

      return target;
   }

   @Override
   public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
      super.addToGoggleTooltip(tooltip, isPlayerSneaking);
      CreateLang.translate("gui.speedometer.title").style(ChatFormatting.GRAY).forGoggles(tooltip);
      IRotate.SpeedLevel.getFormattedSpeedText(this.speed, this.isOverStressed()).forGoggles(tooltip);
      return true;
   }

   @Override
   public void invalidate() {
      super.invalidate();
      this.computerBehaviour.removePeripheral();
   }
}
