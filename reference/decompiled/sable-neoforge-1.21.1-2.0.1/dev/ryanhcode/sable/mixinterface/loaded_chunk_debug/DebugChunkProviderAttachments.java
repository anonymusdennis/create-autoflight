package dev.ryanhcode.sable.mixinterface.loaded_chunk_debug;

import java.util.Collection;
import net.minecraft.world.level.chunk.LevelChunk;

public interface DebugChunkProviderAttachments {
   Collection<LevelChunk> sable$loadedChunks();
}
