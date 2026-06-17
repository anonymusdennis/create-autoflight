package dev.simulated_team.simulated.multiloader.inventory;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;

public interface NBTSerializable {
   CompoundTag write(Provider var1);

   void read(Provider var1, CompoundTag var2);
}
