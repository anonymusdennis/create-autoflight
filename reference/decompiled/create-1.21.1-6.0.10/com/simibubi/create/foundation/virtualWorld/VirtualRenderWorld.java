package com.simibubi.create.foundation.virtualWorld;

import dev.engine_room.flywheel.api.visualization.VisualizationLevel;
import it.unimi.dsi.fastutil.objects.Object2ShortMap;
import it.unimi.dsi.fastutil.objects.Object2ShortOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEvent.Context;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.ticks.LevelTickAccess;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VirtualRenderWorld extends Level implements VisualizationLevel {
   protected final Level level;
   protected final int minBuildHeight;
   protected final int height;
   protected final Vec3i biomeOffset;
   protected final VirtualChunkSource chunkSource;
   protected final LevelLightEngine lightEngine;
   protected final Map<BlockPos, BlockState> blockStates = new HashMap<>();
   protected final Map<BlockPos, BlockEntity> blockEntities = new HashMap<>();
   protected final Object2ShortMap<SectionPos> nonEmptyBlockCounts = new Object2ShortOpenHashMap();
   protected final LevelEntityGetter<Entity> entityGetter = new VirtualLevelEntityGetter();
   protected final MutableBlockPos scratchPos = new MutableBlockPos();
   protected final Runnable onBlockUpdated;
   private int externalPackedLight = 0;

   public VirtualRenderWorld(Level level, int minBuildHeight, int height, Vec3i biomeOffset, Runnable onBlockUpdated) {
      super(
         (WritableLevelData)level.getLevelData(),
         level.dimension(),
         level.registryAccess(),
         level.dimensionTypeRegistration(),
         level.getProfilerSupplier(),
         true,
         false,
         0L,
         0
      );
      this.level = level;
      this.minBuildHeight = nextMultipleOf16(minBuildHeight);
      this.height = nextMultipleOf16(height);
      this.biomeOffset = biomeOffset;
      this.chunkSource = new VirtualChunkSource(this);
      this.lightEngine = new LevelLightEngine(this.chunkSource, true, false);
      this.onBlockUpdated = onBlockUpdated;
   }

   public static int nextMultipleOf16(int a) {
      return a < 0 ? -((Math.abs(a) - 1 | 15) + 1) : (a - 1 | 15) + 1;
   }

   public void setExternalLight(int packedLight) {
      this.externalPackedLight = packedLight;
   }

   public void resetExternalLight() {
      this.externalPackedLight = 0;
   }

   public void sendBlockUpdated(BlockPos pos, BlockState oldState, BlockState newState, int flags) {
      this.onBlockUpdated.run();
   }

   public int getBrightness(LightLayer lightType, BlockPos blockPos) {
      int selfBrightness = super.getBrightness(lightType, blockPos);
      return lightType == LightLayer.SKY
         ? Math.max(selfBrightness, LightTexture.sky(this.externalPackedLight))
         : Math.max(selfBrightness, LightTexture.block(this.externalPackedLight));
   }

   public void clear() {
      this.blockStates.clear();
      this.blockEntities.clear();
      this.nonEmptyBlockCounts.forEach((sectionPos, nonEmptyBlockCount) -> {
         if (nonEmptyBlockCount > 0) {
            this.lightEngine.updateSectionStatus(sectionPos, true);
         }
      });
      this.nonEmptyBlockCounts.clear();
   }

   public void setBlockEntities(Collection<BlockEntity> blockEntities) {
      this.blockEntities.clear();
      blockEntities.forEach(this::setBlockEntity);
   }

   public void runLightEngine() {
      Set<ChunkPos> chunkPosSet = new ObjectOpenHashSet();
      this.nonEmptyBlockCounts.object2ShortEntrySet().forEach(entry -> {
         if (entry.getShortValue() > 0) {
            chunkPosSet.add(((SectionPos)entry.getKey()).chunk());
         }
      });

      for (ChunkPos chunkPos : chunkPosSet) {
         this.lightEngine.propagateLightSources(chunkPos);
      }

      this.lightEngine.runLightUpdates();
   }

   public LevelChunk getChunk(int chunkX, int chunkZ) {
      return (LevelChunk)this.getChunk(chunkX, chunkZ, ChunkStatus.FULL);
   }

   public ChunkAccess getChunk(BlockPos pos) {
      return this.getChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
   }

   public boolean setBlock(BlockPos pos, BlockState newState, int flags, int recursionLeft) {
      if (this.isOutsideBuildHeight(pos)) {
         return false;
      } else {
         pos = pos.immutable();
         BlockState oldState = this.getBlockState(pos);
         if (oldState == newState) {
            return false;
         } else {
            this.blockStates.put(pos, newState);
            SectionPos sectionPos = SectionPos.of(pos);
            short nonEmptyBlockCount = this.nonEmptyBlockCounts.getShort(sectionPos);
            boolean prevEmpty = nonEmptyBlockCount == 0;
            if (!oldState.isAir()) {
               nonEmptyBlockCount--;
            }

            if (!newState.isAir()) {
               nonEmptyBlockCount++;
            }

            this.nonEmptyBlockCounts.put(sectionPos, nonEmptyBlockCount);
            boolean nowEmpty = nonEmptyBlockCount == 0;
            if (prevEmpty != nowEmpty) {
               this.lightEngine.updateSectionStatus(sectionPos, nowEmpty);
            }

            this.lightEngine.checkBlock(pos);
            return true;
         }
      }
   }

   public LevelLightEngine getLightEngine() {
      return this.lightEngine;
   }

   public BlockState getBlockState(BlockPos pos) {
      if (this.isOutsideBuildHeight(pos)) {
         return Blocks.VOID_AIR.defaultBlockState();
      } else {
         BlockState state = this.blockStates.get(pos);
         return state != null ? state : Blocks.AIR.defaultBlockState();
      }
   }

   public BlockState getBlockState(int x, int y, int z) {
      return this.getBlockState(this.scratchPos.set(x, y, z));
   }

   public FluidState getFluidState(BlockPos pos) {
      return this.isOutsideBuildHeight(pos) ? Fluids.EMPTY.defaultFluidState() : this.getBlockState(pos).getFluidState();
   }

   @Nullable
   public BlockEntity getBlockEntity(BlockPos pos) {
      return !this.isOutsideBuildHeight(pos) ? this.blockEntities.get(pos) : null;
   }

   public void setBlockEntity(BlockEntity blockEntity) {
      BlockPos pos = blockEntity.getBlockPos();
      if (!this.isOutsideBuildHeight(pos)) {
         this.blockEntities.put(pos, blockEntity);
      }
   }

   public void removeBlockEntity(BlockPos pos) {
      if (!this.isOutsideBuildHeight(pos)) {
         BlockEntity blockEntity = this.blockEntities.remove(pos);
         if (blockEntity != null) {
            blockEntity.setRemoved();
         }
      }
   }

   public ModelData getModelData(BlockPos pos) {
      BlockEntity blockEntity = this.getBlockEntity(pos);
      return blockEntity != null ? blockEntity.getModelData() : ModelData.EMPTY;
   }

   protected LevelEntityGetter<Entity> getEntities() {
      return this.entityGetter;
   }

   public ChunkSource getChunkSource() {
      return this.chunkSource;
   }

   public int getMinBuildHeight() {
      return this.minBuildHeight;
   }

   public int getHeight() {
      return this.height;
   }

   public Holder<Biome> getBiome(BlockPos pos) {
      return super.getBiome(pos.offset(this.biomeOffset));
   }

   public Holder<Biome> getNoiseBiome(int x, int y, int z) {
      return this.level.getNoiseBiome(x + this.biomeOffset.getX(), y + this.biomeOffset.getY(), z + this.biomeOffset.getZ());
   }

   public Holder<Biome> getUncachedNoiseBiome(int x, int y, int z) {
      return this.level.getUncachedNoiseBiome(x + this.biomeOffset.getX(), y + this.biomeOffset.getY(), z + this.biomeOffset.getZ());
   }

   public int getMaxLocalRawBrightness(BlockPos pos) {
      return 15;
   }

   public float getShade(Direction direction, boolean shade) {
      return 1.0F;
   }

   public Scoreboard getScoreboard() {
      return this.level.getScoreboard();
   }

   public RecipeManager getRecipeManager() {
      return this.level.getRecipeManager();
   }

   public BiomeManager getBiomeManager() {
      return this.level.getBiomeManager();
   }

   public LevelTickAccess<Block> getBlockTicks() {
      return this.level.getBlockTicks();
   }

   public LevelTickAccess<Fluid> getFluidTicks() {
      return this.level.getFluidTicks();
   }

   public FeatureFlagSet enabledFeatures() {
      return this.level.enabledFeatures();
   }

   public PotionBrewing potionBrewing() {
      return this.level.potionBrewing();
   }

   public void setDayTimeFraction(float v) {
      this.level.setDayTimeFraction(v);
   }

   public void setDayTimePerTick(float v) {
      this.level.setDayTimePerTick(v);
   }

   public float getDayTimeFraction() {
      return this.level.getDayTimeFraction();
   }

   public float getDayTimePerTick() {
      return this.level.getDayTimePerTick();
   }

   public void updateNeighbourForOutputSignal(BlockPos pos, Block block) {
   }

   public boolean isLoaded(BlockPos pos) {
      return true;
   }

   public boolean isAreaLoaded(BlockPos center, int range) {
      return true;
   }

   public void playSeededSound(
      Player player, double x, double y, double z, Holder<SoundEvent> soundEvent, SoundSource soundSource, float volume, float pitch, long seed
   ) {
   }

   public void playSeededSound(Player player, Entity entity, Holder<SoundEvent> soundEvent, SoundSource soundSource, float volume, float pitch, long seed) {
   }

   public String gatherChunkSourceStats() {
      return "";
   }

   @Nullable
   public Entity getEntity(int id) {
      return null;
   }

   public TickRateManager tickRateManager() {
      return this.level.tickRateManager();
   }

   @Nullable
   public MapItemSavedData getMapData(MapId mapId) {
      return null;
   }

   public void setMapData(MapId mapId, MapItemSavedData mapItemSavedData) {
   }

   @NotNull
   public MapId getFreeMapId() {
      return new MapId(0);
   }

   public void destroyBlockProgress(int breakerId, BlockPos pos, int progress) {
   }

   public void levelEvent(@Nullable Player player, int type, BlockPos pos, int data) {
   }

   public void gameEvent(Holder<GameEvent> gameEvent, Vec3 pos, Context context) {
   }

   public List<? extends Player> players() {
      return Collections.emptyList();
   }

   public LevelChunk getChunkAtImmediately(int chunkX, int chunkZ) {
      return this.chunkSource.getChunk(chunkX, chunkZ, false);
   }

   public ChunkAccess getAnyChunkImmediately(int chunkX, int chunkZ) {
      return this.chunkSource.getChunk(chunkX, chunkZ);
   }

   public int getMaxBuildHeight() {
      return this.getMinBuildHeight() + this.getHeight();
   }

   public int getSectionsCount() {
      return this.getMaxSection() - this.getMinSection();
   }

   public int getMinSection() {
      return SectionPos.blockToSectionCoord(this.getMinBuildHeight());
   }

   public int getMaxSection() {
      return SectionPos.blockToSectionCoord(this.getMaxBuildHeight() - 1) + 1;
   }

   public boolean isOutsideBuildHeight(BlockPos pos) {
      return this.isOutsideBuildHeight(pos.getY());
   }

   public boolean isOutsideBuildHeight(int y) {
      return y < this.getMinBuildHeight() || y >= this.getMaxBuildHeight();
   }

   public int getSectionIndex(int y) {
      return this.getSectionIndexFromSectionY(SectionPos.blockToSectionCoord(y));
   }

   public int getSectionIndexFromSectionY(int sectionY) {
      return sectionY - this.getMinSection();
   }

   public int getSectionYFromSectionIndex(int sectionIndex) {
      return sectionIndex + this.getMinSection();
   }
}
