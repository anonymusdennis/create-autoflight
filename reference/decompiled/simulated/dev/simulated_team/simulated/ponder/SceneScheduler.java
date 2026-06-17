package dev.simulated_team.simulated.ponder;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.util.SimDistUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Consumer;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.PonderSceneBuilder;
import net.createmod.ponder.foundation.instruction.PonderInstruction;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class SceneScheduler {
   private final SceneBuilder builder;
   private final List<SceneScheduler.Sequence> sequences = new ObjectArrayList();
   private final Map<Object, Set<SceneScheduler.Sequence>> syncs = new Object2ObjectOpenHashMap();
   private int time = 0;
   private boolean ran = false;

   public SceneScheduler(SceneBuilder builder) {
      this.builder = builder;
   }

   public SceneScheduler.Sequence get(int i) {
      if (this.sequences.size() <= i) {
         this.sequences.add(i, new SceneScheduler.Sequence(i, this, this.builder));
      }

      return this.sequences.get(i);
   }

   public void run() {
      this.run(false);
   }

   public void run(boolean debug) {
      if (this.ran) {
         SimDistUtil.getClientPlayer()
            .displayClientMessage(Component.literal("Set of scheduled sequences being re-run! See logs for more info").withStyle(ChatFormatting.RED), false);
         debug = true;
         Simulated.LOGGER
            .error("Trying to re-run scheduled sequences! Undefined behaviour ahead. A new instance should be made for running a new set of sequences");
      }

      this.ran = true;
      StringJoiner joiner = new StringJoiner("\n");
      boolean done = false;

      while (!done) {
         done = true;
         boolean hasSyncing = false;
         int shortestIdle = Integer.MAX_VALUE;
         int shortestI = -1;
         boolean isDeadlocked = true;

         for (int i = 0; i < this.sequences.size(); i++) {
            SceneScheduler.Sequence seq = this.sequences.get(i);

            while (!seq.isDone()) {
               int timeIdling = seq.time - this.time;
               if (timeIdling > 0) {
                  if (timeIdling < shortestIdle) {
                     shortestIdle = timeIdling;
                     shortestI = i;
                  }

                  done = false;
                  isDeadlocked = false;
                  break;
               }

               if (seq.isSyncIdle()) {
                  hasSyncing = true;
                  Object syncKey = seq.getSyncKey();
                  Set<SceneScheduler.Sequence> sync = seq.getSyncSet();
                  if (sync.remove(seq)) {
                     done = false;
                     joiner.add("(" + this.time + ") " + i + " awaiting sync " + syncKey);
                  }

                  if (sync.isEmpty()) {
                     isDeadlocked = false;
                  }
                  break;
               }

               isDeadlocked = false;
               int idle = seq.getIdle();
               if (idle > 0) {
                  seq.time += idle;
               } else {
                  joiner.add("(" + this.time + ") " + i + " action " + seq.queue.element().pInstruction);
               }

               done = false;
               seq.popAndTryRun();
            }
         }

         if (hasSyncing && isDeadlocked) {
            SimDistUtil.getClientPlayer()
               .displayClientMessage(Component.literal("Ponder sequence deadlock! See logs for more info").withStyle(ChatFormatting.RED), false);
            debug = true;
            Simulated.LOGGER.error("Every sequence is awaiting syncs that will never happen");

            for (int i = 0; i < this.sequences.size(); i++) {
               SceneScheduler.Sequence seq = this.sequences.get(i);
               if (!seq.isDone()) {
                  Object syncx = seq.queue.element().sync;
                  Simulated.LOGGER.error("Sequence {} syncing {} (blocked by {})", new Object[]{i, syncx, this.syncs.get(syncx)});
               }
            }

            this.sequences.forEach(seqx -> {
               if (!seqx.isDone()) {
                  joiner.add("!! (" + this.time + ") " + seqx.id + " skipping sync " + seqx.getSyncKey());
                  seqx.popAndTryRun();
                  seqx.time = this.time;
               }
            });
            done = false;
         }

         this.sequences.forEach(seqx -> {
            if (!seqx.isDone() && seqx.isSyncIdle() && seqx.getSyncSet().isEmpty()) {
               seqx.time = this.time;
               seqx.queue.remove();
            }
         });
         this.syncs.entrySet().removeIf(e -> {
            if (e.getValue().isEmpty()) {
               joiner.add("(" + this.time + ") Fully synced " + e.getKey());
               return true;
            } else {
               return false;
            }
         });
         if (shortestI != -1) {
            this.time += shortestIdle;
            this.builder.idle(shortestIdle);
            joiner.add("(" + this.time + ") " + shortestI + " idle " + shortestIdle);
         }
      }

      if (debug) {
         Simulated.LOGGER.info("Finalized sequence:\n" + joiner);
      }
   }

   private static record Instruction(Integer idle, PonderInstruction pInstruction, Object sync) {
   }

   public static class Sequence extends PonderSceneBuilder {
      private final Queue<SceneScheduler.Instruction> queue = new ArrayDeque<>();
      private final int id;
      private final SceneScheduler scheduler;
      private final SceneBuilder builder;
      private int time = 0;
      private boolean independent = true;
      private int duration = 0;

      private Sequence(int id, SceneScheduler scheduler, SceneBuilder builder) {
         super(builder.getScene());
         this.id = id;
         this.scheduler = scheduler;
         this.builder = builder;
      }

      public void addInstruction(PonderInstruction instruction) {
         this.queue.add(new SceneScheduler.Instruction(null, instruction, null));
      }

      public void addInstruction(Consumer<PonderScene> callback) {
         this.addInstruction(PonderInstruction.simple(callback));
      }

      public void idle(int ticks) {
         this.queue.add(new SceneScheduler.Instruction(ticks, null, null));
         this.duration += ticks;
      }

      public void sync(Object o) {
         this.scheduler.syncs.computeIfAbsent(o, k -> new HashSet<>()).add(this);
         this.queue.add(new SceneScheduler.Instruction(null, null, o));
         this.independent = false;
      }

      private boolean isDone() {
         return this.queue.isEmpty();
      }

      private int getIdle() {
         return Objects.requireNonNullElse(this.queue.element().idle(), 0);
      }

      private boolean isSyncIdle() {
         return this.queue.element().sync != null;
      }

      private Object getSyncKey() {
         return this.queue.element().sync;
      }

      private Set<SceneScheduler.Sequence> getSyncSet() {
         return this.scheduler.syncs.get(this.queue.element().sync);
      }

      private void popAndTryRun() {
         SceneScheduler.Instruction i = this.queue.remove();
         if (i.pInstruction != null) {
            this.builder.addInstruction(i.pInstruction);
         }
      }

      public int getDuration() {
         if (!this.independent) {
            SimDistUtil.getClientPlayer()
               .displayClientMessage(Component.literal("Getting independent timestamp of synced sequence " + this.id).withStyle(ChatFormatting.RED), false);
            Simulated.LOGGER.error("Getting independent timestamp of synced sequence " + this.id);
         }

         return this.duration;
      }

      public String toString() {
         if (this.isDone()) {
            return "Finished sequence " + this.id;
         } else {
            SceneScheduler.Instruction i = this.queue.element();
            if (i.idle != null) {
               return "Sequence " + this.id + " waiting " + i.idle + " ticks";
            } else {
               return i.pInstruction != null
                  ? "Sequence " + this.id + " running instruction " + i.pInstruction
                  : "Sequence " + this.id + " waiting for sync of " + i.sync;
            }
         }
      }
   }
}
