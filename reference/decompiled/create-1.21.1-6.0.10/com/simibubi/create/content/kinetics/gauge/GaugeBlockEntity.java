package com.simibubi.create.content.kinetics.gauge;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.utility.CreateLang;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class GaugeBlockEntity extends KineticBlockEntity implements IHaveGoggleInformation {
   public float dialTarget;
   public float dialState;
   public float prevDialState;
   public int color;

   public GaugeBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
      super(typeIn, pos, state);
   }

   @Override
   public void write(CompoundTag compound, Provider registries, boolean clientPacket) {
      compound.putFloat("Value", this.dialTarget);
      compound.putInt("Color", this.color);
      super.write(compound, registries, clientPacket);
   }

   @Override
   protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
      this.dialTarget = compound.getFloat("Value");
      this.color = compound.getInt("Color");
      super.read(compound, registries, clientPacket);
   }

   @Override
   public void tick() {
      super.tick();
      this.prevDialState = this.dialState;
      this.dialState = this.dialState + (this.dialTarget - this.dialState) * 0.125F;
      if (this.dialState > 1.0F && this.level.random.nextFloat() < 0.5F) {
         this.dialState = this.dialState - (this.dialState - 1.0F) * this.level.random.nextFloat();
      }
   }

   @Override
   public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
      CreateLang.translate("gui.gauge.info_header").forGoggles(tooltip);
      return true;
   }
}
