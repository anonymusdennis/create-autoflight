package net.createmod.catnip.levelWrappers;

import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.createmod.ponder.mixin.accessor.BiomeManagerAccessor;
import net.createmod.ponder.mixin.accessor.EntityAccessor;
import net.createmod.ponder.mixin.accessor.MinecraftServerAccessor;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.ticks.LevelTicks;
import net.minecraft.world.ticks.TickPriority;

public class WrappedServerLevel extends ServerLevel {
   protected ServerLevel level;

   public WrappedServerLevel(ServerLevel level) {
      super(
         level.getServer(),
         Util.backgroundExecutor(),
         ((MinecraftServerAccessor)level.getServer()).catnip$getStorageSource(),
         (ServerLevelData)level.getLevelData(),
         level.dimension(),
         new LevelStem(level.dimensionTypeRegistration(), level.getChunkSource().getGenerator()),
         new DummyStatusListener(),
         level.isDebug(),
         ((BiomeManagerAccessor)level.getBiomeManager()).catnip$getBiomeZoomSeed(),
         Collections.emptyList(),
         false,
         level.getRandomSequences()
      );
      this.level = level;
   }

   public float getSunAngle(float p_72826_1_) {
      return 0.0F;
   }

   public int getMaxLocalRawBrightness(BlockPos pos) {
      return 15;
   }

   public void sendBlockUpdated(BlockPos pos, BlockState oldState, BlockState newState, int flags) {
      this.level.sendBlockUpdated(pos, oldState, newState, flags);
   }

   public LevelTicks<Block> getBlockTicks() {
      return super.getBlockTicks();
   }

   public LevelTicks<Fluid> getFluidTicks() {
      return super.getFluidTicks();
   }

   public void scheduleTick(BlockPos pos, Block block, int delay) {
   }

   public void scheduleTick(BlockPos pos, Fluid fluid, int delay) {
   }

   public void scheduleTick(BlockPos pos, Block block, int delay, TickPriority priority) {
   }

   public void scheduleTick(BlockPos pos, Fluid fluid, int delay, TickPriority priority) {
   }

   public void levelEvent(@Nullable Player player, int type, BlockPos pos, int data) {
   }

   public List<ServerPlayer> players() {
      return Collections.emptyList();
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

   @Nullable
   public MapItemSavedData getMapData(MapId mapId) {
      return null;
   }

   public boolean addFreshEntity(Entity entityIn) {
      ((EntityAccessor)entityIn).catnip$callSetLevel(this.level);
      return this.level.addFreshEntity(entityIn);
   }

   public void setMapData(MapId mapId, MapItemSavedData mapData) {
   }

   public MapId getFreeMapId() {
      return new MapId(0);
   }

   public void destroyBlockProgress(int breakerId, BlockPos pos, int progress) {
   }

   public RecipeManager getRecipeManager() {
      return this.level.getRecipeManager();
   }

   public Holder<Biome> getUncachedNoiseBiome(int p_225604_1_, int p_225604_2_, int p_225604_3_) {
      return this.level.getUncachedNoiseBiome(p_225604_1_, p_225604_2_, p_225604_3_);
   }
}
