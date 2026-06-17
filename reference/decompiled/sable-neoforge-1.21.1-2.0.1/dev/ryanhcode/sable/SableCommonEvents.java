package dev.ryanhcode.sable;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.network.packets.tcp.ClientboundFloatingBlockMaterialPacket;
import dev.ryanhcode.sable.network.packets.tcp.ClientboundPhysicsPropertyPacket;
import dev.ryanhcode.sable.physics.chunk.VoxelNeighborhoodState;
import dev.ryanhcode.sable.physics.config.FloatingBlockMaterialDataHandler;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertiesDefinitionLoader;
import dev.ryanhcode.sable.physics.floating_block.FloatingBlockController;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder;
import dev.ryanhcode.sable.sublevel.plot.heat.SubLevelHeatMapManager;
import dev.ryanhcode.sable.sublevel.water_occlusion.WaterOcclusionContainer;
import foundry.veil.api.network.VeilPacketManager.PacketSink;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

public class SableCommonEvents {
   public static void handleBlockChange(ServerLevel level, LevelChunk chunk, int x, int y, int z, BlockState oldState, BlockState newState) {
      ChunkPos chunkPos = chunk.getPos();
      ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
      PlotChunkHolder plotChunk = container.getChunkHolder(chunkPos);
      int localX = x & 15;
      int localZ = z & 15;
      if (plotChunk != null) {
         LevelPlot plot = container.getPlot(chunkPos);
         BlockPos blockPos = new BlockPos(x, y, z);
         plotChunk.handleBlockChange(localX, y, localZ, oldState, newState);
         plot.updateBoundingBox();
         plot.expandIfNecessary(blockPos);
         SubLevel subLevel = plot.getSubLevel();
         WaterOcclusionContainer<?> waterOcclusionContainer = WaterOcclusionContainer.getContainer(level);
         if (waterOcclusionContainer != null
            && VoxelNeighborhoodState.isSolid(level, blockPos, oldState) != VoxelNeighborhoodState.isSolid(level, blockPos, newState)) {
            waterOcclusionContainer.markDirty(blockPos);
         }

         if (subLevel instanceof ServerSubLevel serverSubLevel) {
            SubLevelHeatMapManager heatMapManager = serverSubLevel.getHeatMapManager();
            FloatingBlockController floatingBlockController = serverSubLevel.getFloatingBlockController();
            if (oldState != newState) {
               floatingBlockController.queueRemoveFloatingBlock(oldState, blockPos);
               floatingBlockController.queueAddFloatingBlock(newState, blockPos);
            }

            if (oldState.isAir() && !newState.isAir()) {
               heatMapManager.onSolidAdded(blockPos);
            }

            if (!oldState.isAir() && newState.isAir()) {
               heatMapManager.onSolidRemoved(blockPos);
            }
         }

         if (subLevel.isRemoved()) {
            return;
         }
      }

      int idx = chunk.getSectionIndex(y);
      LevelChunkSection section = chunk.getSection(idx);
      SectionPos sectionPos = SectionPos.of(chunkPos, chunk.getSectionYFromSectionIndex(idx));
      container.physicsSystem().handleBlockChange(sectionPos, section, localX, y & 15, localZ, oldState, newState);
   }

   public static void syncDataPacket(PacketSink sink) {
      sink.sendPacket(
         PhysicsBlockPropertiesDefinitionLoader.INSTANCE
            .getDefinitions()
            .stream()
            .map(ClientboundPhysicsPropertyPacket::new)
            .toArray(CustomPacketPayload[]::new)
      );
      sink.sendPacket(
         FloatingBlockMaterialDataHandler.allMaterials
            .entrySet()
            .stream()
            .map(e -> new ClientboundFloatingBlockMaterialPacket(e.getKey(), e.getValue()))
            .toArray(CustomPacketPayload[]::new)
      );
   }
}
