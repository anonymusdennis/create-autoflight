package dev.ryanhcode.sable.sublevel;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import java.util.UUID;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.joml.Matrix4d;

public abstract class SubLevel implements SubLevelAccess {
   private final Level level;
   private final Pose3d pose;
   protected final Pose3d lastPose;
   protected final BoundingBox3d globalBounds = new BoundingBox3d(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
   protected final BoundingBox3d lastGlobalBounds = new BoundingBox3d(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
   private final Matrix4d globalBoundsTransform = new Matrix4d();
   private final LevelPlot plot;
   private boolean isRemoved = false;
   private UUID uniqueId = null;
   @Nullable
   private String name = null;

   protected SubLevel(Level level, int plotX, int plotY, Pose3d pose) {
      this.level = level;
      SubLevelContainer plotContainer = SubLevelContainer.getContainer(this.level);
      if (plotContainer == null) {
         throw new IllegalStateException("Level does not have a plot container");
      } else {
         this.plot = this.createPlot(plotContainer, plotX, plotY, plotContainer.getLogPlotSize());
         this.pose = new Pose3d(pose);
         this.lastPose = new Pose3d(pose);
      }
   }

   protected abstract LevelPlot createPlot(SubLevelContainer var1, int var2, int var3, int var4);

   public void onPlotBoundsChanged() {
   }

   public void updateLastPose() {
      this.lastPose.set(this.pose);
   }

   public void tick() {
      this.plot.tick();
   }

   public void updateBoundingBox() {
      BoundingBox3ic plotBounds = this.plot.getBoundingBox();

      assert plotBounds != null : "Plot bounds are null";

      this.lastGlobalBounds.set(this.globalBounds);
      this.globalBounds
         .set(
            (double)plotBounds.minX(),
            (double)plotBounds.minY(),
            (double)plotBounds.minZ(),
            (double)plotBounds.maxX() + 1.0,
            (double)plotBounds.maxY() + 1.0,
            (double)plotBounds.maxZ() + 1.0
         );
      this.globalBounds.transform(this.pose, this.globalBoundsTransform, this.globalBounds);
   }

   public Level getLevel() {
      return this.level;
   }

   public Pose3d logicalPose() {
      return this.pose;
   }

   public Pose3dc lastPose() {
      return this.lastPose;
   }

   public BoundingBox3dc boundingBox() {
      return this.globalBounds;
   }

   public LevelPlot getPlot() {
      return this.plot;
   }

   @Internal
   public void onRemove() {
      this.plot.onRemove();
      this.markRemoved();
   }

   public boolean isRemoved() {
      return this.isRemoved;
   }

   public void markRemoved() {
      this.isRemoved = true;
   }

   @Internal
   public void setUniqueId(UUID uniqueId) {
      this.uniqueId = uniqueId;
   }

   @NotNull
   public UUID getUniqueId() {
      return this.uniqueId;
   }

   @Nullable
   public String getName() {
      return this.name;
   }

   public void setName(@Nullable String name) {
      this.name = name;
   }

   @Override
   public String toString() {
      return "[name=" + this.name + ", id=" + this.uniqueId + ", global_plot=" + this.plot.plotPos.x + "," + this.plot.plotPos.z + "]";
   }
}
