package com.simibubi.create.content.equipment.armor;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.advancement.CreateAdvancement;
import com.simibubi.create.foundation.blockEntity.ComparatorUtil;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.particle.AirParticleData;
import java.util.List;
import net.createmod.catnip.codecs.CatnipCodecUtils;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentMap.Builder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Component.Serializer;
import net.minecraft.util.Mth;
import net.minecraft.world.Nameable;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntity.DataComponentInput;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.Vec3;

public class BacktankBlockEntity extends KineticBlockEntity implements Nameable {
   public int airLevel;
   public int airLevelTimer;
   private Component defaultName;
   private Component customName;
   private int capacityEnchantLevel;
   private DataComponentPatch componentPatch;

   public BacktankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
      this.defaultName = getDefaultName(state);
      this.componentPatch = DataComponentPatch.EMPTY;
   }

   public static Component getDefaultName(BlockState state) {
      if (AllBlocks.NETHERITE_BACKTANK.has(state)) {
         ((BacktankItem)AllItems.NETHERITE_BACKTANK.get()).getDescription();
      }

      return ((BacktankItem)AllItems.COPPER_BACKTANK.get()).getDescription();
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      super.addBehaviours(behaviours);
      this.registerAwardables(behaviours, new CreateAdvancement[]{AllAdvancements.BACKTANK});
   }

   @Override
   public void onSpeedChanged(float previousSpeed) {
      super.onSpeedChanged(previousSpeed);
      if (this.getSpeed() != 0.0F) {
         this.award(AllAdvancements.BACKTANK);
      }
   }

   @Override
   public void tick() {
      super.tick();
      if (this.getSpeed() != 0.0F) {
         BlockState state = this.getBlockState();
         BooleanProperty waterProperty = BlockStateProperties.WATERLOGGED;
         if (!state.hasProperty(waterProperty) || !(Boolean)state.getValue(waterProperty)) {
            if (this.airLevelTimer > 0) {
               this.airLevelTimer--;
            } else {
               int max = BacktankUtil.maxAir(this.capacityEnchantLevel);
               if (this.level.isClientSide) {
                  Vec3 centerOf = VecHelper.getCenterOf(this.worldPosition);
                  Vec3 v = VecHelper.offsetRandomly(centerOf, this.level.random, 0.65F);
                  Vec3 m = centerOf.subtract(v);
                  if (this.airLevel != max) {
                     this.level.addParticle(new AirParticleData(1.0F, 0.05F), v.x, v.y, v.z, m.x, m.y, m.z);
                  }
               } else if (this.airLevel != max) {
                  int prevComparatorLevel = this.getComparatorOutput();
                  float abs = Math.abs(this.getSpeed());
                  int increment = Mth.clamp(((int)abs - 100) / 20, 1, 5);
                  this.airLevel = Math.min(max, this.airLevel + increment);
                  if (this.getComparatorOutput() != prevComparatorLevel && !this.level.isClientSide) {
                     this.level.updateNeighbourForOutputSignal(this.worldPosition, state.getBlock());
                  }

                  if (this.airLevel == max) {
                     this.sendData();
                  }

                  this.airLevelTimer = Mth.clamp((int)(128.0F - abs / 5.0F) - 108, 0, 20);
               }
            }
         }
      }
   }

   public int getComparatorOutput() {
      int max = BacktankUtil.maxAir(this.capacityEnchantLevel);
      return ComparatorUtil.fractionToRedstoneLevel((double)((float)this.airLevel / (float)max));
   }

   @Override
   protected void write(CompoundTag compound, Provider registries, boolean clientPacket) {
      super.write(compound, registries, clientPacket);
      compound.putInt("Air", this.airLevel);
      compound.putInt("Timer", this.airLevelTimer);
      compound.putInt("CapacityEnchantment", this.capacityEnchantLevel);
      if (this.customName != null) {
         compound.putString("CustomName", Serializer.toJson(this.customName, registries));
      }

      compound.put("Components", (Tag)CatnipCodecUtils.encode(DataComponentPatch.CODEC, registries, this.componentPatch).orElse(new CompoundTag()));
   }

   @Override
   protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
      super.read(compound, registries, clientPacket);
      int prev = this.airLevel;
      this.airLevel = compound.getInt("Air");
      this.airLevelTimer = compound.getInt("Timer");
      this.capacityEnchantLevel = compound.getInt("CapacityEnchantment");
      if (compound.contains("CustomName", 8)) {
         this.customName = Serializer.fromJson(compound.getString("CustomName"), registries);
      }

      this.componentPatch = CatnipCodecUtils.decode(DataComponentPatch.CODEC, registries, compound.getCompound("Components")).orElse(DataComponentPatch.EMPTY);
      if (prev != 0 && prev != this.airLevel && this.airLevel == BacktankUtil.maxAir(this.capacityEnchantLevel) && clientPacket) {
         this.playFilledEffect();
      }
   }

   protected void applyImplicitComponents(DataComponentInput componentInput) {
      this.setAirLevel((Integer)componentInput.getOrDefault(AllDataComponents.BACKTANK_AIR, 0));
   }

   protected void collectImplicitComponents(Builder components) {
      components.set(AllDataComponents.BACKTANK_AIR, this.airLevel);
   }

   protected void playFilledEffect() {
      AllSoundEvents.CONFIRM.playAt(this.level, this.worldPosition, 0.4F, 1.0F, true);
      Vec3 baseMotion = new Vec3(0.25, 0.1, 0.0);
      Vec3 baseVec = VecHelper.getCenterOf(this.worldPosition);

      for (int i = 0; i < 360; i += 10) {
         Vec3 m = VecHelper.rotate(baseMotion, (double)i, Axis.Y);
         Vec3 v = baseVec.add(m.normalize().scale(0.25));
         this.level.addParticle(ParticleTypes.SPIT, v.x, v.y, v.z, m.x, m.y, m.z);
      }
   }

   public Component getName() {
      return this.customName != null ? this.customName : this.defaultName;
   }

   public int getAirLevel() {
      return this.airLevel;
   }

   public void setAirLevel(int airLevel) {
      this.airLevel = airLevel;
      this.sendData();
   }

   public void setCustomName(Component customName) {
      this.customName = customName;
   }

   public void setCapacityEnchantLevel(int capacityEnchantLevel) {
      this.capacityEnchantLevel = capacityEnchantLevel;
   }

   public void setComponentPatch(DataComponentPatch componentPatch) {
      this.componentPatch = componentPatch;
   }

   public DataComponentPatch getComponentPatch() {
      return this.componentPatch;
   }
}
