package dev.ryanhcode.sable.mixin.loaded_chunk_debug;

import java.util.concurrent.atomic.AtomicReferenceArray;
import net.minecraft.client.multiplayer.ClientChunkCache.Storage;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({Storage.class})
public interface ClientChunkCacheStorageAccessor {
   @Accessor
   AtomicReferenceArray<LevelChunk> getChunks();
}
