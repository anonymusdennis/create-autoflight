package com.simibubi.create.api.schematic.nbt;

import com.simibubi.create.api.registry.SimpleRegistry;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class SafeNbtWriterRegistry {
   public static final SimpleRegistry<BlockEntityType<?>, SafeNbtWriterRegistry.SafeNbtWriter> REGISTRY = SimpleRegistry.create();

   private SafeNbtWriterRegistry() {
      throw new AssertionError("This class should not be instantiated");
   }

   @FunctionalInterface
   public interface SafeNbtWriter {
      void writeSafe(BlockEntity var1, CompoundTag var2, Provider var3);
   }
}
