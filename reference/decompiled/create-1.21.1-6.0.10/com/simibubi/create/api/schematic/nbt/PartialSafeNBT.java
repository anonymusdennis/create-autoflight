package com.simibubi.create.api.schematic.nbt;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;

public interface PartialSafeNBT {
   void writeSafe(CompoundTag var1, Provider var2);
}
