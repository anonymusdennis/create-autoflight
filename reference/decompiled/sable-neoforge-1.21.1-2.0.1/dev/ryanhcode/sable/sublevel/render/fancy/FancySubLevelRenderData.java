package dev.ryanhcode.sable.sublevel.render.fancy;

import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.render.SubLevelRenderData;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.LinkedList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.PrioritizeChunkUpdates;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.core.SectionPos;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3i;
import org.joml.Vector3ic;

public class FancySubLevelRenderData implements SubLevelRenderData {
   private final ClientSubLevel subLevel;
   private final FancySubLevelSectionCompiler compiler;
   private final Vector3d origin = new Vector3d();
   private final Vector3i chunkOrigin = new Vector3i();
   private final Vector3i size = new Vector3i();
   private final List<FancySubLevelSectionCompiler.RenderSection> allRenderSections = new ObjectArrayList();
   private final List<FancySubLevelSectionCompiler.RenderSection> dirtyRenderSections = new LinkedList<>();
   private final FancySubLevelOcclusionData occlusionData;
   private FancySubLevelSectionCompiler.RenderSection[] renderSections;

   public FancySubLevelRenderData(ClientSubLevel subLevel, FancySubLevelSectionCompiler compiler) {
      this.subLevel = subLevel;
      this.compiler = compiler;
      this.occlusionData = new FancySubLevelOcclusionData(this);
      this.resize();
   }

   private int getIndex(int x, int y, int z) {
      return x - this.chunkOrigin.x() + (y - this.chunkOrigin.y()) * this.size.x() + (z - this.chunkOrigin.z()) * this.size.x() * this.size.y();
   }

   private boolean inBounds(int x, int y, int z) {
      int localX = x - this.chunkOrigin.x();
      int localY = y - this.chunkOrigin.y();
      int localZ = z - this.chunkOrigin.z();
      return localX >= 0 && localY >= 0 && localZ >= 0 && localX < this.size.x() && localY < this.size.y() && localZ < this.size.z();
   }

   public void resize() {
      BoundingBox3ic bounds = this.subLevel.getPlot().getBoundingBox();

      for (FancySubLevelSectionCompiler.RenderSection section : this.allRenderSections) {
         section.free();
      }

      this.allRenderSections.clear();
      this.dirtyRenderSections.clear();
      if (bounds != null && !bounds.equals(BoundingBox3i.EMPTY) && (double)bounds.volume() > 0.0) {
         Vector3i minChunkPos = new Vector3i(bounds.minX() >> 4, bounds.minY() >> 4, bounds.minZ() >> 4);
         Vector3i maxChunkPos = new Vector3i(bounds.maxX() >> 4, bounds.maxY() >> 4, bounds.maxZ() >> 4);
         this.size.set(maxChunkPos.x() - minChunkPos.x() + 1, maxChunkPos.y() - minChunkPos.y() + 1, maxChunkPos.z() - minChunkPos.z() + 1);
         this.chunkOrigin.set(minChunkPos);
         this.origin.set((double)(minChunkPos.x() << 4), (double)(minChunkPos.y() << 4), (double)(minChunkPos.z() << 4));
         this.renderSections = new FancySubLevelSectionCompiler.RenderSection[this.size.x() * this.size.y() * this.size.z()];

         for (int z = minChunkPos.z(); z <= maxChunkPos.z(); z++) {
            for (int x = minChunkPos.x(); x <= maxChunkPos.x(); x++) {
               for (int y = minChunkPos.y(); y <= maxChunkPos.y(); y++) {
                  FancySubLevelSectionCompiler.RenderSection section = new FancySubLevelSectionCompiler.RenderSection(SectionPos.of(x, y, z), minChunkPos);
                  this.renderSections[this.getIndex(x, y, z)] = section;
                  this.allRenderSections.add(section);
                  if (section.isDirty()) {
                     this.dirtyRenderSections.add(section);
                  }
               }
            }
         }
      }

      this.occlusionData.invalidate();
   }

   @Nullable
   public FancySubLevelSectionCompiler.RenderSection getRenderSection(int x, int y, int z) {
      int index = this.getIndex(x, y, z);
      return index >= 0 && index < this.renderSections.length ? this.renderSections[index] : null;
   }

   public Vector3dc getOrigin() {
      return this.origin;
   }

   public Vector3ic getChunkOrigin() {
      return this.chunkOrigin;
   }

   public Vector3ic getSize() {
      return this.size;
   }

   @Override
   public void rebuild() {
      this.allRenderSections.clear();
      this.resize();
   }

   @Override
   public boolean isSectionCompiled(int x, int y, int z) {
      FancySubLevelSectionCompiler.RenderSection section = this.getRenderSection(x, y, z);
      return section != null && section.getCompiledSection() != FancySubLevelSectionCompiler.CompiledSection.UNCOMPILED;
   }

   @Override
   public void setDirty(int x, int y, int z, boolean playerChanged) {
      int index = this.getIndex(x, y, z);
      if (index >= 0 && index < this.renderSections.length) {
         FancySubLevelSectionCompiler.RenderSection section = this.renderSections[index];
         if (section != null) {
            if (!section.isDirty()) {
               section.setDirty(playerChanged);
               this.dirtyRenderSections.add(section);
            }
         }
      }
   }

   @Override
   public void compileSections(PrioritizeChunkUpdates chunkUpdates, RenderRegionCache renderRegionCache, Camera camera) {
      if (!this.dirtyRenderSections.isEmpty()) {
         ClientLevel level = this.subLevel.getLevel();
         Vector3d cameraPos = JOMLConversion.atCenterOf(camera.getBlockPosition()).sub(8.0, 8.0, 8.0);
         this.subLevel.logicalPose().transformPositionInverse(cameraPos);

         for (FancySubLevelSectionCompiler.RenderSection section : this.dirtyRenderSections) {
            SectionPos origin = section.getPos();
            double distanceSq = cameraPos.distanceSquared((double)(origin.x() << 4), (double)(origin.y() << 4), (double)(origin.z() << 4));
            this.compiler
               .getScheduler()
               .scheduleCompile(section, renderRegionCache.createRegion(level, section.getPos()), distanceSq, this.occlusionData::addSection);
            section.setNotDirty();
         }

         this.dirtyRenderSections.clear();
      }
   }

   @Override
   public int getVisibleSectionCount() {
      return this.allRenderSections.size();
   }

   @Override
   public ClientSubLevel getSubLevel() {
      return this.subLevel;
   }

   @Override
   public void close() {
   }

   public FancySubLevelOcclusionData getOcclusionData() {
      return this.occlusionData;
   }
}
