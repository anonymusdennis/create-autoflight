package dev.ryanhcode.sable.sublevel.plot;

import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelReactionWheel;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.platform.SableChunkEventPlatform;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.PalettedContainer.Strategy;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.LevelChunkTicks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.joml.Vector3dc;

public abstract class LevelPlot {
   public final ChunkPos plotPos;
   protected final SubLevelContainer container;
   protected final int logSize;
   private final PlotChunkHolder[] chunks;
   @NotNull
   private final SubLevel subLevel;
   private final List<PlotChunkHolder> loadedChunks = new ObjectArrayList();
   protected final Object2ObjectOpenHashMap<BlockPos, BlockEntitySubLevelActor> blockEntityActors = new Object2ObjectOpenHashMap();
   private final Object2ObjectOpenHashMap<BlockPos, BlockEntitySubLevelReactionWheel> blockEntityReactionWheels = new Object2ObjectOpenHashMap();
   protected boolean expandPlotIfNecessary = true;
   @Nullable
   protected BoundingBox3i localBounds = null;
   protected ResourceKey<Biome> biome = Biomes.PLAINS;

   public LevelPlot(SubLevelContainer container, int x, int z, int logSize, @NotNull SubLevel subLevel) {
      this.container = container;
      this.plotPos = new ChunkPos(x, z);
      this.logSize = logSize;
      this.chunks = new PlotChunkHolder[(1 << logSize) * (1 << logSize)];
      this.subLevel = subLevel;
   }

   public void tick() {
   }

   public EmbeddedPlotLevelAccessor getEmbeddedLevelAccessor() {
      return new EmbeddedPlotLevelAccessor(this);
   }

   public BlockPos getCenterBlock() {
      ChunkPos centerChunk = this.getCenterChunk();
      Level level = this.subLevel.getLevel();
      return new BlockPos(centerChunk.getMinBlockX() + 8, (level.getMinBuildHeight() + level.getMaxBuildHeight()) / 2, centerChunk.getMinBlockZ() + 8);
   }

   protected void newChunk(ChunkPos pos, LevelChunk chunk, boolean initializeLighting) {
      ChunkPos local = this.toLocal(pos);
      if (this.getChunkHolder(local) != null) {
         throw new IllegalStateException("Chunk already exists at %s".formatted(pos));
      } else {
         PlotChunkHolder holder = PlotChunkHolder.create(chunk.getLevel(), pos, this.getLightEngine(), chunk);
         this.addChunkHolder(local, holder, initializeLighting);
      }
   }

   public abstract LevelLightEngine getLightEngine();

   public void newEmptyChunk(ChunkPos pos) {
      Level level = this.container.getLevel();
      int sectionCount = level.getSectionsCount();
      LevelChunkSection[] sections = new LevelChunkSection[sectionCount];

      for (int i = 0; i < sectionCount; i++) {
         Registry<Biome> biomeRegistry = level.registryAccess().registryOrThrow(Registries.BIOME);
         PalettedContainer<BlockState> states = new PalettedContainer(Block.BLOCK_STATE_REGISTRY, Blocks.AIR.defaultBlockState(), Strategy.SECTION_STATES);
         PalettedContainer<Holder<Biome>> biomes = new PalettedContainer(
            biomeRegistry.asHolderIdMap(), biomeRegistry.getHolderOrThrow(this.biome), Strategy.SECTION_BIOMES
         );
         sections[i] = new LevelChunkSection(states, biomes);
      }

      LevelChunk chunk = new LevelChunk(level, pos, UpgradeData.EMPTY, new LevelChunkTicks(), new LevelChunkTicks(), 0L, sections, null, null);
      this.newChunk(pos, chunk, true);
      SableChunkEventPlatform.INSTANCE.onPlotChunkLoaded(chunk);
   }

   public SubLevel getSubLevel() {
      return this.subLevel;
   }

   public boolean contains(double x, double z) {
      int logBlockSize = this.logSize + 4;
      return x >= (double)(this.plotPos.x << logBlockSize)
         && x < (double)(this.plotPos.x + 1 << logBlockSize)
         && z >= (double)(this.plotPos.z << logBlockSize)
         && z < (double)(this.plotPos.z + 1 << logBlockSize);
   }

   public boolean contains(Vec3 point) {
      return this.contains(point.x(), point.z());
   }

   public boolean contains(Vector3dc point) {
      return this.contains(point.x(), point.z());
   }

   public ChunkPos getChunkMin() {
      return new ChunkPos(this.plotPos.x << this.logSize, this.plotPos.z << this.logSize);
   }

   public ChunkPos getChunkMax() {
      return new ChunkPos((this.plotPos.x + 1 << this.logSize) - 1, (this.plotPos.z + 1 << this.logSize) - 1);
   }

   public boolean contains(ChunkPos chunk) {
      return chunk.x >> this.logSize == this.plotPos.x && chunk.z >> this.logSize == this.plotPos.z;
   }

   public ChunkPos toLocal(ChunkPos global) {
      return new ChunkPos(global.x - (this.plotPos.x << this.logSize), global.z - (this.plotPos.z << this.logSize));
   }

   public ChunkPos toGlobal(ChunkPos local) {
      return new ChunkPos(local.x + (this.plotPos.x << this.logSize), local.z + (this.plotPos.z << this.logSize));
   }

   @Nullable
   public PlotChunkHolder getChunkHolder(ChunkPos local) {
      return local.x >= 0 && local.x < 1 << this.logSize && local.z >= 0 && local.z < 1 << this.logSize ? this.chunks[local.z << this.logSize | local.x] : null;
   }

   @Internal
   public void addChunkHolder(ChunkPos localChunkPos, PlotChunkHolder holder, boolean initializeLighting) {
      if (holder == null) {
         throw new IllegalArgumentException("Chunk cannot be null");
      } else {
         this.loadedChunks.add(holder);
         this.chunks[localChunkPos.z << this.logSize | localChunkPos.x] = holder;
         this.updateBoundingBox();
      }
   }

   public LevelChunk getChunk(ChunkPos local) {
      PlotChunkHolder holder = this.getChunkHolder(local);
      return holder == null ? null : holder.getChunk();
   }

   public ChunkPos getCenterChunk() {
      return new ChunkPos((this.plotPos.x << this.logSize) + (1 << this.logSize - 1), (this.plotPos.z << this.logSize) + (1 << this.logSize - 1));
   }

   public Collection<PlotChunkHolder> getLoadedChunks() {
      return this.loadedChunks;
   }

   public void updateBoundingBox() {
      if (!this.subLevel.getLevel().isClientSide) {
         BoundingBox3i previousBounds = this.localBounds;
         this.localBounds = null;
         BoundingBox3i temp = new BoundingBox3i(0, 0, 0, 0, 0, 0);

         for (PlotChunkHolder chunk : this.loadedChunks) {
            ChunkPos pos = chunk.getPos();
            BoundingBox3ic chunkLocalBounds = chunk.getBoundingBox();
            if (chunkLocalBounds != null) {
               BoundingBox3i chunkBounds = chunkLocalBounds.move(pos.getMinBlockX(), 0, pos.getMinBlockZ(), temp);
               if (chunkBounds != null) {
                  if (this.localBounds == null) {
                     this.localBounds = new BoundingBox3i(chunkBounds);
                  } else {
                     this.localBounds = this.localBounds.expandTo(chunkBounds, this.localBounds);
                  }
               }
            }
         }

         if (!Objects.equals(previousBounds, this.localBounds)) {
            this.subLevel.onPlotBoundsChanged();
         }
      }
   }

   public BoundingBox3ic getBoundingBox() {
      return (BoundingBox3ic)(this.localBounds != null ? this.localBounds : BoundingBox3i.EMPTY);
   }

   public void setBoundingBox(BoundingBox3ic bounds) {
      if (this.localBounds == null) {
         this.localBounds = new BoundingBox3i(bounds);
      } else {
         this.localBounds.set(bounds);
      }
   }

   public void onRemove() {
      for (PlotChunkHolder chunk : this.loadedChunks) {
         LevelChunk levelChunk = chunk.getChunk();

         assert levelChunk != null;

         levelChunk.setLoaded(false);
         this.onRemoveChunkHolder(levelChunk);
      }

      this.loadedChunks.clear();
      this.localBounds = null;
   }

   protected abstract void onRemoveChunkHolder(LevelChunk var1);

   public void expandIfNecessary(BlockPos blockPos) {
      if (this.expandPlotIfNecessary) {
         for (Direction direction : Direction.values()) {
            BlockPos offsetPos = blockPos.relative(direction, 2);
            ChunkPos globalChunk = new ChunkPos(offsetPos);
            if (this.getChunk(this.toLocal(globalChunk)) == null) {
               this.newEmptyChunk(globalChunk);
            }
         }
      }
   }

   public void onBlockChange(BlockPos pos, BlockState state) {
      Level level = this.subLevel.getLevel();
      BlockEntity blockEntity = level.getBlockEntity(pos);
      BlockEntitySubLevelActor actor = blockEntity instanceof BlockEntitySubLevelActor ? (BlockEntitySubLevelActor)blockEntity : null;
      if (actor != null) {
         this.blockEntityActors.put(pos, actor);
      } else {
         this.blockEntityActors.remove(pos);
      }

      if (blockEntity instanceof BlockEntitySubLevelReactionWheel reactionWheel) {
         this.blockEntityReactionWheels.put(pos, reactionWheel);
         if (this.subLevel instanceof ServerSubLevel serverSubLevel) {
            serverSubLevel.getReactionWheelManager().wheelChanged(pos, reactionWheel, true);
         }
      } else {
         BlockEntitySubLevelReactionWheel reactionWheelx = (BlockEntitySubLevelReactionWheel)this.blockEntityReactionWheels.remove(pos);
         if (reactionWheelx != null && this.subLevel instanceof ServerSubLevel serverSubLevel) {
            serverSubLevel.getReactionWheelManager().wheelChanged(pos, reactionWheelx, false);
         }
      }
   }

   public Iterable<BlockEntitySubLevelActor> getBlockEntityActors() {
      return this.blockEntityActors.values();
   }

   public Collection<BlockEntitySubLevelReactionWheel> getBlockEntityReactionWheels() {
      return this.blockEntityReactionWheels.values();
   }

   public Set<Entry<BlockPos, BlockEntitySubLevelReactionWheel>> getBlockEntityReactionWheelMap() {
      return this.blockEntityReactionWheels.entrySet();
   }
}
