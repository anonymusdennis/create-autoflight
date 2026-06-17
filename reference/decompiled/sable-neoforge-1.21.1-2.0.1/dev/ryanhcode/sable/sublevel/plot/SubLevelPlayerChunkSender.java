package dev.ryanhcode.sable.sublevel.plot;

import java.util.function.Consumer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.lighting.LevelLightEngine;

public class SubLevelPlayerChunkSender {
   public static void sendChunk(Consumer<Packet<? super ClientGamePacketListener>> listener, LevelLightEngine lightEngine, LevelChunk chunk) {
      listener.accept(new ClientboundLevelChunkWithLightPacket(chunk, lightEngine, null, null));
   }

   public static void sendChunkPoiData(ServerLevel level, LevelChunk chunk) {
      ChunkPos chunkPos = chunk.getPos();
      DebugPackets.sendPoiPacketsForChunk(level, chunkPos);
   }
}
