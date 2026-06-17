package dev.ryanhcode.sable.platform;

import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public interface SableChunkEventPlatform {
   SableChunkEventPlatform INSTANCE = SablePlatformUtil.load(SableChunkEventPlatform.class);

   void onClientChunkPacketReplaced(LevelChunk var1);

   void onOldChunkInvalid(LevelChunk var1);

   void onPlotChunkLoaded(LevelChunk var1);
}
