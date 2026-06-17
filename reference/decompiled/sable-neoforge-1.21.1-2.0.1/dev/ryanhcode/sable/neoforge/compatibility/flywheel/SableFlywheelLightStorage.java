package dev.ryanhcode.sable.neoforge.compatibility.flywheel;

import dev.engine_room.flywheel.api.task.Plan;
import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visual.DynamicVisual.Context;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.backend.BackendDebugFlags;
import dev.engine_room.flywheel.backend.engine.LightStorage;
import dev.engine_room.flywheel.backend.engine.indirect.StagingBuffer;
import dev.engine_room.flywheel.backend.gl.buffer.GlBuffer;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.task.SimplePlan;
import dev.engine_room.flywheel.lib.task.functional.RunnableWithContext.Ignored;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import dev.engine_room.flywheel.lib.visual.component.HitboxComponent;
import dev.engine_room.flywheel.lib.visual.util.InstanceRecycler;
import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.neoforge.mixin.compatibility.flywheel.LightStorageAccessor;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import java.util.BitSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

public class SableFlywheelLightStorage extends LightStorage {
   private static final int INVALID_SECTION = -1;
   public static final int STATIC_SCENE_ID = 0;
   private final SableLightLut sableLut;
   private final Int2ObjectMap<Long2IntMap> scene2SectionArenaIndexMap;
   private final BitSet changed = new BitSet();
   private final LongSet updatedSections = new LongOpenHashSet();
   private boolean isDebugOn = false;
   @Nullable
   private LongSet requestedSections;

   public SableFlywheelLightStorage(LevelAccessor level) {
      super(level);
      this.sableLut = new SableLightLut();
      this.scene2SectionArenaIndexMap = new Int2ObjectOpenHashMap();
   }

   public EffectVisual<?> visualize(VisualizationContext ctx, float partialTick) {
      return new SableFlywheelLightStorage.DebugVisual(ctx, partialTick);
   }

   public <C> Plan<C> createFramePlan() {
      return SimplePlan.of(new Ignored[]{() -> {
         if (BackendDebugFlags.LIGHT_STORAGE_VIEW != this.isDebugOn) {
            VisualizationManager visualizationManager = VisualizationManager.get(this.level());
            if (visualizationManager != null) {
               if (BackendDebugFlags.LIGHT_STORAGE_VIEW) {
                  visualizationManager.effects().queueAdd(this);
               } else {
                  visualizationManager.effects().queueRemove(this);
               }
            }

            this.isDebugOn = BackendDebugFlags.LIGHT_STORAGE_VIEW;
         }

         if (!this.updatedSections.isEmpty() || this.requestedSections != null) {
            this.updateLightSections();
         }
      }});
   }

   public void sections(LongSet sections) {
      this.requestedSections = sections;
   }

   private void updateLightSections() {
      this.removeUnusedSections();
      ActiveSableCompanion helper = Sable.HELPER;
      ClientLevel level = Minecraft.getInstance().level;
      ClientSubLevelContainer container = SubLevelContainer.getContainer(level);
      Int2ObjectMap<LongSet> sectionsToCollect;
      if (this.requestedSections == null) {
         sectionsToCollect = new Int2ObjectOpenHashMap();
      } else {
         sectionsToCollect = new Int2ObjectOpenHashMap();
         LongIterator var5 = this.requestedSections.iterator();

         while (var5.hasNext()) {
            long section = (Long)var5.next();
            SectionPos sectionPos = SectionPos.of(section);
            SubLevel subLevel = helper.getContaining(level, sectionPos);
            int lightingSceneId = 0;
            if (subLevel instanceof ClientSubLevel clientSubLevel) {
               int subLevelLightingSceneId = container.getLightingSceneId(clientSubLevel);
               if (subLevelLightingSceneId != -1) {
                  lightingSceneId = subLevelLightingSceneId;
               }
            }

            ((LongSet)sectionsToCollect.computeIfAbsent(lightingSceneId, x -> new LongOpenHashSet())).add(section);
         }
      }

      IntIterator var18 = this.scene2SectionArenaIndexMap.keySet().iterator();

      while (var18.hasNext()) {
         int scene = (Integer)var18.next();
         Long2IntMap section2ArenaIndex = (Long2IntMap)this.scene2SectionArenaIndexMap.get(scene);
         LongSet longs = (LongSet)sectionsToCollect.get(scene);
         if (longs != null) {
            longs.removeAll(section2ArenaIndex.keySet());
         }
      }

      LongIterator var19 = this.updatedSections.iterator();

      while (var19.hasNext()) {
         long updatedSection = (Long)var19.next();

         for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
               for (int z = -1; z <= 1; z++) {
                  long section = SectionPos.offset(updatedSection, x, y, z);
                  SectionPos sectionPos = SectionPos.of(section);
                  if (helper.getContaining(level, sectionPos) instanceof ClientSubLevel clientSubLevelx) {
                     int lightingSceneId = container.getLightingSceneId(clientSubLevelx);
                     if (lightingSceneId != -1) {
                        Long2IntMap map = (Long2IntMap)this.scene2SectionArenaIndexMap.get(lightingSceneId);
                        if (map != null && map.containsKey(section)) {
                           ((LongSet)sectionsToCollect.computeIfAbsent(lightingSceneId, ignored -> new LongOpenHashSet())).add(section);
                        }
                        continue;
                     }
                  }

                  if (this.scene2SectionArenaIndexMap.values().stream().anyMatch(mapx -> mapx.containsKey(section))) {
                     ((LongSet)sectionsToCollect.computeIfAbsent(0, ignored -> new LongOpenHashSet())).add(section);
                  }
               }
            }
         }
      }

      ObjectIterator var20 = sectionsToCollect.int2ObjectEntrySet().iterator();

      while (var20.hasNext()) {
         Entry<LongSet> entry = (Entry<LongSet>)var20.next();
         int scene = entry.getIntKey();
         LongSet sections = (LongSet)entry.getValue();
         LongIterator var29 = sections.iterator();

         while (var29.hasNext()) {
            long sectionx = (Long)var29.next();
            this.collectSection(scene, sectionx);
         }
      }

      this.updatedSections.clear();
      this.requestedSections = null;
   }

   public void onLightUpdate(long section) {
      this.updatedSections.add(section);
   }

   public void collectSection(int scene, long section) {
      int index = this.indexForSection(scene, section);
      this.changed.set(index);
      long ptr = this.arena.indexToPointer(index);
      MemoryUtil.memSet(ptr, 0, (long)LightStorage.SECTION_SIZE_BYTES);
      ((LightStorageAccessor)this).getCollector().collectSection(ptr, section);
   }

   private int indexForSection(int scene, long section) {
      Long2IntMap map = (Long2IntMap)this.scene2SectionArenaIndexMap.get(scene);
      int out = map != null ? map.get(section) : -1;
      if (out == -1) {
         out = this.arena.alloc();
         ((Long2IntMap)this.scene2SectionArenaIndexMap.computeIfAbsent(scene, ignored -> {
            Long2IntOpenHashMap newMap = new Long2IntOpenHashMap();
            newMap.defaultReturnValue(-1);
            return newMap;
         })).put(section, out);
         SectionPos sectionPos = SectionPos.of(section);
         SubLevel subLevel = Sable.HELPER.getContainingClient(sectionPos);
         if (subLevel != null) {
            LevelPlot plot = subLevel.getPlot();
            ChunkPos centerChunk = plot.getCenterChunk();
            section = SectionPos.asLong(sectionPos.x() - centerChunk.x, sectionPos.y(), sectionPos.z() - centerChunk.z);
         }

         this.beginTrackingSection(scene, section, out);
      }

      return out;
   }

   private void removeUnusedSections() {
      if (this.requestedSections != null) {
         ClientLevel world = Minecraft.getInstance().level;
         boolean anyRemoved = false;
         ObjectIterator var3 = this.scene2SectionArenaIndexMap.int2ObjectEntrySet().iterator();

         while (var3.hasNext()) {
            Entry<Long2IntMap> sceneEntry = (Entry<Long2IntMap>)var3.next();
            int sceneId = sceneEntry.getIntKey();
            Long2IntMap section2ArenaIndex = (Long2IntMap)sceneEntry.getValue();
            ObjectSet<it.unimi.dsi.fastutil.longs.Long2IntMap.Entry> entries = section2ArenaIndex.long2IntEntrySet();
            ObjectIterator<it.unimi.dsi.fastutil.longs.Long2IntMap.Entry> it = entries.iterator();

            while (it.hasNext()) {
               it.unimi.dsi.fastutil.longs.Long2IntMap.Entry entry = (it.unimi.dsi.fastutil.longs.Long2IntMap.Entry)it.next();
               long section = entry.getLongKey();
               if (!this.requestedSections.contains(section)) {
                  this.arena.free(entry.getIntValue());
                  long localSection = section;
                  SectionPos sectionPos = SectionPos.of(section);
                  SubLevelContainer container = SubLevelContainer.getContainer(world);
                  if (container != null && container.inBounds(sectionPos.x(), sectionPos.z())) {
                     int logPlotSize = container.getLogPlotSize();
                     int plotX = sectionPos.x() >> logPlotSize;
                     int plotZ = sectionPos.z() >> logPlotSize;
                     localSection = SectionPos.asLong(
                        sectionPos.x() - ((plotX << logPlotSize) + (1 << logPlotSize - 1)),
                        sectionPos.y(),
                        sectionPos.z() - ((plotZ << logPlotSize) + (1 << logPlotSize - 1))
                     );
                  }

                  this.endTrackingSection(sceneId, localSection);
                  it.remove();
                  anyRemoved = true;
               }
            }
         }

         if (anyRemoved) {
            this.sableLut.prune();
            ((LightStorageAccessor)this).setNeedsLutRebuild(true);
         }
      }
   }

   private void beginTrackingSection(int scene, long section, int index) {
      this.sableLut.add(scene, section, index);
      ((LightStorageAccessor)this).setNeedsLutRebuild(true);
   }

   private void endTrackingSection(int scene, long section) {
      this.sableLut.remove(scene, section);
      ((LightStorageAccessor)this).setNeedsLutRebuild(true);
   }

   public void uploadChangedSections(StagingBuffer staging, int dstVbo) {
      for (int i = this.changed.nextSetBit(0); i >= 0; i = this.changed.nextSetBit(i + 1)) {
         staging.enqueueCopy(this.arena.indexToPointer(i), (long)SECTION_SIZE_BYTES, dstVbo, (long)i * (long)SECTION_SIZE_BYTES);
      }

      this.changed.clear();
   }

   public void upload(GlBuffer buffer) {
      if (!this.changed.isEmpty()) {
         buffer.upload(this.arena.indexToPointer(0), (long)this.arena.capacity() * (long)SECTION_SIZE_BYTES);
         this.changed.clear();
      }
   }

   public IntArrayList createLut() {
      return this.sableLut.flatten();
   }

   public class DebugVisual implements EffectVisual<LightStorage>, SimpleDynamicVisual {
      private final InstanceRecycler<TransformedInstance> boxes;
      private final Vec3i renderOrigin;

      public DebugVisual(final VisualizationContext ctx, final float partialTick) {
         this.renderOrigin = ctx.renderOrigin();
         this.boxes = new InstanceRecycler(
            () -> (TransformedInstance)ctx.instancerProvider().instancer(InstanceTypes.TRANSFORMED, HitboxComponent.BOX_MODEL).createInstance()
         );
      }

      public void beginFrame(Context ctx) {
         this.boxes.resetCount();
         this.setupSectionBoxes();
         this.setupLutRangeBoxes();
         this.boxes.discardExtra();
      }

      private void setupSectionBoxes() {
         ObjectIterator var1 = SableFlywheelLightStorage.this.scene2SectionArenaIndexMap.int2ObjectEntrySet().iterator();

         while (var1.hasNext()) {
            Entry<Long2IntMap> entry = (Entry<Long2IntMap>)var1.next();
            int sceneId = entry.getIntKey();
            Long2IntMap section2ArenaIndex = (Long2IntMap)entry.getValue();
            section2ArenaIndex.keySet()
               .forEach(
                  l -> {
                     int x = SectionPos.x(l) * 16 - this.renderOrigin.getX();
                     int y = SectionPos.y(l) * 16 - this.renderOrigin.getY();
                     int z = SectionPos.z(l) * 16 - this.renderOrigin.getZ();
                     TransformedInstance instance = (TransformedInstance)this.boxes.get();
                     ((TransformedInstance)instance.setIdentityTransform().translate((float)(x + 1), (float)(y + 1), (float)(z + 1)).scale(14.0F))
                        .color(255, 255, sceneId * 64)
                        .light(15728880)
                        .setChanged();
                  }
               );
         }
      }

      private void setupLutRangeBoxes() {
         SableLightLut.Layer<SableLightLut.Layer<SableLightLut.Layer<SableLightLut.IntLayer>>> first = SableFlywheelLightStorage.this.sableLut.indices;
         int base1 = first.base();
         int size1 = first.size();
         float debug1 = (float)(base1 * 16 - this.renderOrigin.getY());
         float min2 = Float.POSITIVE_INFINITY;
         float max2 = Float.NEGATIVE_INFINITY;
         float min3 = Float.POSITIVE_INFINITY;
         float max3 = Float.NEGATIVE_INFINITY;

         for (int y = 0; y < size1; y++) {
            SableLightLut.Layer<SableLightLut.Layer<SableLightLut.IntLayer>> second = first.getRaw(y);
            if (second != null) {
               int base2 = second.base();
               int size2 = second.size();
               float y2 = (float)((base1 + y) * 16 - this.renderOrigin.getY()) + 7.5F;
               min2 = Math.min(min2, (float)base2);
               max2 = Math.max(max2, (float)(base2 + size2));
               float minLocal3 = Float.POSITIVE_INFINITY;
               float maxLocal3 = Float.NEGATIVE_INFINITY;
               float debug2 = (float)(base2 * 16 - this.renderOrigin.getX());

               for (int x = 0; x < size2; x++) {
                  SableLightLut.Layer<SableLightLut.IntLayer> third = second.getRaw(x);
                  if (third != null) {
                     int base3 = third.base();
                     int size3 = third.size();
                     float x2 = (float)((base2 + x) * 16 - this.renderOrigin.getX()) + 7.5F;
                     min3 = Math.min(min3, (float)base3);
                     max3 = Math.max(max3, (float)(base3 + size3));
                     minLocal3 = Math.min(minLocal3, (float)base3);
                     maxLocal3 = Math.max(maxLocal3, (float)(base3 + size3));
                     float debug3 = (float)(base3 * 16 - this.renderOrigin.getZ());

                     for (int z = 0; z < size3; z++) {
                        ((TransformedInstance)this.boxes.get())
                           .setIdentityTransform()
                           .translate(x2, y2, debug3)
                           .scale(1.0F, 1.0F, (float)(size3 * 16))
                           .color(0, 0, 255)
                           .light(15728880)
                           .setChanged();
                     }
                  }
               }

               ((TransformedInstance)this.boxes.get())
                  .setIdentityTransform()
                  .translate(debug2, y2, minLocal3 * 16.0F - (float)this.renderOrigin.getZ())
                  .scale((float)(size2 * 16), 1.0F, (maxLocal3 - minLocal3) * 16.0F)
                  .color(255, 0, 0)
                  .light(15728880)
                  .setChanged();
            }
         }

         ((TransformedInstance)this.boxes.get())
            .setIdentityTransform()
            .translate(min2 * 16.0F - (float)this.renderOrigin.getX(), debug1, min3 * 16.0F - (float)this.renderOrigin.getZ())
            .scale((max2 - min2) * 16.0F, (float)(size1 * 16), (max3 - min3) * 16.0F)
            .color(0, 255, 0)
            .light(15728880)
            .setChanged();
      }

      public void update(float partialTick) {
      }

      public void delete() {
         this.boxes.delete();
      }
   }
}
