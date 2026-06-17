package com.simibubi.create.foundation.virtualWorld;

import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import it.unimi.dsi.fastutil.shorts.ShortList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ChunkAccess.TicksToSave;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.ticks.BlackholeTickAccess;
import net.minecraft.world.ticks.TickContainerAccess;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VirtualChunk extends LevelChunk {
   public final VirtualRenderWorld world;
   private final VirtualChunkSection[] sections;
   private boolean needsLight;

   public VirtualChunk(VirtualRenderWorld world, int x, int z) {
      super(world, new ChunkPos(x, z));
      this.world = world;
      int sectionCount = world.getSectionsCount();
      this.sections = new VirtualChunkSection[sectionCount];

      for (int i = 0; i < sectionCount; i++) {
         this.sections[i] = new VirtualChunkSection(this, i << 4);
      }

      this.needsLight = true;
   }

   @Nullable
   public BlockState setBlockState(BlockPos pos, BlockState state, boolean isMoving) {
      return null;
   }

   public void setBlockEntity(BlockEntity blockEntity) {
   }

   public void addEntity(Entity entity) {
   }

   public Set<BlockPos> getBlockEntitiesPos() {
      return Collections.emptySet();
   }

   public LevelChunkSection[] getSections() {
      return this.sections;
   }

   public Collection<Entry<Types, Heightmap>> getHeightmaps() {
      return Collections.emptySet();
   }

   public void setHeightmap(Types type, long[] data) {
   }

   public Heightmap getOrCreateHeightmapUnprimed(Types type) {
      return null;
   }

   public int getHeight(Types type, int x, int z) {
      return 0;
   }

   @Nullable
   public StructureStart getStartForStructure(Structure structure) {
      return null;
   }

   public void setStartForStructure(Structure structure, StructureStart structureStart) {
   }

   public Map<Structure, StructureStart> getAllStarts() {
      return Collections.emptyMap();
   }

   public void setAllStarts(Map<Structure, StructureStart> structureStarts) {
   }

   public LongSet getReferencesForStructure(Structure pStructure) {
      return LongSets.emptySet();
   }

   public void addReferenceForStructure(Structure structure, long reference) {
   }

   public Map<Structure, LongSet> getAllReferences() {
      return Collections.emptyMap();
   }

   public void setAllReferences(Map<Structure, LongSet> structureReferencesMap) {
   }

   public void setUnsaved(boolean unsaved) {
   }

   public boolean isUnsaved() {
      return false;
   }

   public ChunkStatus getPersistedStatus() {
      return ChunkStatus.LIGHT;
   }

   public void removeBlockEntity(BlockPos pos) {
   }

   public ShortList[] getPostProcessing() {
      return new ShortList[0];
   }

   @Nullable
   public CompoundTag getBlockEntityNbt(BlockPos pos) {
      return null;
   }

   @Nullable
   public CompoundTag getBlockEntityNbtForSaving(BlockPos pos, Provider registries) {
      return null;
   }

   public void findBlocks(
      @NotNull Predicate<BlockState> roughFilter, @NotNull BiPredicate<BlockState, BlockPos> fineFilter, @NotNull BiConsumer<BlockPos, BlockState> output
   ) {
      this.world
         .blockStates
         .forEach(
            (pos, state) -> {
               if (SectionPos.blockToSectionCoord(pos.getX()) == this.chunkPos.x
                  && SectionPos.blockToSectionCoord(pos.getZ()) == this.chunkPos.z
                  && roughFilter.test(state)
                  && fineFilter.test(state, pos)) {
                  output.accept(pos, state);
               }
            }
         );
   }

   public TickContainerAccess<Block> getBlockTicks() {
      return BlackholeTickAccess.emptyContainer();
   }

   public TickContainerAccess<Fluid> getFluidTicks() {
      return BlackholeTickAccess.emptyContainer();
   }

   public TicksToSave getTicksForSerialization() {
      throw new UnsupportedOperationException();
   }

   public long getInhabitedTime() {
      return 0L;
   }

   public void setInhabitedTime(long amount) {
   }

   public boolean isLightCorrect() {
      return this.needsLight;
   }

   public void setLightCorrect(boolean lightCorrect) {
      this.needsLight = lightCorrect;
   }

   @Nullable
   public BlockEntity getBlockEntity(BlockPos pos) {
      return this.world.getBlockEntity(pos);
   }

   public BlockState getBlockState(BlockPos pos) {
      return this.world.getBlockState(pos);
   }

   public FluidState getFluidState(BlockPos pos) {
      return this.world.getFluidState(pos);
   }
}
