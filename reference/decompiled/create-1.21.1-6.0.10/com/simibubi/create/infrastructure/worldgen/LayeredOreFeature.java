package com.simibubi.create.infrastructure.worldgen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.BulkSectionAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration.TargetBlockState;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

public class LayeredOreFeature extends Feature<LayeredOreConfiguration> {
   private static final float MAX_LAYER_DISPLACEMENT = 1.75F;
   private static final float LAYER_NOISE_FREQUENCY = 0.125F;
   private static final float MAX_RADIAL_THRESHOLD_REDUCTION = 0.25F;
   private static final float RADIAL_NOISE_FREQUENCY = 0.125F;

   public LayeredOreFeature() {
      super(LayeredOreConfiguration.CODEC);
   }

   public boolean place(FeaturePlaceContext<LayeredOreConfiguration> pContext) {
      RandomSource random = pContext.random();
      BlockPos origin = pContext.origin();
      WorldGenLevel worldGenLevel = pContext.level();
      LayeredOreConfiguration config = (LayeredOreConfiguration)pContext.config();
      List<LayerPattern> patternPool = config.layerPatterns;
      if (patternPool.isEmpty()) {
         return false;
      } else {
         LayerPattern layerPattern = patternPool.get(random.nextInt(patternPool.size()));
         int placedAmount = 0;
         int size = config.size + 1;
         float radius = (float)config.size * 0.5F;
         int radiusBound = Mth.ceil(radius) - 1;
         int x0 = origin.getX();
         int y0 = origin.getY();
         int z0 = origin.getZ();
         if (origin.getY() >= worldGenLevel.getHeight(Types.OCEAN_FLOOR_WG, origin.getX(), origin.getZ())) {
            return false;
         } else {
            List<LayeredOreFeature.TemporaryLayerEntry> tempLayers = new ArrayList<>();
            float layerSizeTotal = 0.0F;
            LayerPattern.Layer current = null;

            while (layerSizeTotal < (float)size) {
               LayerPattern.Layer next = layerPattern.rollNext(current, random);
               float layerSize = Mth.randomBetween(random, (float)next.minSize, (float)next.maxSize);
               tempLayers.add(new LayeredOreFeature.TemporaryLayerEntry(next, layerSize));
               layerSizeTotal += layerSize;
               current = next;
            }

            List<LayeredOreFeature.ResolvedLayerEntry> resolvedLayers = new ArrayList<>(tempLayers.size());
            float cumulativeLayerSize = -(layerSizeTotal - (float)size) * random.nextFloat();

            for (LayeredOreFeature.TemporaryLayerEntry tempLayerEntry : tempLayers) {
               float rampStartValue = resolvedLayers.size() == 0 ? Float.NEGATIVE_INFINITY : cumulativeLayerSize * (2.0F / (float)size) - 1.0F;
               cumulativeLayerSize += tempLayerEntry.size();
               if (!(cumulativeLayerSize < 0.0F)) {
                  float radialThresholdMultiplier = Mth.randomBetween(random, 0.5F, 1.0F);
                  resolvedLayers.add(new LayeredOreFeature.ResolvedLayerEntry(tempLayerEntry.layer, radialThresholdMultiplier, rampStartValue));
               }
            }

            float gy = Mth.randomBetween(random, -1.0F, 1.0F);
            gy = (float)Math.cbrt((double)gy);
            float xzRescale = Mth.sqrt(1.0F - gy * gy);
            float theta = random.nextFloat() * (float) (Math.PI * 2);
            float gx = Mth.cos(theta) * xzRescale;
            float gz = Mth.sin(theta) * xzRescale;
            SimplexNoise layerDisplacementNoise = new SimplexNoise(random);
            SimplexNoise radiusNoise = new SimplexNoise(random);
            MutableBlockPos mutablePos = new MutableBlockPos();
            BulkSectionAccess bulkSectionAccess = new BulkSectionAccess(worldGenLevel);

            try {
               for (int dzBlock = -radiusBound; dzBlock <= radiusBound; dzBlock++) {
                  float dz = (float)dzBlock * (1.0F / radius);
                  if (!(dz * dz > 1.0F)) {
                     for (int dxBlock = -radiusBound; dxBlock <= radiusBound; dxBlock++) {
                        float dx = (float)dxBlock * (1.0F / radius);
                        if (!(dz * dz + dx * dx > 1.0F)) {
                           for (int dyBlock = -radiusBound; dyBlock <= radiusBound; dyBlock++) {
                              float dy = (float)dyBlock * (1.0F / radius);
                              float distanceSquared = dz * dz + dx * dx + dy * dy;
                              if (!(distanceSquared > 1.0F) && !worldGenLevel.isOutsideBuildHeight(y0 + dyBlock)) {
                                 int currentX = x0 + dxBlock;
                                 int currentY = y0 + dyBlock;
                                 int currentZ = z0 + dzBlock;
                                 float rampValue = gx * dx + gy * dy + gz * dz;
                                 rampValue = (float)(
                                    (double)rampValue
                                       + layerDisplacementNoise.getValue(
                                             (double)((float)currentX * 0.125F), (double)((float)currentY * 0.125F), (double)((float)currentZ * 0.125F)
                                          )
                                          * (double)(1.75F / (float)size)
                                 );
                                 int layerIndex = Collections.binarySearch(resolvedLayers, new LayeredOreFeature.ResolvedLayerEntry(null, 0.0F, rampValue));
                                 if (layerIndex < 0) {
                                    layerIndex = -2 - layerIndex;
                                 }

                                 LayeredOreFeature.ResolvedLayerEntry layerEntry = resolvedLayers.get(layerIndex);
                                 if (!(distanceSquared > layerEntry.radialThresholdMultiplier)) {
                                    float thresholdNoiseValue = Mth.map(
                                       (float)radiusNoise.getValue(
                                          (double)((float)currentX * 0.125F), (double)((float)currentY * 0.125F), (double)((float)currentZ * 0.125F)
                                       ),
                                       -1.0F,
                                       1.0F,
                                       0.75F,
                                       1.0F
                                    );
                                    if (!(distanceSquared > layerEntry.radialThresholdMultiplier * thresholdNoiseValue)) {
                                       LayerPattern.Layer layer = layerEntry.layer;
                                       List<TargetBlockState> targetBlockStates = layer.rollBlock(random);
                                       mutablePos.set(currentX, currentY, currentZ);
                                       if (worldGenLevel.ensureCanWrite(mutablePos)) {
                                          LevelChunkSection levelChunkSection = bulkSectionAccess.getSection(mutablePos);
                                          if (levelChunkSection != null) {
                                             int localX = SectionPos.sectionRelative(currentX);
                                             int localY = SectionPos.sectionRelative(currentY);
                                             int localZ = SectionPos.sectionRelative(currentZ);
                                             BlockState blockState = levelChunkSection.getBlockState(localX, localY, localZ);

                                             for (TargetBlockState targetBlockState : targetBlockStates) {
                                                if (this.canPlaceOre(blockState, bulkSectionAccess::getBlockState, random, config, targetBlockState, mutablePos)
                                                   && !targetBlockState.state.isAir()) {
                                                   levelChunkSection.setBlockState(localX, localY, localZ, targetBlockState.state, false);
                                                   placedAmount++;
                                                   break;
                                                }
                                             }
                                          }
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            } catch (Throwable var53) {
               try {
                  bulkSectionAccess.close();
               } catch (Throwable var52) {
                  var53.addSuppressed(var52);
               }

               throw var53;
            }

            bulkSectionAccess.close();
            return placedAmount > 0;
         }
      }
   }

   public boolean canPlaceOre(
      BlockState pState,
      Function<BlockPos, BlockState> pAdjacentStateAccessor,
      RandomSource pRandom,
      LayeredOreConfiguration pConfig,
      TargetBlockState pTargetState,
      MutableBlockPos pMatablePos
   ) {
      if (!pTargetState.target.test(pState, pRandom)) {
         return false;
      } else {
         return this.shouldSkipAirCheck(pRandom, pConfig.discardChanceOnAirExposure) ? true : !isAdjacentToAir(pAdjacentStateAccessor, pMatablePos);
      }
   }

   protected boolean shouldSkipAirCheck(RandomSource pRandom, float pChance) {
      return pChance <= 0.0F ? true : (pChance >= 1.0F ? false : pRandom.nextFloat() >= pChance);
   }

   private static record ResolvedLayerEntry(LayerPattern.Layer layer, float radialThresholdMultiplier, float rampStartValue)
      implements Comparable<LayeredOreFeature.ResolvedLayerEntry> {
      public int compareTo(LayeredOreFeature.ResolvedLayerEntry b) {
         return Float.compare(this.rampStartValue, b.rampStartValue);
      }
   }

   private static record TemporaryLayerEntry(LayerPattern.Layer layer, float size) {
   }
}
