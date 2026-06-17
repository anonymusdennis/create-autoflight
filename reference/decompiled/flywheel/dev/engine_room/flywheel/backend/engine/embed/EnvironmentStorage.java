package dev.engine_room.flywheel.backend.engine.embed;

import dev.engine_room.flywheel.backend.engine.CpuArena;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;

public class EnvironmentStorage {
   public static final int MATRIX_SIZE_BYTES = 112;
   protected final Object lock = new Object();
   protected final ReferenceSet<EmbeddedEnvironment> environments = new ReferenceLinkedOpenHashSet();
   public final CpuArena arena = new CpuArena(112L, 32);

   public EnvironmentStorage() {
      this.arena.alloc();
   }

   public void track(EmbeddedEnvironment environment) {
      synchronized (this.lock) {
         if (this.environments.add(environment)) {
            environment.matrixIndex = this.arena.alloc();
         }
      }
   }

   public void flush() {
      this.environments.removeIf(embeddedEnvironment -> {
         boolean deleted = embeddedEnvironment.isDeleted();
         if (deleted && embeddedEnvironment.matrixIndex > 0) {
            this.arena.free(embeddedEnvironment.matrixIndex);
         }

         return deleted;
      });
      ObjectIterator var1 = this.environments.iterator();

      while (var1.hasNext()) {
         EmbeddedEnvironment environment = (EmbeddedEnvironment)var1.next();
         environment.flush(this.arena.indexToPointer(environment.matrixIndex));
      }
   }

   public void delete() {
      this.arena.delete();
   }
}
