package dev.ryanhcode.sable.mixin.plot;

import com.mojang.datafixers.DataFixer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.entity.ChunkStatusUpdateListener;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ServerChunkCache.class})
public class ServerChunkCacheMixin {
   @Shadow
   @Final
   ServerLevel level;
   @Unique
   private EmptyLevelChunk sable$emptyChunk;

   @Inject(
      method = {"<init>"},
      at = {@At("RETURN")}
   )
   public void init(
      ServerLevel serverLevel,
      LevelStorageAccess levelStorageAccess,
      DataFixer dataFixer,
      StructureTemplateManager structureTemplateManager,
      Executor executor,
      ChunkGenerator chunkGenerator,
      int i,
      int j,
      boolean bl,
      ChunkProgressListener chunkProgressListener,
      ChunkStatusUpdateListener chunkStatusUpdateListener,
      Supplier supplier,
      CallbackInfo ci
   ) {
      this.sable$emptyChunk = new EmptyLevelChunk(
         serverLevel, new ChunkPos(0, 0), serverLevel.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.PLAINS)
      );
   }

   @Unique
   @NotNull
   private SubLevelContainer sable$getPlotContainer() {
      SubLevelContainer container = SubLevelContainer.getContainer(this.level);
      return container;
   }

   @Inject(
      method = {"getChunkNow"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void getChunkNow(int x, int z, CallbackInfoReturnable<LevelChunk> cir) {
      SubLevelContainer container = this.sable$getPlotContainer();
      if (container.inBounds(x, z)) {
         LevelChunk chunk = container.getChunk(new ChunkPos(x, z));
         cir.setReturnValue(chunk);
      }
   }

   @Inject(
      method = {"getChunkFutureMainThread"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void getChunkFutureMainThread(
      int x, int z, ChunkStatus chunkStatus, boolean bl, CallbackInfoReturnable<CompletableFuture<ChunkResult<ChunkAccess>>> cir
   ) {
      SubLevelContainer container = this.sable$getPlotContainer();
      if (container.inBounds(x, z)) {
         ChunkPos chunkPos = new ChunkPos(x, z);
         LevelChunk chunk = container.getChunk(chunkPos);
         if (chunk != null) {
            cir.setReturnValue(CompletableFuture.completedFuture(ChunkResult.of(chunk)));
         } else {
            cir.setReturnValue(CompletableFuture.completedFuture(ChunkResult.of(this.sable$emptyChunk)));
         }
      }
   }

   @Inject(
      method = {"hasChunk"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hasChunk(int x, int z, CallbackInfoReturnable<Boolean> cir) {
      SubLevelContainer container = this.sable$getPlotContainer();
      if (container.inBounds(x, z)) {
         ChunkAccess chunk = container.getChunk(new ChunkPos(x, z));
         cir.setReturnValue(chunk != null);
      }
   }

   @Inject(
      method = {"getChunkForLighting"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void getChunkForLighting(int x, int z, CallbackInfoReturnable<LightChunk> cir) {
      SubLevelContainer container = this.sable$getPlotContainer();
      if (container.inBounds(x, z)) {
         LevelChunk chunk = container.getChunk(new ChunkPos(x, z));
         cir.setReturnValue(chunk);
      }
   }

   @Inject(
      method = {"isPositionTicking"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void isPositionTicking(long pos, CallbackInfoReturnable<Boolean> cir) {
      SubLevelContainer container = this.sable$getPlotContainer();
      if (container.inBounds(ChunkPos.getX(pos), ChunkPos.getZ(pos))) {
         ChunkPos chunkPos = new ChunkPos(pos);
         LevelChunk chunk = container.getChunk(chunkPos);
         cir.setReturnValue(chunk != null);
      }
   }

   @Inject(
      method = {"getFullChunk"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void getFullChunk(long pos, Consumer<LevelChunk> consumer, CallbackInfo ci) {
      SubLevelContainer container = this.sable$getPlotContainer();
      if (container.inBounds(ChunkPos.getX(pos), ChunkPos.getZ(pos))) {
         ChunkPos chunkPos = new ChunkPos(pos);
         LevelChunk chunk = container.getChunk(chunkPos);
         if (chunk != null) {
            consumer.accept(chunk);
         }

         ci.cancel();
      }
   }

   @Inject(
      method = {"blockChanged"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void blockChanged(BlockPos blockPos, CallbackInfo ci) {
      SubLevelContainer container = this.sable$getPlotContainer();
      ChunkPos pos = new ChunkPos(blockPos);
      if (container.inBounds(pos)) {
         PlotChunkHolder holder = container.getChunkHolder(pos);
         if (holder == null) {
            throw new UnsupportedOperationException("Cannot change blocks in nonexistent plot holder");
         }

         holder.blockChanged(blockPos);
         ci.cancel();
      }
   }

   @Inject(
      method = {"getVisibleChunkIfPresent"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void getVisibleChunkIfPresent(long l, CallbackInfoReturnable<ChunkHolder> cir) {
      int x = ChunkPos.getX(l);
      int z = ChunkPos.getZ(l);
      SubLevelContainer container = this.sable$getPlotContainer();
      if (container.inBounds(x, z)) {
         ChunkPos chunkPos = new ChunkPos(x, z);
         PlotChunkHolder holder = container.getChunkHolder(chunkPos);
         cir.setReturnValue(holder);
      }
   }

   @Inject(
      method = {"addRegionTicket"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private <T> void addRegionTicket(TicketType<T> type, ChunkPos pos, int distance, T value, CallbackInfo ci) {
      SubLevelContainer container = this.sable$getPlotContainer();
      if (container.inBounds(pos)) {
         ci.cancel();
      }
   }
}
