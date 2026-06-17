package net.createmod.catnip.levelWrappers;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.chunk.LevelChunk.EntityCreationType;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
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
import net.minecraft.world.ticks.BlackholeTickAccess;
import net.minecraft.world.ticks.LevelTickAccess;
import org.jetbrains.annotations.Nullable;

public class SchematicChunkSource extends ChunkSource {
   private final Level fallbackWorld;

   public SchematicChunkSource(Level world) {
      this.fallbackWorld = world;
   }

   @Nullable
   public LightChunk getChunkForLighting(int x, int z) {
      return this.getChunk(x, z);
   }

   public Level getLevel() {
      return this.fallbackWorld;
   }

   @Nullable
   public ChunkAccess getChunk(int x, int z, ChunkStatus status, boolean p_212849_4_) {
      return this.getChunk(x, z);
   }

   public ChunkAccess getChunk(int x, int z) {
      return new SchematicChunkSource.EmptierChunk(this.fallbackWorld);
   }

   public String gatherStats() {
      return "WrappedChunkProvider";
   }

   public LevelLightEngine getLightEngine() {
      return this.fallbackWorld.getLightEngine();
   }

   public void tick(BooleanSupplier p_202162_, boolean p_202163_) {
   }

   public int getLoadedChunksCount() {
      return 0;
   }

   public static class EmptierChunk extends LevelChunk {
      public EmptierChunk(Level level) {
         super(new SchematicChunkSource.EmptierChunk.DummyLevel(level), ChunkPos.ZERO);
      }

      public BlockState getBlockState(BlockPos p_180495_1_) {
         return Blocks.VOID_AIR.defaultBlockState();
      }

      @Nullable
      public BlockState setBlockState(BlockPos p_177436_1_, BlockState p_177436_2_, boolean p_177436_3_) {
         return null;
      }

      public FluidState getFluidState(BlockPos p_204610_1_) {
         return Fluids.EMPTY.defaultFluidState();
      }

      public int getLightEmission(BlockPos p_217298_1_) {
         return 0;
      }

      @Nullable
      public BlockEntity getBlockEntity(BlockPos p_177424_1_, EntityCreationType p_177424_2_) {
         return null;
      }

      public void addAndRegisterBlockEntity(BlockEntity p_150813_1_) {
      }

      public void setBlockEntity(BlockEntity p_177426_2_) {
      }

      public void removeBlockEntity(BlockPos p_177425_1_) {
      }

      public void markUnsaved() {
      }

      public boolean isEmpty() {
         return true;
      }

      public boolean isYSpaceEmpty(int p_76606_1_, int p_76606_2_) {
         return true;
      }

      public FullChunkStatus getFullStatus() {
         return FullChunkStatus.FULL;
      }

      private static final class DummyLevel extends Level {
         private final RegistryAccess access;

         private DummyLevel(
            WritableLevelData pLevelData,
            ResourceKey<Level> pDimension,
            RegistryAccess pRegistryAccess,
            Holder<DimensionType> pDimensionTypeRegistration,
            Supplier<ProfilerFiller> pProfiler,
            boolean pIsClientSide,
            boolean pIsDebug,
            long pBiomeZoomSeed,
            int pMaxChainedNeighborUpdates
         ) {
            super(
               pLevelData,
               pDimension,
               pRegistryAccess,
               pDimensionTypeRegistration,
               pProfiler,
               pIsClientSide,
               pIsDebug,
               pBiomeZoomSeed,
               pMaxChainedNeighborUpdates
            );
            this.access = pRegistryAccess;
         }

         private DummyLevel(Level level) {
            this(null, null, level.registryAccess(), level.dimensionTypeRegistration(), null, false, false, 0L, 0);
         }

         public ChunkSource getChunkSource() {
            return null;
         }

         public void levelEvent(Player pPlayer, int pType, BlockPos pPos, int pData) {
         }

         public void gameEvent(@Nullable Entity entity, Holder<GameEvent> gameEvent, Vec3 pos) {
         }

         public void gameEvent(Holder<GameEvent> holder, Vec3 vec3, Context context) {
         }

         public RegistryAccess registryAccess() {
            return this.access;
         }

         public PotionBrewing potionBrewing() {
            return null;
         }

         public List<? extends Player> players() {
            return null;
         }

         public Holder<Biome> getUncachedNoiseBiome(int pX, int pY, int pZ) {
            return null;
         }

         public float getShade(Direction pDirection, boolean pShade) {
            return 0.0F;
         }

         public void sendBlockUpdated(BlockPos pPos, BlockState pOldState, BlockState pNewState, int pFlags) {
         }

         public void playSound(Player pPlayer, double pX, double pY, double pZ, SoundEvent pSound, SoundSource pCategory, float pVolume, float pPitch) {
         }

         public void playSound(Player pPlayer, Entity pEntity, SoundEvent pEvent, SoundSource pCategory, float pVolume, float pPitch) {
         }

         public void playSeededSound(
            Player pPlayer, double pX, double pY, double pZ, Holder<SoundEvent> pSound, SoundSource pSource, float pVolume, float pPitch, long pSeed
         ) {
         }

         public void playSeededSound(
            Player p_220363_,
            double p_220364_,
            double p_220365_,
            double p_220366_,
            SoundEvent p_220367_,
            SoundSource p_220368_,
            float p_220369_,
            float p_220370_,
            long p_220371_
         ) {
         }

         public void playSeededSound(
            Player p_220372_, Entity p_220373_, Holder<SoundEvent> p_220374_, SoundSource p_220375_, float p_220376_, float p_220377_, long p_220378_
         ) {
         }

         public String gatherChunkSourceStats() {
            return null;
         }

         public Entity getEntity(int pId) {
            return null;
         }

         @Nullable
         public MapItemSavedData getMapData(MapId mapId) {
            return null;
         }

         public void setMapData(MapId mapId, MapItemSavedData mapItemSavedData) {
         }

         public MapId getFreeMapId() {
            return new MapId(0);
         }

         public void destroyBlockProgress(int pBreakerId, BlockPos pPos, int pProgress) {
         }

         public Scoreboard getScoreboard() {
            return null;
         }

         public RecipeManager getRecipeManager() {
            return null;
         }

         protected LevelEntityGetter<Entity> getEntities() {
            return null;
         }

         public LevelTickAccess<Block> getBlockTicks() {
            return BlackholeTickAccess.emptyLevelList();
         }

         public LevelTickAccess<Fluid> getFluidTicks() {
            return BlackholeTickAccess.emptyLevelList();
         }

         public FeatureFlagSet enabledFeatures() {
            return FeatureFlagSet.of();
         }

         public TickRateManager tickRateManager() {
            return null;
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
   }
}
