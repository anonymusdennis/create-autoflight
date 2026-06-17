package dev.simulated_team.simulated.content.worldgen;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

public class EndSeaPreset extends SimulatedWorldPreset {
   public static Vec3 PLAYER_SPAWN_POS = new Vec3(0.0, -30.0, 0.0);

   public EndSeaPreset(ResourceLocation id, Component description) {
      super(id, description);
   }

   @Override
   public void onPlayerJoin(ServerLevel level, ServerPlayer player) {
      if (!level.dimension().equals(Level.END)) {
         player.setRespawnPosition(Level.END, BlockPos.containing(PLAYER_SPAWN_POS), 0.0F, true, false);
         ServerLevel endLevel = level.getServer().getLevel(Level.END);
         DimensionTransition transition = new DimensionTransition(endLevel, player, DimensionTransition.DO_NOTHING);
         player.changeDimension(transition);
         player.teleportTo(PLAYER_SPAWN_POS.x(), PLAYER_SPAWN_POS.y(), PLAYER_SPAWN_POS.z());
      }
   }

   @Override
   public void onChunkLoad(ServerLevel level, ChunkAccess chunkAccess, boolean newChunk) {
      if (newChunk && chunkAccess.getPos().equals(ChunkPos.ZERO) && level.dimension().equals(Level.END)) {
         SubLevelContainer container = SubLevelContainer.getContainer(level);
         Pose3d pose = new Pose3d();
         pose.position().set(-4.5, -41.0, -4.5);
         SubLevel subLevel = container.allocateNewSubLevel(pose);
         LevelPlot plot = subLevel.getPlot();
         int size = 5;
         MutableBlockPos pos = new MutableBlockPos();
         plot.newEmptyChunk(plot.getCenterChunk());

         for (int i = -5; i < 5; i++) {
            for (int j = -5; j < 5; j++) {
               pos.set(i, 0, j);
               plot.getEmbeddedLevelAccessor().setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
            }
         }

         subLevel.updateLastPose();
      }
   }
}
