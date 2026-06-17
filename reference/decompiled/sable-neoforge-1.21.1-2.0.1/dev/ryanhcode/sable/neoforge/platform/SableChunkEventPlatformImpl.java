package dev.ryanhcode.sable.neoforge.platform;

import dev.ryanhcode.sable.platform.SableChunkEventPlatform;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkEvent.Load;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public class SableChunkEventPlatformImpl implements SableChunkEventPlatform {
   @Override
   public void onClientChunkPacketReplaced(LevelChunk chunk) {
      NeoForge.EVENT_BUS.post(new Load(chunk, false));
   }

   @Override
   public void onOldChunkInvalid(LevelChunk chunk) {
   }

   @Override
   public void onPlotChunkLoaded(LevelChunk chunk) {
      NeoForge.EVENT_BUS.post(new Load(chunk, false));
   }
}
