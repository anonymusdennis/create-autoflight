package dev.engine_room.flywheel.lib.instance;

import dev.engine_room.flywheel.api.instance.Instance;
import java.util.Iterator;
import java.util.stream.Stream;
import net.minecraft.client.renderer.LightTexture;
import org.jetbrains.annotations.Nullable;

public interface FlatLit extends Instance {
   FlatLit light(int var1);

   default FlatLit light(int blockLight, int skyLight) {
      return this.light(LightTexture.pack(blockLight, skyLight));
   }

   static void relight(int packedLight, @Nullable FlatLit... instances) {
      for (FlatLit instance : instances) {
         if (instance != null) {
            instance.light(packedLight).handle().setChanged();
         }
      }
   }

   static void relight(int packedLight, Iterator<FlatLit> instances) {
      while (instances.hasNext()) {
         FlatLit instance = instances.next();
         if (instance != null) {
            instance.light(packedLight).handle().setChanged();
         }
      }
   }

   static void relight(int packedLight, Iterable<FlatLit> instances) {
      relight(packedLight, instances.iterator());
   }

   static void relight(int packedLight, Stream<FlatLit> instances) {
      relight(packedLight, instances.iterator());
   }
}
