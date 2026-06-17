package net.createmod.catnip.levelWrappers;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import net.createmod.ponder.mixin.accessor.EntityAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEvent.Context;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.ticks.LevelTickAccess;
import org.jetbrains.annotations.Nullable;

public class WrappedLevel extends Level {
   protected Level level;
   protected ChunkSource chunkSource;
   protected LevelEntityGetter<Entity> entityGetter = new DummyLevelEntityGetter();

   public WrappedLevel(Level level) {
      super(
         (WritableLevelData)level.getLevelData(),
         level.dimension(),
         level.registryAccess(),
         level.dimensionTypeRegistration(),
         level::getProfiler,
         level.isClientSide,
         level.isDebug(),
         0L,
         0
      );
      this.level = level;
   }

   public void setChunkSource(ChunkSource source) {
      this.chunkSource = source;
   }

   public Level getLevel() {
      return this.level;
   }

   public LevelLightEngine getLightEngine() {
      return this.level.getLightEngine();
   }

   public BlockState getBlockState(@Nullable BlockPos pos) {
      return this.level.getBlockState(pos);
   }

   public boolean isStateAtPosition(BlockPos p_217375_1_, Predicate<BlockState> p_217375_2_) {
      return this.level.isStateAtPosition(p_217375_1_, p_217375_2_);
   }

   @Nullable
   public BlockEntity getBlockEntity(BlockPos pos) {
      return this.level.getBlockEntity(pos);
   }

   public boolean setBlock(BlockPos pos, BlockState newState, int flags) {
      return this.level.setBlock(pos, newState, flags);
   }

   public int getMaxLocalRawBrightness(BlockPos pos) {
      return 15;
   }

   public void sendBlockUpdated(BlockPos pos, BlockState oldState, BlockState newState, int flags) {
      this.level.sendBlockUpdated(pos, oldState, newState, flags);
   }

   public LevelTickAccess<Block> getBlockTicks() {
      return this.level.getBlockTicks();
   }

   public LevelTickAccess<Fluid> getFluidTicks() {
      return this.level.getFluidTicks();
   }

   public ChunkSource getChunkSource() {
      return this.chunkSource != null ? this.chunkSource : this.level.getChunkSource();
   }

   public void levelEvent(@Nullable Player player, int type, BlockPos pos, int data) {
   }

   public List<? extends Player> players() {
      return Collections.emptyList();
   }

   public void playSeededSound(
      Player pPlayer, double pX, double pY, double pZ, Holder<SoundEvent> pSound, SoundSource pSource, float pVolume, float pPitch, long pSeed
   ) {
   }

   public void playSeededSound(Player pPlayer, Entity pEntity, Holder<SoundEvent> pSound, SoundSource pCategory, float pVolume, float pPitch, long pSeed) {
   }

   public void playSound(@Nullable Player player, double x, double y, double z, SoundEvent soundIn, SoundSource category, float volume, float pitch) {
   }

   public void playSound(
      @Nullable Player p_217384_1_, Entity p_217384_2_, SoundEvent p_217384_3_, SoundSource p_217384_4_, float p_217384_5_, float p_217384_6_
   ) {
   }

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

   public boolean addFreshEntity(Entity entityIn) {
      ((EntityAccessor)entityIn).catnip$callSetLevel(this.level);
      return this.level.addFreshEntity(entityIn);
   }

   public void setMapData(MapId mapId, MapItemSavedData mapItemSavedData) {
   }

   public MapId getFreeMapId() {
      return this.level.getFreeMapId();
   }

   public void destroyBlockProgress(int breakerId, BlockPos pos, int progress) {
   }

   public Scoreboard getScoreboard() {
      return this.level.getScoreboard();
   }

   public RecipeManager getRecipeManager() {
      return this.level.getRecipeManager();
   }

   public Holder<Biome> getUncachedNoiseBiome(int p_225604_1_, int p_225604_2_, int p_225604_3_) {
      return this.level.getUncachedNoiseBiome(p_225604_1_, p_225604_2_, p_225604_3_);
   }

   public RegistryAccess registryAccess() {
      return this.level.registryAccess();
   }

   public PotionBrewing potionBrewing() {
      return this.level.potionBrewing();
   }

   public float getShade(Direction p_230487_1_, boolean p_230487_2_) {
      return this.level.getShade(p_230487_1_, p_230487_2_);
   }

   public void updateNeighbourForOutputSignal(BlockPos p_175666_1_, Block p_175666_2_) {
   }

   public void gameEvent(@Nullable Entity entity, Holder<GameEvent> gameEvent, Vec3 pos) {
   }

   public void gameEvent(Holder<GameEvent> holder, Vec3 vec3, Context context) {
   }

   public String gatherChunkSourceStats() {
      return this.level.gatherChunkSourceStats();
   }

   protected LevelEntityGetter<Entity> getEntities() {
      return this.entityGetter;
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

   public FeatureFlagSet enabledFeatures() {
      return this.level.enabledFeatures();
   }

   public void setDayTimeFraction(float var1) {
   }

   public float getDayTimeFraction() {
      return 0.0F;
   }

   public float getDayTimePerTick() {
      return 0.0F;
   }

   public void setDayTimePerTick(float var1) {
   }
}
