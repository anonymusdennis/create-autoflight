package dev.ryanhcode.sable.api.physics;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Comparator;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public interface PhysicsPipelineProvider {
   PhysicsPipelineProvider INSTANCE = ServiceLoader.load(PhysicsPipelineProvider.class).stream().max(Comparator.comparingInt(provider -> {
      Class<? extends PhysicsPipelineProvider> type = provider.type();
      PhysicsPipelineProvider.LoadPriority annotation = type.getAnnotation(PhysicsPipelineProvider.LoadPriority.class);
      return annotation != null ? annotation.value() : 1000;
   })).map(Provider::get).orElseThrow(() -> new RuntimeException("Failed to find any physics pipeline providers"));

   @NotNull
   @Contract(
      value = "_ -> new",
      pure = true
   )
   PhysicsPipeline createPipeline(@NotNull ServerLevel var1);

   @Retention(RetentionPolicy.RUNTIME)
   public @interface LoadPriority {
      int value() default 1000;
   }
}
