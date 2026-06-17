package dev.engine_room.flywheel.backend.engine;

import dev.engine_room.flywheel.api.task.Plan;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.Effect;
import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.backend.BackendDebugFlags;
import dev.engine_room.flywheel.backend.engine.indirect.StagingBuffer;
import dev.engine_room.flywheel.backend.gl.buffer.GlBuffer;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.math.MoreMath;
import dev.engine_room.flywheel.lib.task.SimplePlan;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import dev.engine_room.flywheel.lib.visual.component.HitboxComponent;
import dev.engine_room.flywheel.lib.visual.util.InstanceRecycler;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.Long2IntMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import java.util.BitSet;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.LevelAccessor;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

public class LightStorage implements Effect {
   public static final int BLOCKS_PER_SECTION = 5832;
   public static final int LIGHT_SIZE_BYTES = 5832;
   public static final int SOLID_SIZE_BYTES = MoreMath.ceilingDiv(5832, 32) * 4;
   public static final int SECTION_SIZE_BYTES = SOLID_SIZE_BYTES + 5832;
   private static final int DEFAULT_ARENA_CAPACITY_SECTIONS = 64;
   private static final int INVALID_SECTION = -1;
   private final LevelAccessor level;
   private final LightLut lut;
   public final CpuArena arena;
   private final Long2IntMap section2ArenaIndex;
   private final LightDataCollector collector;
   private final BitSet changed = new BitSet();
   private boolean needsLutRebuild = false;
   private boolean isDebugOn = false;
   private final LongSet updatedSections = new LongOpenHashSet();
   @Nullable
   private LongSet requestedSections;

   public LightStorage(LevelAccessor level) {
      this.level = level;
      this.lut = new LightLut();
      this.arena = new CpuArena((long)SECTION_SIZE_BYTES, 64);
      this.section2ArenaIndex = new Long2IntOpenHashMap();
      this.section2ArenaIndex.defaultReturnValue(-1);
      this.collector = LightDataCollector.of(level);
   }

   @Override
   public LevelAccessor level() {
      return this.level;
   }

   @Override
   public EffectVisual<?> visualize(VisualizationContext ctx, float partialTick) {
      return new LightStorage.DebugVisual(ctx, partialTick);
   }

   public void sections(LongSet sections) {
      this.requestedSections = sections;
   }

   public void onLightUpdate(long section) {
      this.updatedSections.add(section);
   }

   public <C> Plan<C> createFramePlan() {
      return SimplePlan.of(() -> {
         if (BackendDebugFlags.LIGHT_STORAGE_VIEW != this.isDebugOn) {
            VisualizationManager visualizationManager = VisualizationManager.get(this.level);
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
            this.removeUnusedSections();
            LongSet sectionsToCollect;
            if (this.requestedSections == null) {
               sectionsToCollect = new LongOpenHashSet();
            } else {
               sectionsToCollect = new LongOpenHashSet(this.requestedSections);
               sectionsToCollect.removeAll(this.section2ArenaIndex.keySet());
            }

            LongIterator var2 = this.updatedSections.iterator();

            while (var2.hasNext()) {
               long updatedSection = (Long)var2.next();

               for (int x = -1; x <= 1; x++) {
                  for (int y = -1; y <= 1; y++) {
                     for (int z = -1; z <= 1; z++) {
                        long section = SectionPos.offset(updatedSection, x, y, z);
                        if (this.section2ArenaIndex.containsKey(section)) {
                           sectionsToCollect.add(section);
                        }
                     }
                  }
               }
            }

            sectionsToCollect.forEach(this::collectSection);
            this.updatedSections.clear();
            this.requestedSections = null;
         }
      });
   }

   private void removeUnusedSections() {
      if (this.requestedSections != null) {
         boolean anyRemoved = false;
         ObjectSet<Entry> entries = this.section2ArenaIndex.long2IntEntrySet();
         ObjectIterator<Entry> it = entries.iterator();

         while (it.hasNext()) {
            Entry entry = (Entry)it.next();
            long section = entry.getLongKey();
            if (!this.requestedSections.contains(section)) {
               this.arena.free(entry.getIntValue());
               this.endTrackingSection(section);
               it.remove();
               anyRemoved = true;
            }
         }

         if (anyRemoved) {
            this.lut.prune();
            this.needsLutRebuild = true;
         }
      }
   }

   private void beginTrackingSection(long section, int index) {
      this.lut.add(section, index);
      this.needsLutRebuild = true;
   }

   private void endTrackingSection(long section) {
      this.lut.remove(section);
      this.needsLutRebuild = true;
   }

   public int capacity() {
      return this.arena.capacity();
   }

   public void collectSection(long section) {
      int index = this.indexForSection(section);
      this.changed.set(index);
      long ptr = this.arena.indexToPointer(index);
      MemoryUtil.memSet(ptr, 0, (long)SECTION_SIZE_BYTES);
      this.collector.collectSection(ptr, section);
   }

   private int indexForSection(long section) {
      int out = this.section2ArenaIndex.get(section);
      if (out == -1) {
         out = this.arena.alloc();
         this.section2ArenaIndex.put(section, out);
         this.beginTrackingSection(section, out);
      }

      return out;
   }

   public void delete() {
      this.arena.delete();
   }

   public boolean checkNeedsLutRebuildAndClear() {
      boolean out = this.needsLutRebuild;
      this.needsLutRebuild = false;
      return out;
   }

   public void uploadChangedSections(StagingBuffer staging, int dstVbo) {
      for (int i = this.changed.nextSetBit(0); i >= 0; i = this.changed.nextSetBit(i + 1)) {
         staging.enqueueCopy(this.arena.indexToPointer(i), (long)SECTION_SIZE_BYTES, dstVbo, (long)(i * SECTION_SIZE_BYTES));
      }

      this.changed.clear();
   }

   public void upload(GlBuffer buffer) {
      if (!this.changed.isEmpty()) {
         buffer.upload(this.arena.indexToPointer(0), (long)(this.arena.capacity() * SECTION_SIZE_BYTES));
         this.changed.clear();
      }
   }

   public IntArrayList createLut() {
      return this.lut.flatten();
   }

   public class DebugVisual implements EffectVisual<LightStorage>, SimpleDynamicVisual {
      private final InstanceRecycler<TransformedInstance> boxes;
      private final Vec3i renderOrigin;

      public DebugVisual(VisualizationContext ctx, float partialTick) {
         this.renderOrigin = ctx.renderOrigin();
         this.boxes = new InstanceRecycler<>(() -> ctx.instancerProvider().instancer(InstanceTypes.TRANSFORMED, HitboxComponent.BOX_MODEL).createInstance());
      }

      @Override
      public void beginFrame(DynamicVisual.Context ctx) {
         this.boxes.resetCount();
         this.setupSectionBoxes();
         this.setupLutRangeBoxes();
         this.boxes.discardExtra();
      }

      private void setupSectionBoxes() {
         LightStorage.this.section2ArenaIndex
            .keySet()
            .forEach(
               l -> {
                  int x = SectionPos.x(l) * 16 - this.renderOrigin.getX();
                  int y = SectionPos.y(l) * 16 - this.renderOrigin.getY();
                  int z = SectionPos.z(l) * 16 - this.renderOrigin.getZ();
                  TransformedInstance instance = this.boxes.get();
                  instance.setIdentityTransform()
                     .translate((float)(x + 1), (float)(y + 1), (float)(z + 1))
                     .scale(14.0F)
                     .color(255, 255, 0)
                     .light(15728880)
                     .setChanged();
               }
            );
      }

      private void setupLutRangeBoxes() {
         LightLut.Layer<LightLut.Layer<LightLut.IntLayer>> first = LightStorage.this.lut.indices;
         int base1 = first.base();
         int size1 = first.size();
         float debug1 = (float)(base1 * 16 - this.renderOrigin.getY());
         float min2 = Float.POSITIVE_INFINITY;
         float max2 = Float.NEGATIVE_INFINITY;
         float min3 = Float.POSITIVE_INFINITY;
         float max3 = Float.NEGATIVE_INFINITY;

         for (int y = 0; y < size1; y++) {
            LightLut.Layer<LightLut.IntLayer> second = first.getRaw(y);
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
                  LightLut.IntLayer third = second.getRaw(x);
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
                        this.boxes
                           .get()
                           .setIdentityTransform()
                           .translate(x2, y2, debug3)
                           .scale(1.0F, 1.0F, (float)(size3 * 16))
                           .color(0, 0, 255)
                           .light(15728880)
                           .setChanged();
                     }
                  }
               }

               this.boxes
                  .get()
                  .setIdentityTransform()
                  .translate(debug2, y2, minLocal3 * 16.0F - (float)this.renderOrigin.getZ())
                  .scale((float)(size2 * 16), 1.0F, (maxLocal3 - minLocal3) * 16.0F)
                  .color(255, 0, 0)
                  .light(15728880)
                  .setChanged();
            }
         }

         this.boxes
            .get()
            .setIdentityTransform()
            .translate(min2 * 16.0F - (float)this.renderOrigin.getX(), debug1, min3 * 16.0F - (float)this.renderOrigin.getZ())
            .scale((max2 - min2) * 16.0F, (float)(size1 * 16), (max3 - min3) * 16.0F)
            .color(0, 255, 0)
            .light(15728880)
            .setChanged();
      }

      @Override
      public void update(float partialTick) {
      }

      @Override
      public void delete() {
         this.boxes.delete();
      }
   }
}
