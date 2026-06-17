package dev.ryanhcode.sable.sublevel.plot;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.CommonLevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEvent.Context;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.LevelTickAccess;
import org.jetbrains.annotations.Nullable;

public class EmbeddedPlotLevelAccessor implements CommonLevelAccessor, ServerLevelAccessor {
   private final LevelPlot plot;
   private final BlockPos center;
   private final ChunkPos centerChunk;
   private final Level level;

   public EmbeddedPlotLevelAccessor(LevelPlot plot) {
      this.plot = plot;
      this.level = plot.getSubLevel().getLevel();
      this.center = plot.getCenterBlock();
      this.centerChunk = plot.getCenterChunk();
   }

   public float getShade(Direction direction, boolean bl) {
      return this.level.getShade(direction, bl);
   }

   public LevelLightEngine getLightEngine() {
      return this.level.getLightEngine();
   }

   public WorldBorder getWorldBorder() {
      return this.level.getWorldBorder();
   }

   @Nullable
   public BlockEntity getBlockEntity(BlockPos blockPos) {
      return this.level.getBlockEntity(blockPos.offset(this.center));
   }

   public BlockState getBlockState(BlockPos blockPos) {
      return this.level.getBlockState(blockPos.offset(this.center));
   }

   public FluidState getFluidState(BlockPos blockPos) {
      return this.level.getFluidState(blockPos.offset(this.center));
   }

   public List<Entity> getEntities(@Nullable Entity entity, AABB aABB, Predicate<? super Entity> predicate) {
      return this.level.getEntities(entity, aABB.move((double)this.center.getX(), (double)this.center.getY(), (double)this.center.getZ()), predicate);
   }

   public <T extends Entity> List<T> getEntities(EntityTypeTest<Entity, T> entityTypeTest, AABB aABB, Predicate<? super T> predicate) {
      return this.level.getEntities(entityTypeTest, aABB.move((double)this.center.getX(), (double)this.center.getY(), (double)this.center.getZ()), predicate);
   }

   public List<? extends Player> players() {
      return this.level.players();
   }

   @Nullable
   public ChunkAccess getChunk(int i, int j, ChunkStatus chunkStatus, boolean bl) {
      return this.level.getChunk(i + this.centerChunk.x, j + this.centerChunk.z, chunkStatus, bl);
   }

   public long nextSubTickCount() {
      return this.level.nextSubTickCount();
   }

   public LevelTickAccess<Block> getBlockTicks() {
      return this.level.getBlockTicks();
   }

   public LevelTickAccess<Fluid> getFluidTicks() {
      return this.level.getFluidTicks();
   }

   public LevelData getLevelData() {
      return this.level.getLevelData();
   }

   public DifficultyInstance getCurrentDifficultyAt(BlockPos blockPos) {
      return this.level.getCurrentDifficultyAt(blockPos.offset(this.center));
   }

   @Nullable
   public MinecraftServer getServer() {
      return this.level.getServer();
   }

   public ChunkSource getChunkSource() {
      return this.level.getChunkSource();
   }

   public boolean hasChunk(int i, int j) {
      return this.level.hasChunk(i + this.centerChunk.x, j + this.centerChunk.z);
   }

   public RandomSource getRandom() {
      return this.level.getRandom();
   }

   public void playSound(@Nullable Player player, BlockPos blockPos, SoundEvent soundEvent, SoundSource soundSource, float f, float g) {
      this.level.playSound(player, blockPos.offset(this.center), soundEvent, soundSource, f, g);
   }

   public void addParticle(ParticleOptions particleOptions, double d, double e, double f, double g, double h, double i) {
      this.level.addParticle(particleOptions, d + (double)this.center.getX(), e + (double)this.center.getY(), f + (double)this.center.getZ(), g, h, i);
   }

   public void levelEvent(@Nullable Player player, int i, BlockPos blockPos, int j) {
      this.level.levelEvent(player, i, blockPos.offset(this.center), j);
   }

   public void gameEvent(Holder<GameEvent> holder, Vec3 vec3, Context context) {
      this.level.gameEvent(holder, vec3, context);
   }

   public int getHeight(Types types, int i, int j) {
      return this.level.getHeight(types, i + this.center.getX(), j + this.center.getZ());
   }

   public int getSkyDarken() {
      return this.level.getSkyDarken();
   }

   public BiomeManager getBiomeManager() {
      return this.level.getBiomeManager();
   }

   public Holder<Biome> getUncachedNoiseBiome(int i, int j, int k) {
      return this.level.getUncachedNoiseBiome(i + this.center.getX(), j + this.center.getY(), k + this.center.getZ());
   }

   public boolean isClientSide() {
      return this.level.isClientSide();
   }

   @Deprecated
   public int getSeaLevel() {
      return this.level.getSeaLevel();
   }

   public DimensionType dimensionType() {
      return this.level.dimensionType();
   }

   public RegistryAccess registryAccess() {
      return this.level.registryAccess();
   }

   public FeatureFlagSet enabledFeatures() {
      return this.level.enabledFeatures();
   }

   public boolean isStateAtPosition(BlockPos blockPos, Predicate<BlockState> predicate) {
      return this.level.isStateAtPosition(blockPos.offset(this.center), predicate);
   }

   public boolean isFluidAtPosition(BlockPos blockPos, Predicate<FluidState> predicate) {
      return this.level.isFluidAtPosition(blockPos.offset(this.center), predicate);
   }

   public boolean setBlock(BlockPos blockPos, BlockState blockState, int i, int j) {
      return this.level.setBlock(blockPos.offset(this.center), blockState, i, j);
   }

   public boolean removeBlock(BlockPos blockPos, boolean bl) {
      return this.level.removeBlock(blockPos.offset(this.center), bl);
   }

   public boolean destroyBlock(BlockPos blockPos, boolean bl, @Nullable Entity entity, int i) {
      return this.level.destroyBlock(blockPos.offset(this.center), bl, entity, i);
   }

   public ServerLevel getLevel() {
      return (ServerLevel)this.level;
   }
}
