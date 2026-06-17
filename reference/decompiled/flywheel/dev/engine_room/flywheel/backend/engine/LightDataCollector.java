package dev.engine_room.flywheel.backend.engine;

import dev.engine_room.flywheel.backend.SkyLightSectionStorageExtension;
import dev.engine_room.flywheel.backend.mixin.light.LayerLightSectionStorageAccessor;
import dev.engine_room.flywheel.backend.mixin.light.LightEngineAccessor;
import it.unimi.dsi.fastutil.longs.Long2ObjectFunction;
import java.util.BitSet;
import java.util.Objects;
import net.minecraft.core.SectionPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.lighting.LayerLightEventListener;
import net.minecraft.world.level.lighting.LayerLightEventListener.DummyLightLayerEventListener;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

public abstract class LightDataCollector {
   private static final LightDataCollector.ConstantDataLayer ALWAYS_0 = new LightDataCollector.ConstantDataLayer(0);
   private static final LightDataCollector.ConstantDataLayer ALWAYS_15 = new LightDataCollector.ConstantDataLayer(15);
   protected final LevelAccessor level;
   protected final LayerLightEventListener skyLayerListener;
   protected final LayerLightEventListener blockLayerListener;

   protected LightDataCollector(LevelAccessor level, LayerLightEventListener skyLayerListener, LayerLightEventListener blockLayerListener) {
      this.level = level;
      this.skyLayerListener = skyLayerListener;
      this.blockLayerListener = blockLayerListener;
   }

   public static LightDataCollector of(LevelAccessor level) {
      LayerLightEventListener skyLayerListener = level.getLightEngine().getLayerListener(LightLayer.SKY);
      LayerLightEventListener blockLayerListener = level.getLightEngine().getLayerListener(LightLayer.BLOCK);
      Long2ObjectFunction<DataLayer> fastSkyDataGetter = createFastSkyDataGetter(skyLayerListener);
      Long2ObjectFunction<DataLayer> fastBlockDataGetter = createFastBlockDataGetter(blockLayerListener);
      return (LightDataCollector)(fastSkyDataGetter != null && fastBlockDataGetter != null
         ? new LightDataCollector.Fast(level, skyLayerListener, blockLayerListener, fastSkyDataGetter, fastBlockDataGetter)
         : new LightDataCollector.Slow(level, skyLayerListener, blockLayerListener));
   }

   @Nullable
   private static Long2ObjectFunction<DataLayer> createFastSkyDataGetter(LayerLightEventListener layerListener) {
      if (layerListener == DummyLightLayerEventListener.INSTANCE) {
         return section -> ALWAYS_0;
      } else {
         if (layerListener instanceof LightEngineAccessor<?, ?> accessor && accessor.flywheel$storage() instanceof SkyLightSectionStorageExtension skyStorage) {
            return section -> {
               DataLayer out = skyStorage.flywheel$skyDataLayer(section);
               return Objects.requireNonNullElse(out, ALWAYS_15);
            };
         }

         return null;
      }
   }

   @Nullable
   private static Long2ObjectFunction<DataLayer> createFastBlockDataGetter(LayerLightEventListener layerListener) {
      if (layerListener == DummyLightLayerEventListener.INSTANCE) {
         return section -> ALWAYS_0;
      } else {
         if (layerListener instanceof LightEngineAccessor<?, ?> accessor && accessor.flywheel$storage() instanceof LayerLightSectionStorageAccessor storage) {
            return section -> {
               DataLayer out = storage.flywheel$callGetDataLayer(section, false);
               return Objects.requireNonNullElse(out, ALWAYS_0);
            };
         }

         return null;
      }
   }

   public void collectSection(long ptr, long section) {
      this.collectSolidData(ptr, section);
      this.collectLightData(ptr, section);
   }

   private void collectSolidData(long ptr, long section) {
      MutableBlockPos blockPos = new MutableBlockPos();
      int xMin = SectionPos.sectionToBlockCoord(SectionPos.x(section));
      int yMin = SectionPos.sectionToBlockCoord(SectionPos.y(section));
      int zMin = SectionPos.sectionToBlockCoord(SectionPos.z(section));
      BitSet bitSet = new BitSet(5832);
      int index = 0;

      for (int y = -1; y < 17; y++) {
         for (int z = -1; z < 17; z++) {
            for (int x = -1; x < 17; x++) {
               blockPos.set(xMin + x, yMin + y, zMin + z);
               BlockState blockState = this.level.getBlockState(blockPos);
               if (blockState.canOcclude() && blockState.isCollisionShapeFullBlock(this.level, blockPos)) {
                  bitSet.set(index);
               }

               index++;
            }
         }
      }

      long[] longArray = bitSet.toLongArray();

      for (long l : longArray) {
         MemoryUtil.memPutLong(ptr, l);
         ptr += 8L;
      }
   }

   protected abstract void collectLightData(long var1, long var3);

   protected static void write(long ptr, int x, int y, int z, int block, int sky) {
      int x1 = x + 1;
      int y1 = y + 1;
      int z1 = z + 1;
      int offset = x1 + z1 * 18 + y1 * 18 * 18;
      long packedByte = (long)(block & 15 | (sky & 15) << 4);
      MemoryUtil.memPutByte(ptr + (long)LightStorage.SOLID_SIZE_BYTES + (long)offset, (byte)((int)packedByte));
   }

   private static class ConstantDataLayer extends DataLayer {
      private final int value;

      private ConstantDataLayer(int value) {
         this.value = value;
      }

      public int get(int x, int y, int z) {
         return this.value;
      }
   }

   private static class Fast extends LightDataCollector {
      private final Long2ObjectFunction<DataLayer> skyDataGetter;
      private final Long2ObjectFunction<DataLayer> blockDataGetter;

      public Fast(
         LevelAccessor level,
         LayerLightEventListener skyLayerListener,
         LayerLightEventListener blockLayerListener,
         Long2ObjectFunction<DataLayer> skyDataGetter,
         Long2ObjectFunction<DataLayer> blockDataGetter
      ) {
         super(level, skyLayerListener, blockLayerListener);
         this.skyDataGetter = skyDataGetter;
         this.blockDataGetter = blockDataGetter;
      }

      @Override
      protected void collectLightData(long ptr, long section) {
         this.collectCenter(ptr, section);

         for (LightDataCollector.Fast.SectionEdge i : LightDataCollector.Fast.SectionEdge.VALUES) {
            this.collectYZPlane(ptr, SectionPos.offset(section, i.sectionOffset, 0, 0), i);
            this.collectXZPlane(ptr, SectionPos.offset(section, 0, i.sectionOffset, 0), i);
            this.collectXYPlane(ptr, SectionPos.offset(section, 0, 0, i.sectionOffset), i);

            for (LightDataCollector.Fast.SectionEdge j : LightDataCollector.Fast.SectionEdge.VALUES) {
               this.collectXStrip(ptr, SectionPos.offset(section, 0, i.sectionOffset, j.sectionOffset), i, j);
               this.collectYStrip(ptr, SectionPos.offset(section, i.sectionOffset, 0, j.sectionOffset), i, j);
               this.collectZStrip(ptr, SectionPos.offset(section, i.sectionOffset, j.sectionOffset, 0), i, j);
            }
         }

         this.collectCorners(ptr, section);
      }

      private void collectXStrip(long ptr, long section, LightDataCollector.Fast.SectionEdge y, LightDataCollector.Fast.SectionEdge z) {
         DataLayer blockData = (DataLayer)this.blockDataGetter.get(section);
         DataLayer skyData = (DataLayer)this.skyDataGetter.get(section);

         for (int x = 0; x < 16; x++) {
            write(ptr, x, y.relative, z.relative, blockData.get(x, y.pos, z.pos), skyData.get(x, y.pos, z.pos));
         }
      }

      private void collectYStrip(long ptr, long section, LightDataCollector.Fast.SectionEdge x, LightDataCollector.Fast.SectionEdge z) {
         DataLayer blockData = (DataLayer)this.blockDataGetter.get(section);
         DataLayer skyData = (DataLayer)this.skyDataGetter.get(section);

         for (int y = 0; y < 16; y++) {
            write(ptr, x.relative, y, z.relative, blockData.get(x.pos, y, z.pos), skyData.get(x.pos, y, z.pos));
         }
      }

      private void collectZStrip(long ptr, long section, LightDataCollector.Fast.SectionEdge x, LightDataCollector.Fast.SectionEdge y) {
         DataLayer blockData = (DataLayer)this.blockDataGetter.get(section);
         DataLayer skyData = (DataLayer)this.skyDataGetter.get(section);

         for (int z = 0; z < 16; z++) {
            write(ptr, x.relative, y.relative, z, blockData.get(x.pos, y.pos, z), skyData.get(x.pos, y.pos, z));
         }
      }

      private void collectYZPlane(long ptr, long section, LightDataCollector.Fast.SectionEdge x) {
         DataLayer blockData = (DataLayer)this.blockDataGetter.get(section);
         DataLayer skyData = (DataLayer)this.skyDataGetter.get(section);

         for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
               write(ptr, x.relative, y, z, blockData.get(x.pos, y, z), skyData.get(x.pos, y, z));
            }
         }
      }

      private void collectXZPlane(long ptr, long section, LightDataCollector.Fast.SectionEdge y) {
         DataLayer blockData = (DataLayer)this.blockDataGetter.get(section);
         DataLayer skyData = (DataLayer)this.skyDataGetter.get(section);

         for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
               write(ptr, x, y.relative, z, blockData.get(x, y.pos, z), skyData.get(x, y.pos, z));
            }
         }
      }

      private void collectXYPlane(long ptr, long section, LightDataCollector.Fast.SectionEdge z) {
         DataLayer blockData = (DataLayer)this.blockDataGetter.get(section);
         DataLayer skyData = (DataLayer)this.skyDataGetter.get(section);

         for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
               write(ptr, x, y, z.relative, blockData.get(x, y, z.pos), skyData.get(x, y, z.pos));
            }
         }
      }

      private void collectCenter(long ptr, long section) {
         DataLayer blockData = (DataLayer)this.blockDataGetter.get(section);
         DataLayer skyData = (DataLayer)this.skyDataGetter.get(section);

         for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
               for (int x = 0; x < 16; x++) {
                  write(ptr, x, y, z, blockData.get(x, y, z), skyData.get(x, y, z));
               }
            }
         }
      }

      private void collectCorners(long ptr, long section) {
         LayerLightEventListener blockLayerListener = this.blockLayerListener;
         LayerLightEventListener skyLayerListener = this.skyLayerListener;
         MutableBlockPos blockPos = new MutableBlockPos();
         int xMin = SectionPos.sectionToBlockCoord(SectionPos.x(section));
         int yMin = SectionPos.sectionToBlockCoord(SectionPos.y(section));
         int zMin = SectionPos.sectionToBlockCoord(SectionPos.z(section));

         for (LightDataCollector.Fast.SectionEdge y : LightDataCollector.Fast.SectionEdge.VALUES) {
            for (LightDataCollector.Fast.SectionEdge z : LightDataCollector.Fast.SectionEdge.VALUES) {
               for (LightDataCollector.Fast.SectionEdge x : LightDataCollector.Fast.SectionEdge.VALUES) {
                  blockPos.set(x.relative + xMin, y.relative + yMin, z.relative + zMin);
                  write(ptr, x.relative, y.relative, z.relative, blockLayerListener.getLightValue(blockPos), skyLayerListener.getLightValue(blockPos));
               }
            }
         }
      }

      private static enum SectionEdge {
         LOW(15, -1, -1),
         HIGH(0, 16, 1);

         public static final LightDataCollector.Fast.SectionEdge[] VALUES = values();
         private final int pos;
         private final int relative;
         private final int sectionOffset;

         private SectionEdge(int pos, int relative, int sectionOffset) {
            this.pos = pos;
            this.relative = relative;
            this.sectionOffset = sectionOffset;
         }
      }
   }

   private static class Slow extends LightDataCollector {
      public Slow(LevelAccessor level, LayerLightEventListener skyLayerListener, LayerLightEventListener blockLayerListener) {
         super(level, skyLayerListener, blockLayerListener);
      }

      @Override
      protected void collectLightData(long ptr, long section) {
         LayerLightEventListener blockLayerListener = this.blockLayerListener;
         LayerLightEventListener skyLayerListener = this.skyLayerListener;
         MutableBlockPos blockPos = new MutableBlockPos();
         int xMin = SectionPos.sectionToBlockCoord(SectionPos.x(section));
         int yMin = SectionPos.sectionToBlockCoord(SectionPos.y(section));
         int zMin = SectionPos.sectionToBlockCoord(SectionPos.z(section));

         for (int y = -1; y < 17; y++) {
            for (int z = -1; z < 17; z++) {
               for (int x = -1; x < 17; x++) {
                  blockPos.set(xMin + x, yMin + y, zMin + z);
                  write(ptr, x, y, z, blockLayerListener.getLightValue(blockPos), skyLayerListener.getLightValue(blockPos));
               }
            }
         }
      }
   }
}
