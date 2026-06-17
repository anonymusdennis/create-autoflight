package com.simibubi.create.foundation.mixin.accessor;

import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import java.util.Map;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Holder.Reference;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({MappedRegistry.class})
public interface MappedRegistryAccessor<T> {
   @Accessor
   Reference2IntMap<T> getToId();

   @Accessor
   Map<T, Reference<T>> getByValue();
}
