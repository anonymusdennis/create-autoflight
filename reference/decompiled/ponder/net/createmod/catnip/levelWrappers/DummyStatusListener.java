package net.createmod.catnip.levelWrappers;

import javax.annotation.Nullable;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public class DummyStatusListener implements ChunkProgressListener {
   public void updateSpawnPos(ChunkPos pCenter) {
   }

   public void onStatusChange(ChunkPos pChunkPosition, @Nullable ChunkStatus pNewStatus) {
   }

   public void start() {
   }

   public void stop() {
   }
}
