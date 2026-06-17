package dev.ryanhcode.sable.mixin.plot;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.mixin.loaded_chunk_debug.ClientChunkCacheStorageAccessor;
import dev.ryanhcode.sable.mixinterface.loaded_chunk_debug.DebugChunkProviderAttachments;
import dev.ryanhcode.sable.platform.SableChunkEventPlatform;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Consumer;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientChunkCache.Storage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData.BlockEntityTagOutput;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ClientChunkCache.class})
public abstract class ClientChunkCacheMixin implements DebugChunkProviderAttachments {
   @Shadow
   @Final
   private static Logger LOGGER;
   @Shadow
   @Final
   private ClientLevel level;
   @Shadow
   @Final
   private LevelChunk emptyChunk;
   @Shadow
   volatile Storage storage;

   @Shadow
   private static boolean isValidChunk(@Nullable LevelChunk levelChunk, int i, int j) {
      return false;
   }

   @Unique
   @NotNull
   private SubLevelContainer sable$getPlotContainer() {
      SubLevelContainer container = SubLevelContainer.getContainer(this.level);
      return container;
   }

   @Inject(
      method = {"getChunk(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/LevelChunk;"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void getChunk(int x, int z, ChunkStatus status, boolean create, CallbackInfoReturnable<LevelChunk> cir) {
      SubLevelContainer container = this.sable$getPlotContainer();
      if (container.inBounds(x, z)) {
         ChunkPos chunkPos = new ChunkPos(x, z);
         LevelChunk chunk = container.getChunk(chunkPos);
         if (chunk != null) {
            cir.setReturnValue(chunk);
         } else {
            cir.setReturnValue(this.emptyChunk);
         }
      }
   }

   @Inject(
      method = {"drop"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void drop(ChunkPos chunkPos, CallbackInfo ci) {
      SubLevelContainer container = this.sable$getPlotContainer();
      if (container.inBounds(chunkPos)) {
         ci.cancel();
         throw new UnsupportedOperationException("Cannot drop chunks in plot");
      }
   }

   @Inject(
      method = {"replaceBiomes"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void replaceBiomes(int x, int z, FriendlyByteBuf friendlyByteBuf, CallbackInfo ci) {
      SubLevelContainer container = this.sable$getPlotContainer();
      if (container.inBounds(x, z)) {
         ChunkPos chunkPos = new ChunkPos(x, z);
         LevelChunk levelChunk = container.getChunk(chunkPos);
         if (levelChunk != null && isValidChunk(levelChunk, x, z)) {
            levelChunk.replaceBiomes(friendlyByteBuf);
         } else {
            LOGGER.warn("Ignoring chunk since it's not present: {}, {}", x, z);
         }
      }
   }

   @Inject(
      method = {"replaceWithPacketData"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void replaceWithPacketData(
      int x, int z, FriendlyByteBuf friendlyByteBuf, CompoundTag compoundTag, Consumer<BlockEntityTagOutput> consumer, CallbackInfoReturnable<LevelChunk> cir
   ) {
      SubLevelContainer container = this.sable$getPlotContainer();
      if (container.inBounds(x, z)) {
         ChunkPos chunkPos = new ChunkPos(x, z);
         LevelChunk levelChunk = container.getChunk(chunkPos);
         if (!isValidChunk(levelChunk, x, z)) {
            if (levelChunk != null) {
               SableChunkEventPlatform.INSTANCE.onOldChunkInvalid(levelChunk);
               this.level.unload(levelChunk);
            }

            levelChunk = new LevelChunk(this.level, chunkPos);
            levelChunk.replaceWithPacketData(friendlyByteBuf, compoundTag, consumer);
            container.newPopulatedChunk(chunkPos, levelChunk);
         } else {
            levelChunk.replaceWithPacketData(friendlyByteBuf, compoundTag, consumer);
         }

         this.level.onChunkLoaded(chunkPos);
         this.level.getLightEngine().setLightEnabled(chunkPos, true);
         SableChunkEventPlatform.INSTANCE.onClientChunkPacketReplaced(levelChunk);
         cir.setReturnValue(levelChunk);
      }
   }

   @Override
   public Collection<LevelChunk> sable$loadedChunks() {
      List<LevelChunk> loadedChunks = new LinkedList<>();
      ClientChunkCacheStorageAccessor accessor = (ClientChunkCacheStorageAccessor)this.storage;
      if (accessor != null) {
         AtomicReferenceArray<LevelChunk> chunks = accessor.getChunks();

         for (int i = 0; i < chunks.length(); i++) {
            LevelChunk chunk = chunks.get(i);
            if (chunk != null) {
               loadedChunks.add(chunk);
            }
         }
      }

      return loadedChunks;
   }
}
