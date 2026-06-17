package com.simibubi.create.content.decoration.slidingDoor;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;

public class DoorControlBehaviour extends BlockEntityBehaviour {
   public static final BehaviourType<DoorControlBehaviour> TYPE = new BehaviourType<>();
   public DoorControl mode = DoorControl.ALL;

   public DoorControlBehaviour(SmartBlockEntity be) {
      super(be);
   }

   public void set(DoorControl mode) {
      if (this.mode != mode) {
         this.mode = mode;
         this.blockEntity.notifyUpdate();
      }
   }

   @Override
   public void write(CompoundTag nbt, Provider registries, boolean clientPacket) {
      NBTHelper.writeEnum(nbt, "DoorControl", this.mode);
      super.write(nbt, registries, clientPacket);
   }

   @Override
   public void read(CompoundTag nbt, Provider registries, boolean clientPacket) {
      this.mode = (DoorControl)NBTHelper.readEnum(nbt, "DoorControl", DoorControl.class);
      super.read(nbt, registries, clientPacket);
   }

   @Override
   public BehaviourType<?> getType() {
      return TYPE;
   }
}
