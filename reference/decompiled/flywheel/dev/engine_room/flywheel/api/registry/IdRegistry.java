package dev.engine_room.flywheel.api.registry;

import java.util.Collection;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

@NonExtendable
public interface IdRegistry<T> extends Iterable<T> {
   void register(ResourceLocation var1, T var2);

   <S extends T> S registerAndGet(ResourceLocation var1, S var2);

   @Nullable
   T get(ResourceLocation var1);

   @Nullable
   ResourceLocation getId(T var1);

   T getOrThrow(ResourceLocation var1);

   ResourceLocation getIdOrThrow(T var1);

   @UnmodifiableView
   Set<ResourceLocation> getAllIds();

   @UnmodifiableView
   Collection<T> getAll();

   boolean isFrozen();
}
