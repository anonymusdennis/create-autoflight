package com.simibubi.create.foundation.utility;

import java.util.concurrent.Callable;
import java.util.function.Supplier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLLoader;
import org.jetbrains.annotations.ApiStatus.Internal;

@Deprecated(
   forRemoval = true,
   since = "1.21"
)
@Internal
public class DistExecutor {
   @Deprecated(
      forRemoval = true,
      since = "1.21"
   )
   @Internal
   public static <T> T unsafeCallWhenOn(Dist dist, Supplier<Callable<T>> toRun) {
      if (FMLLoader.getDist() == dist) {
         try {
            return toRun.get().call();
         } catch (Exception var3) {
            throw new RuntimeException(var3);
         }
      } else {
         return null;
      }
   }
}
