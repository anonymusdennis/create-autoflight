package com.simibubi.create.content.kinetics.gauge;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.compat.Mods;
import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;
import com.simibubi.create.compat.computercraft.ComputerCraftProxy;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.advancement.CreateAdvancement;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.utility.CreateLang;
import dan200.computercraft.api.peripheral.PeripheralCapability;
import java.util.List;
import net.createmod.catnip.lang.LangBuilder;
import net.createmod.catnip.platform.CatnipServices;
import net.createmod.catnip.theme.Color;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public class StressGaugeBlockEntity extends GaugeBlockEntity {
   public AbstractComputerBehaviour computerBehaviour;
   static BlockPos lastSent;

   public StressGaugeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   public static void registerCapabilities(RegisterCapabilitiesEvent event) {
      if (Mods.COMPUTERCRAFT.isLoaded()) {
         event.registerBlockEntity(
            PeripheralCapability.get(),
            (BlockEntityType)AllBlockEntityTypes.STRESSOMETER.get(),
            (be, context) -> be.computerBehaviour.getPeripheralCapability()
         );
      }
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      super.addBehaviours(behaviours);
      behaviours.add(this.computerBehaviour = ComputerCraftProxy.behaviour(this));
      this.registerAwardables(behaviours, new CreateAdvancement[]{AllAdvancements.STRESSOMETER, AllAdvancements.STRESSOMETER_MAXED});
   }

   @Override
   public void updateFromNetwork(float maxStress, float currentStress, int networkSize) {
      super.updateFromNetwork(maxStress, currentStress, networkSize);
      if (this.computerBehaviour.hasAttachedComputer()) {
         this.computerBehaviour.prepareComputerEvent(this.makeComputerKineticsChangeEvent());
      }

      if (!IRotate.StressImpact.isEnabled()) {
         this.dialTarget = 0.0F;
      } else if (this.isOverStressed()) {
         this.dialTarget = 1.125F;
      } else if (maxStress == 0.0F) {
         this.dialTarget = 0.0F;
      } else {
         this.dialTarget = currentStress / maxStress;
      }

      if (this.dialTarget > 0.0F) {
         if (this.dialTarget < 0.5F) {
            this.color = Color.mixColors(65280, 16776960, this.dialTarget * 2.0F);
         } else if (this.dialTarget < 1.0F) {
            this.color = Color.mixColors(16776960, 16711680, this.dialTarget * 2.0F - 1.0F);
         } else {
            this.color = 16711680;
         }
      }

      this.sendData();
      this.setChanged();
   }

   @Override
   public void onSpeedChanged(float prevSpeed) {
      super.onSpeedChanged(prevSpeed);
      if (this.getSpeed() == 0.0F) {
         this.dialTarget = 0.0F;
         this.setChanged();
      } else {
         this.updateFromNetwork(this.capacity, this.stress, this.getOrCreateNetwork().getSize());
      }
   }

   @Override
   public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
      if (!IRotate.StressImpact.isEnabled()) {
         return false;
      } else {
         super.addToGoggleTooltip(tooltip, isPlayerSneaking);
         double capacity = (double)this.getNetworkCapacity();
         double stressFraction = (double)this.getNetworkStress() / (capacity == 0.0 ? 1.0 : capacity);
         CreateLang.translate("gui.stressometer.title").style(ChatFormatting.GRAY).forGoggles(tooltip);
         if (this.getTheoreticalSpeed() == 0.0F) {
            CreateLang.text(TooltipHelper.makeProgressBar(3, 0))
               .translate("gui.stressometer.no_rotation", new Object[0])
               .style(ChatFormatting.DARK_GRAY)
               .forGoggles(tooltip);
         } else {
            IRotate.StressImpact.getFormattedStressText(stressFraction).forGoggles(tooltip);
            CreateLang.translate("gui.stressometer.capacity").style(ChatFormatting.GRAY).forGoggles(tooltip);
            double remainingCapacity = capacity - (double)this.getNetworkStress();
            LangBuilder su = CreateLang.translate("generic.unit.stress");
            LangBuilder stressTip = CreateLang.number(remainingCapacity).add(su).style(IRotate.StressImpact.of(stressFraction).getRelativeColor());
            if (remainingCapacity != capacity) {
               stressTip.text(ChatFormatting.GRAY, " / ").add(CreateLang.number(capacity).add(su).style(ChatFormatting.DARK_GRAY));
            }

            stressTip.forGoggles(tooltip, 1);
         }

         if (!this.worldPosition.equals(lastSent)) {
            CatnipServices.NETWORK.sendToServer(new GaugeObservedPacket(lastSent = this.worldPosition));
         }

         return true;
      }
   }

   @Override
   protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
      super.read(compound, registries, clientPacket);
      if (clientPacket && this.worldPosition != null && this.worldPosition.equals(lastSent)) {
         lastSent = null;
      }
   }

   public float getNetworkStress() {
      return this.stress;
   }

   public float getNetworkCapacity() {
      return this.capacity;
   }

   public void onObserved() {
      this.award(AllAdvancements.STRESSOMETER);
      if (Mth.equal(this.dialTarget, 1.0F)) {
         this.award(AllAdvancements.STRESSOMETER_MAXED);
      }
   }

   @Override
   public void invalidate() {
      super.invalidate();
      this.computerBehaviour.removePeripheral();
   }
}
