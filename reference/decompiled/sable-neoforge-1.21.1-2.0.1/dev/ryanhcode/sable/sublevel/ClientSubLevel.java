package dev.ryanhcode.sable.sublevel;

import dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.ClientSubLevelAccess;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.network.client.ClientSableInterpolationState;
import dev.ryanhcode.sable.network.client.SubLevelSnapshotInterpolator;
import dev.ryanhcode.sable.sublevel.plot.ClientLevelPlot;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.render.SubLevelRenderData;
import dev.ryanhcode.sable.sublevel.render.dispatcher.SubLevelRenderDispatcher;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class ClientSubLevel extends SubLevel implements ClientSubLevelAccess {
   private SubLevelRenderData renderData;
   private final Vector3d latestNetworkedVelocity = new Vector3d();
   private final Vector3d latestNetworkedAngularVelocity = new Vector3d();
   private final Pose3d renderPose = new Pose3d();
   private final SubLevelSnapshotInterpolator interpolator;
   private final BoundingBox3d sweptBounds = new BoundingBox3d();
   private final Vector3d lastBoundsCenter = new Vector3d();
   private int latestSkyLightScale = -1;
   private float lastRenderPosePartialTick = -1.0F;
   private int lightingSceneId = -1;
   private boolean finalized = false;

   public ClientSubLevel(Level level, int plotX, int plotY, Pose3d pose) {
      super(level, plotX, plotY, pose);
      this.logicalPose().set(pose);
      this.interpolator = new SubLevelSnapshotInterpolator(pose);
   }

   @Override
   protected LevelPlot createPlot(SubLevelContainer plotContainer, int plotX, int plotY, int logPlotSize) {
      return new ClientLevelPlot(plotContainer, plotX, plotY, plotContainer.getLogPlotSize(), this);
   }

   @Override
   public void tick() {
      this.updateLastPose();
      super.tick();
      this.lastRenderPosePartialTick = -1.0F;
      Pose3d logicalPose = this.logicalPose();
      ClientSubLevelContainer container = ClientSubLevelContainer.getContainer(this.getLevel());

      assert container != null;

      this.interpolator.tick(container.getInterpolation().getTickPointer());
      Pose3dc interpolatedPose = this.interpolator.getInterpolatedPose();
      logicalPose.set(interpolatedPose);
      this.updateBoundingBox();
      if (this.lastGlobalBounds.minX == 0.0 && this.lastGlobalBounds.maxX == 0.0) {
         this.sweptBounds.set(this.globalBounds);
      } else {
         this.sweptBounds.set(this.lastGlobalBounds).expandTo(this.globalBounds, this.sweptBounds);
      }

      this.latestSkyLightScale = this.computeSubLevelSkyLight(this.logicalPose());
   }

   public void forceUpdateBounds() {
      this.updateBoundingBox();
      this.lastGlobalBounds.set(this.globalBounds);
      this.sweptBounds.set(this.globalBounds);
   }

   public int scaleSkyLight(int skyLight) {
      return (int)((float)skyLight * ((float)this.getLatestSkyLightScale() / 15.0F));
   }

   public int scaleLightColor(int lightColor) {
      int skyLightScale = this.getLatestSkyLightScale();
      int newSkyLight = (int)((float)(lightColor >> 20) * ((float)skyLightScale / 15.0F));
      return lightColor & 1048575 | newSkyLight << 20;
   }

   public int getLatestSkyLightScale() {
      if (this.latestSkyLightScale == -1) {
         this.latestSkyLightScale = this.computeSubLevelSkyLight(this.logicalPose());
      }

      return this.latestSkyLightScale;
   }

   public int computeSubLevelSkyLight(Pose3dc pose) {
      Vector3dc pos = pose.position();
      ClientLevel level = this.getLevel();
      if (this.boundingBox().volume() < 9.0) {
         int skyLight = level.getBrightness(LightLayer.SKY, BlockPos.containing(pos.x(), pos.y(), pos.z()));
         if (skyLight == 0) {
            skyLight = level.getBrightness(LightLayer.SKY, BlockPos.containing(pos.x(), pos.y() + 1.0, pos.z()));
         }

         if (skyLight == 0) {
            skyLight = level.getBrightness(LightLayer.SKY, BlockPos.containing(pos.x(), pos.y() - 1.0, pos.z()));
         }

         return skyLight;
      } else {
         BoundingBox3dc box = this.boundingBox();
         Vector3dc center = box.center(this.lastBoundsCenter);
         double xMin = box.minX();
         double xMax = box.maxX();
         double zMin = box.minZ();
         double zMax = box.maxZ();
         int maxLight = 0;
         double sampleY = center.y() + 0.1;
         maxLight = Math.max(maxLight, level.getBrightness(LightLayer.SKY, BlockPos.containing(center.x(), sampleY, center.z())));
         maxLight = Math.max(maxLight, level.getBrightness(LightLayer.SKY, BlockPos.containing(xMin, sampleY, zMin)));
         maxLight = Math.max(maxLight, level.getBrightness(LightLayer.SKY, BlockPos.containing(xMax, sampleY, zMin)));
         maxLight = Math.max(maxLight, level.getBrightness(LightLayer.SKY, BlockPos.containing(xMin, sampleY, zMax)));
         return Math.max(maxLight, level.getBrightness(LightLayer.SKY, BlockPos.containing(xMax, sampleY, zMax)));
      }
   }

   @Override
   public BoundingBox3dc boundingBox() {
      return this.sweptBounds;
   }

   @Override
   public void onPlotBoundsChanged() {
      this.renderData = SubLevelRenderDispatcher.get().resize(this, this.renderData);
   }

   @Override
   public void onRemove() {
      if (this.lightingSceneId != -1) {
         SubLevelContainer.getContainer(this.getLevel()).freeLightingScene(this.lightingSceneId);
         this.lightingSceneId = -1;
      }

      super.onRemove();
      this.renderData.close();
   }

   public void updateRenderData() {
      try {
         if (this.renderData != null) {
            this.renderData.close();
         }

         this.renderData = SubLevelRenderDispatcher.get().createRenderData(this);
      } catch (Throwable var4) {
         CrashReport crashreport = CrashReport.forThrowable(var4, "Updating render data");
         CrashReportCategory crashreportcategory = crashreport.addCategory("Render Dispatcher");
         crashreportcategory.setDetail("Class", () -> SubLevelRenderDispatcher.get().getClass().getName());
         throw new ReportedException(crashreport);
      }
   }

   public SubLevelRenderData getRenderData() {
      return this.renderData;
   }

   public ClientLevel getLevel() {
      return (ClientLevel)super.getLevel();
   }

   public ClientLevelPlot getPlot() {
      return (ClientLevelPlot)super.getPlot();
   }

   @Internal
   public void setLightingSceneId(int lightingSceneId) {
      this.lightingSceneId = lightingSceneId;
   }

   @Internal
   public int getLightingSceneId() {
      return this.lightingSceneId;
   }

   public Pose3dc renderPose() {
      float pt = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
      if (this.lastRenderPosePartialTick == pt) {
         this.lastRenderPosePartialTick = pt;
         return this.renderPose;
      } else {
         return this.renderPose(pt);
      }
   }

   public Pose3dc renderPose(float pt) {
      if (this.lastRenderPosePartialTick == pt) {
         this.lastRenderPosePartialTick = pt;
         return this.renderPose;
      } else {
         Pose3d renderPose = this.renderPose.set(this.lastPose());
         Pose3d target = this.logicalPose();
         renderPose.position().lerp(target.position(), (double)pt);
         renderPose.orientation().slerp(target.orientation(), (double)pt);
         renderPose.rotationPoint().lerp(target.rotationPoint(), (double)pt);
         renderPose.scale().lerp(target.scale(), (double)pt);
         return renderPose;
      }
   }

   public void receiveServerMovementStop() {
      this.latestNetworkedVelocity.zero();
      this.latestNetworkedAngularVelocity.zero();
      this.interpolator.receiveStop();
   }

   @Internal
   public void wasSplitFrom(ClientSableInterpolationState state, @NotNull ClientSubLevel splitFrom, @NotNull Pose3dc pose) {
      SubLevelSnapshotInterpolator otherInterpolator = splitFrom.getInterpolator();
      this.interpolator.splitFrom(otherInterpolator, pose);
      this.setInitialPosesFrom(state);
   }

   @Internal
   public void setInitialPosesFrom(ClientSableInterpolationState state) {
      if (!state.isStopped()) {
         this.interpolator.getSampleAt(state.mostRecentInterpolationTick, this.logicalPose());
         this.interpolator.getSampleAt(state.lastInterpolationTick, this.lastPose);
      }
   }

   public SubLevelSnapshotInterpolator getInterpolator() {
      return this.interpolator;
   }

   @Override
   public String toString() {
      return "ClientSubLevel" + super.toString();
   }

   public void setFinalized() {
      this.finalized = true;
   }

   public boolean isFinalized() {
      return this.finalized;
   }
}
