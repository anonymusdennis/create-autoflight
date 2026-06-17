package com.simibubi.create.content.equipment.bell;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import java.util.List;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public abstract class AbstractBellBlockEntity extends SmartBlockEntity {
   public static final int RING_DURATION = 74;
   public boolean isRinging;
   public int ringingTicks;
   public Direction ringDirection;

   public AbstractBellBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
   }

   public boolean ring(Level world, BlockPos pos, Direction direction) {
      this.isRinging = true;
      this.ringingTicks = 0;
      this.ringDirection = direction;
      this.sendData();
      return true;
   }

   @Override
   public void tick() {
      super.tick();
      if (this.isRinging) {
         this.ringingTicks++;
      }

      if (this.ringingTicks >= 74) {
         this.isRinging = false;
         this.ringingTicks = 0;
      }
   }

   @Override
   protected void write(CompoundTag tag, Provider registries, boolean clientPacket) {
      super.write(tag, registries, clientPacket);
      if (clientPacket && this.ringingTicks == 0 && this.isRinging) {
         NBTHelper.writeEnum(tag, "Ringing", this.ringDirection);
      }
   }

   @Override
   protected void read(CompoundTag tag, Provider registries, boolean clientPacket) {
      super.read(tag, registries, clientPacket);
      if (clientPacket && tag.contains("Ringing")) {
         this.ringDirection = (Direction)NBTHelper.readEnum(tag, "Ringing", Direction.class);
         this.ringingTicks = 0;
         this.isRinging = true;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public abstract PartialModel getBellModel();
}
