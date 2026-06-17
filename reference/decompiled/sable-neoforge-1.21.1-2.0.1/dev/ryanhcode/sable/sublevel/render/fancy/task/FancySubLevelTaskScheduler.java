package dev.ryanhcode.sable.sublevel.render.fancy.task;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.render.fancy.FancySubLevelSectionCompiler;
import dev.ryanhcode.sable.sublevel.render.fancy.SubLevelMeshBuilder;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FancySubLevelTaskScheduler {
   private final SubLevelTask.MeshUploader meshUploader;
   private final Thread[] threads;
   private final AtomicBoolean running;
   private final Lock taskLock;
   private final Condition hasWork;
   private final Queue<FancySubLevelTaskScheduler.Task> tasks;

   public FancySubLevelTaskScheduler(SubLevelTask.MeshUploader meshUploader, int threadCount) {
      this.meshUploader = meshUploader;
      this.threads = new Thread[threadCount];
      this.running = new AtomicBoolean(false);
      this.taskLock = new ReentrantLock();
      this.hasWork = this.taskLock.newCondition();
      this.tasks = new PriorityBlockingQueue<>();
   }

   private void runTask() {
      SectionBufferBuilderPack pack = new SectionBufferBuilderPack();

      try {
         while (true) {
            FancySubLevelTaskScheduler.Task task;
            try {
               this.taskLock.lock();
               task = this.tasks.poll();
               if (task == null) {
                  if (!this.running.get()) {
                     break;
                  }

                  this.hasWork.awaitUninterruptibly();
                  continue;
               }
            } finally {
               this.taskLock.unlock();
            }

            try {
               task.task.process(pack, this.meshUploader);
            } catch (Throwable var9) {
               Sable.LOGGER.error("Error running sub-level task", var9);
            }

            if (task.onComplete != null) {
               task.onComplete.run();
            }
         }
      } catch (Throwable var11) {
         try {
            pack.close();
         } catch (Throwable var8) {
            var11.addSuppressed(var8);
         }

         throw var11;
      }

      pack.close();
   }

   public void start() {
      if (this.running.compareAndSet(false, true)) {
         for (int i = 0; i < this.threads.length; i++) {
            Thread thread = new Thread(this::runTask, "FancySubLevelTaskScheduler#" + i);
            thread.setPriority(3);
            thread.start();
            this.threads[i] = thread;
         }
      }
   }

   public void stop() {
      if (this.running.compareAndSet(true, false)) {
         this.hasWork.signalAll();

         for (Thread thread : this.threads) {
            try {
               thread.join();
            } catch (InterruptedException var6) {
               Sable.LOGGER.error("Error shutting down task thread", var6);
            }
         }
      }
   }

   public void schedule(SubLevelTask task, double distance, @Nullable Runnable onComplete) {
      if (!this.running.get()) {
         throw new IllegalStateException("SubLevelTaskScheduler is not running");
      } else {
         try {
            this.taskLock.lock();
            this.tasks.add(new FancySubLevelTaskScheduler.Task(task, distance, onComplete));
            this.hasWork.signal();
         } finally {
            this.taskLock.unlock();
         }
      }
   }

   public void scheduleCompile(
      FancySubLevelSectionCompiler.RenderSection section,
      @Nullable RenderChunkRegion renderChunkRegion,
      double distance,
      @Nullable Consumer<FancySubLevelSectionCompiler.RenderSection> onComplete
   ) {
      if (renderChunkRegion == null) {
         section.setCompiledSection(FancySubLevelSectionCompiler.CompiledSection.EMPTY);
      } else {
         this.schedule((pack, uploader) -> {
            SubLevelMeshBuilder.Results results = uploader.getMeshBuilder().compile(section.getOrigin(), section.getPos(), renderChunkRegion, pack);
            section.setCompiledSection(FancySubLevelSectionCompiler.CompiledSection.create(results, uploader));
         }, distance, onComplete != null ? () -> onComplete.accept(section) : null);
      }
   }

   private static record Task(SubLevelTask task, double distance, @Nullable Runnable onComplete) implements Comparable<FancySubLevelTaskScheduler.Task> {
      public int compareTo(@NotNull FancySubLevelTaskScheduler.Task o) {
         return Double.compare(this.distance, o.distance);
      }
   }
}
