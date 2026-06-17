package dev.ryanhcode.sable.api.sublevel.ticket;

import com.mojang.serialization.Codec;
import dev.ryanhcode.sable.Sable;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record SubLevelLoadingTicketType<T>(ResourceLocation name, Codec<T> codec) {
   private static final Map<ResourceLocation, SubLevelLoadingTicketType<?>> REGISTRY = new HashMap<>();
   public static final SubLevelLoadingTicketType<Unit> COMMAND_FORCED = create(Sable.sablePath("command_forced"), Unit.CODEC);

   public static <T> SubLevelLoadingTicketType<T> create(ResourceLocation name, Codec<T> codec) {
      SubLevelLoadingTicketType<T> type = new SubLevelLoadingTicketType<>(name, codec);
      REGISTRY.put(name, type);
      return type;
   }

   @Nullable
   public static SubLevelLoadingTicketType<?> byName(ResourceLocation name) {
      return REGISTRY.get(name);
   }

   @NotNull
   @Override
   public String toString() {
      return "SubLevelTicketType{name=" + this.name + "}";
   }
}
