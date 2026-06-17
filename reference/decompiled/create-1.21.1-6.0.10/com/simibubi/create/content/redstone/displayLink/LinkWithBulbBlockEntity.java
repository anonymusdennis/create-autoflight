package com.simibubi.create.content.redstone.displayLink;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public abstract class LinkWithBulbBlockEntity extends SmartBlockEntity {
   private LerpedFloat glow = LerpedFloat.linear().startWithValue(0.0);
   private boolean sendPulse;

   public LinkWithBulbBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
      this.glow.chase(0.0, 0.5, Chaser.EXP);
   }

   @Override
   public void tick() {
      super.tick();
      if (this.isVirtual() || this.level.isClientSide()) {
         this.glow.tickChaser();
      }
   }

   public float getGlow(float partialTicks) {
      return this.glow.getValue(partialTicks);
   }

   public void sendPulseNextSync() {
      this.sendPulse = true;
   }

   public void pulse() {
      this.glow.setValue(2.0);
   }

   @Override
   protected void write(CompoundTag tag, Provider registries, boolean clientPacket) {
      super.write(tag, registries, clientPacket);
      if (clientPacket && this.sendPulse) {
         this.sendPulse = false;
         NBTHelper.putMarker(tag, "Pulse");
      }
   }

   @Override
   protected void read(CompoundTag tag, Provider registries, boolean clientPacket) {
      super.read(tag, registries, clientPacket);
      if (clientPacket && tag.contains("Pulse")) {
         this.pulse();
      }
   }

   public Vec3 getBulbOffset(BlockState state) {
      return Vec3.ZERO;
   }

   public Direction getBulbFacing(BlockState state) {
      return (Direction)state.getValue(DisplayLinkBlock.FACING);
   }
}
