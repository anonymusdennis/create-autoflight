package dev.ryanhcode.sable.mixin.plot;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import java.util.List;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ChunkMap.class})
public class ChunkMapMixin {
   @Shadow
   @Final
   private ServerLevel level;

   @Inject(
      method = {"getPlayers"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void sable$getPlayers(ChunkPos chunkPos, boolean bl, CallbackInfoReturnable<List<ServerPlayer>> cir) {
      SubLevelContainer container = SubLevelContainer.getContainer(this.level);
      if (container.inBounds(chunkPos)) {
         List<ServerPlayer> players = container.getPlayersTracking(chunkPos);
         cir.setReturnValue(players);
      }
   }

   @Inject(
      method = {"saveChunkIfNeeded"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void sable$saveChunkIfNeeded(ChunkHolder chunkHolder, CallbackInfoReturnable<Boolean> cir) {
      if (chunkHolder instanceof PlotChunkHolder) {
         cir.setReturnValue(false);
      }
   }

   @Redirect(
      method = {"hasWork"},
      at = @At(
         value = "INVOKE",
         target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectLinkedOpenHashMap;isEmpty()Z",
         ordinal = 1,
         remap = false
      )
   )
   private boolean sable$hasWork(Long2ObjectLinkedOpenHashMap<ChunkHolder> updatingChunkMap) {
      return !updatingChunkMap.values().stream().anyMatch(chunkHolder -> !(chunkHolder instanceof PlotChunkHolder));
   }

   @Inject(
      method = {"isChunkTracked"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void sable$isChunkTracked(ServerPlayer serverPlayer, int i, int j, CallbackInfoReturnable<Boolean> cir) {
      SubLevelContainer container = SubLevelContainer.getContainer(this.level);
      LevelPlot plot = container.getPlot(new ChunkPos(i, j));
      if (plot != null) {
         ServerSubLevel subLevel = (ServerSubLevel)plot.getSubLevel();
         cir.setReturnValue(subLevel.getTrackingPlayers().contains(serverPlayer.getGameProfile().getId()));
      }
   }

   @Inject(
      method = {"anyPlayerCloseEnoughForSpawning"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void sable$anyPlayerCloseEnoughForSpawning(ChunkPos chunkPos, CallbackInfoReturnable<Boolean> cir) {
      SubLevelContainer container = SubLevelContainer.getContainer(this.level);

      assert container != null;

      if (container.inBounds(chunkPos)) {
         LevelPlot plot = container.getPlot(chunkPos);
         if (plot != null) {
            ServerSubLevel subLevel = (ServerSubLevel)plot.getSubLevel();
            cir.setReturnValue(!subLevel.getTrackingPlayers().isEmpty());
         }
      }
   }
}
