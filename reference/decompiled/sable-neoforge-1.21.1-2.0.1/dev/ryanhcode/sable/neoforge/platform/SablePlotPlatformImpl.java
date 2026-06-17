package dev.ryanhcode.sable.neoforge.platform;

import com.mojang.logging.LogUtils;
import dev.ryanhcode.sable.platform.SablePlotPlatform;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkDataEvent.Load;
import org.slf4j.Logger;

public class SablePlotPlatformImpl implements SablePlotPlatform {
   private static final Logger LOGGER = LogUtils.getLogger();

   @Override
   public void readLightData(CompoundTag tag, RegistryAccess registryAccess, LevelChunk chunk) {
      if (tag.contains("neoforge:aux_lights", 9)) {
         chunk.getAuxLightManager(chunk.getPos()).deserializeNBT(registryAccess, tag.getList("neoforge:aux_lights", 10));
      }
   }

   @Override
   public void readChunkAttachments(CompoundTag tag, RegistryAccess registryAccess, LevelChunk chunk) {
      if (tag.contains("neoforge:attachments", 10)) {
         chunk.readAttachmentsFromNBT(registryAccess, tag.getCompound("neoforge:attachments"));
      }
   }

   @Override
   public void postLoad(CompoundTag tag, LevelChunk chunk) {
      NeoForge.EVENT_BUS.post(new Load(chunk, tag, ChunkType.LEVELCHUNK));
   }

   @Override
   public void writeLightData(CompoundTag tag, RegistryAccess registryAccess, LevelChunk chunk) {
      Tag lightTag = chunk.getAuxLightManager(chunk.getPos()).serializeNBT(registryAccess);
      if (lightTag != null) {
         tag.put("neoforge:aux_lights", lightTag);
      }
   }

   @Override
   public void writeChunkAttachments(CompoundTag tag, RegistryAccess registryAccess, LevelChunk chunk) {
      try {
         CompoundTag capTag = chunk.writeAttachmentsToNBT(registryAccess);
         if (capTag != null) {
            tag.put("neoforge:attachments", capTag);
         }
      } catch (Exception var5) {
         LOGGER.error(
            "Failed to write chunk attachments. An attachment has likely thrown an exception trying to write state. It will not persist. Report this to the mod author",
            var5
         );
      }
   }
}
