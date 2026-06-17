package com.simibubi.create.content.redstone.diodes;

import com.simibubi.create.content.equipment.clipboard.ClipboardCloneable;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public abstract class BrassDiodeBlockEntity extends SmartBlockEntity implements ClipboardCloneable {
   protected int state;
   ScrollValueBehaviour maxState;

   public BrassDiodeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      this.maxState = new BrassDiodeScrollValueBehaviour(CreateLang.translateDirect("logistics.redstone_interval"), this, new BrassDiodeScrollSlot())
         .between(2, 72000);
      this.maxState.withFormatter(this::format);
      this.maxState.withCallback(this::onMaxDelayChanged);
      this.maxState.setValue(this.defaultValue());
      behaviours.add(this.maxState);
   }

   protected int defaultValue() {
      return 2;
   }

   public float getProgress() {
      int max = Math.max(2, this.maxState.getValue());
      return (float)Mth.clamp(this.state, 0, max) / (float)max;
   }

   public boolean isIdle() {
      return this.state == 0;
   }

   @Override
   public void tick() {
      super.tick();
      boolean powered = (Boolean)this.getBlockState().getValue(DiodeBlock.POWERED);
      boolean powering = (Boolean)this.getBlockState().getValue(BrassDiodeBlock.POWERING);
      boolean atMax = this.state >= this.maxState.getValue();
      boolean atMin = this.state <= 0;
      this.updateState(powered, powering, atMax, atMin);
   }

   protected abstract void updateState(boolean var1, boolean var2, boolean var3, boolean var4);

   private void onMaxDelayChanged(int newMax) {
      this.state = Mth.clamp(this.state, 0, newMax);
      this.sendData();
   }

   @Override
   protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
      this.state = compound.getInt("State");
      super.read(compound, registries, clientPacket);
   }

   @Override
   public void write(CompoundTag compound, Provider registries, boolean clientPacket) {
      compound.putInt("State", this.state);
      super.write(compound, registries, clientPacket);
   }

   private String format(int value) {
      if (value < 60) {
         return value + "t";
      } else {
         return value < 1200 ? value / 20 + "s" : value / 20 / 60 + "m";
      }
   }

   @Override
   public String getClipboardKey() {
      return "Block";
   }

   @Override
   public boolean readFromClipboard(@NotNull Provider registries, CompoundTag tag, Player player, Direction side, boolean simulate) {
      if (!tag.contains("Inverted")) {
         return false;
      } else if (simulate) {
         return true;
      } else {
         BlockState blockState = this.getBlockState();
         if ((Boolean)blockState.getValue(BrassDiodeBlock.INVERTED) != tag.getBoolean("Inverted")) {
            this.level.setBlockAndUpdate(this.worldPosition, (BlockState)blockState.cycle(BrassDiodeBlock.INVERTED));
         }

         return true;
      }
   }

   @Override
   public boolean writeToClipboard(@NotNull Provider registries, CompoundTag tag, Direction side) {
      tag.putBoolean("Inverted", this.getBlockState().getOptionalValue(BrassDiodeBlock.INVERTED).orElse(false));
      return true;
   }
}
