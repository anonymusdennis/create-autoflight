package dev.ryanhcode.sable.neoforge.compatibility.flywheel;

import dev.engine_room.flywheel.lib.visualization.VisualizationHelper;
import dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import foundry.veil.Veil;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class FlywheelCompatNeoForge {
   public static boolean FLYWHEEL_LOADED = Veil.platform().isModLoaded("flywheel");
   private static final Long2ObjectMap<FlywheelCompatNeoForge.SubLevelFlwRenderState> RENDER_POSES = Long2ObjectMaps.synchronize(new Long2ObjectOpenHashMap());

   public static void tryAddVisual(BlockEntity blockEntity) {
      VisualizationHelper.tryAddBlockEntity(blockEntity);
   }

   public static void preVisualizationFrame(Level level, float partialTicks) {
      ClientSubLevelContainer container = (ClientSubLevelContainer)SubLevelContainer.getContainer(level);
      if (container == null) {
         RENDER_POSES.clear();
      } else {
         ObjectIterator<Entry<FlywheelCompatNeoForge.SubLevelFlwRenderState>> iter = RENDER_POSES.long2ObjectEntrySet().iterator();

         while (iter.hasNext()) {
            Entry<FlywheelCompatNeoForge.SubLevelFlwRenderState> entry = (Entry<FlywheelCompatNeoForge.SubLevelFlwRenderState>)iter.next();
            long pos = entry.getLongKey();
            FlywheelCompatNeoForge.SubLevelFlwRenderState poseEntry = (FlywheelCompatNeoForge.SubLevelFlwRenderState)entry.getValue();
            int plotX = ChunkPos.getX(pos);
            int plotZ = ChunkPos.getZ(pos);
            SubLevel subLevel = container.getSubLevel(plotX, plotZ);
            if (subLevel != null && Objects.equals(subLevel.getUniqueId(), poseEntry.subLevelID)) {
               updateEntry(container, (ClientSubLevel)subLevel, poseEntry, partialTicks);
            } else {
               iter.remove();
            }
         }
      }
   }

   public static FlywheelCompatNeoForge.SubLevelFlwRenderState getInfo(long plotCoord) {
      return (FlywheelCompatNeoForge.SubLevelFlwRenderState)RENDER_POSES.get(plotCoord);
   }

   private static void updateEntry(
      ClientSubLevelContainer container, ClientSubLevel clientSubLevel, FlywheelCompatNeoForge.SubLevelFlwRenderState poseEntry, float partialTicks
   ) {
      poseEntry.sceneID = container.getLightingSceneId(clientSubLevel);
      poseEntry.subLevelID = clientSubLevel.getUniqueId();
      poseEntry.renderPose.set(clientSubLevel.renderPose(partialTicks));
      poseEntry.latestSkyLightScale = (float)clientSubLevel.getLatestSkyLightScale();
      poseEntry.centerChunk = clientSubLevel.getPlot().getCenterChunk();
   }

   public static void createRenderInfo(Level level, SubLevel subLevel) {
      ClientSubLevelContainer container = (ClientSubLevelContainer)SubLevelContainer.getContainer(level);
      if (container != null) {
         ChunkPos plotPos = subLevel.getPlot().plotPos;
         long plotCoord = ChunkPos.asLong(plotPos.x - container.getOrigin().x, plotPos.z - container.getOrigin().y);
         RENDER_POSES.computeIfAbsent(plotCoord, x -> {
            FlywheelCompatNeoForge.SubLevelFlwRenderState renderState = new FlywheelCompatNeoForge.SubLevelFlwRenderState();
            updateEntry(container, (ClientSubLevel)subLevel, renderState, 1.0F);
            return renderState;
         });
      }
   }

   public static class SubLevelFlwRenderState {
      public int sceneID;
      public final Pose3d renderPose = new Pose3d();
      public UUID subLevelID;
      public float latestSkyLightScale;
      public ChunkPos centerChunk;
   }
}
