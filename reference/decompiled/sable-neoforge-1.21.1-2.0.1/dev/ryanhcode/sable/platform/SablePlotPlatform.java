package dev.ryanhcode.sable.platform;

import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.chunk.LevelChunk;

public interface SablePlotPlatform {
   SablePlotPlatform INSTANCE = SablePlatformUtil.load(SablePlotPlatform.class);

   void readLightData(CompoundTag var1, RegistryAccess var2, LevelChunk var3);

   void readChunkAttachments(CompoundTag var1, RegistryAccess var2, LevelChunk var3);

   void postLoad(CompoundTag var1, LevelChunk var2);

   void writeLightData(CompoundTag var1, RegistryAccess var2, LevelChunk var3);

   void writeChunkAttachments(CompoundTag var1, RegistryAccess var2, LevelChunk var3);
}
