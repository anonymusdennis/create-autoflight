package net.createmod.catnip.ghostblock;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.Map;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class GhostBlocks {
   private static final GhostBlocks instance = new GhostBlocks();
   final Map<Object, GhostBlocks.Entry> ghosts = new HashMap<>();

   public static GhostBlocks getInstance() {
      return instance;
   }

   public static double getBreathingAlpha() {
      double period = 2500.0;
      double timer = (double)System.currentTimeMillis() % period;
      double offset = (double)Mth.cos((float)(2.0 / period * Math.PI * timer));
      return 0.55 - 0.2 * offset;
   }

   public GhostBlockParams showGhostState(Object slot, BlockState state) {
      return this.showGhostState(slot, state, 1);
   }

   public GhostBlockParams showGhostState(Object slot, BlockState state, int ttl) {
      GhostBlocks.Entry e = this.refresh(slot, GhostBlockRenderer.transparent(), GhostBlockParams.of(state), ttl);
      return e.params;
   }

   public GhostBlockParams showGhost(Object slot, GhostBlockRenderer ghost, GhostBlockParams params, int ttl) {
      GhostBlocks.Entry e = this.refresh(slot, ghost, params, ttl);
      return e.params;
   }

   private GhostBlocks.Entry refresh(Object slot, GhostBlockRenderer ghost, GhostBlockParams params, int ttl) {
      if (!this.ghosts.containsKey(slot)) {
         this.ghosts.put(slot, new GhostBlocks.Entry(ghost, params, ttl));
      }

      GhostBlocks.Entry e = this.ghosts.get(slot);
      e.ticksToLive = ttl;
      e.params = params;
      e.ghost = ghost;
      return e;
   }

   private GhostBlocks() {
   }

   public void tickGhosts() {
      this.ghosts.forEach((slot, entry) -> entry.ticksToLive--);
      this.ghosts.entrySet().removeIf(e -> !e.getValue().isAlive());
   }

   public void renderAll(PoseStack ms, SuperRenderTypeBuffer buffer, Vec3 camera) {
      this.ghosts.forEach((slot, entry) -> {
         GhostBlockRenderer ghost = entry.ghost;
         ghost.render(ms, buffer, camera, entry.params);
      });
   }

   static class Entry {
      private GhostBlockRenderer ghost;
      private GhostBlockParams params;
      private int ticksToLive;

      public Entry(GhostBlockRenderer ghost, GhostBlockParams params) {
         this(ghost, params, 1);
      }

      public Entry(GhostBlockRenderer ghost, GhostBlockParams params, int ttl) {
         this.ghost = ghost;
         this.params = params;
         this.ticksToLive = ttl;
      }

      public boolean isAlive() {
         return this.ticksToLive >= 0;
      }
   }
}
