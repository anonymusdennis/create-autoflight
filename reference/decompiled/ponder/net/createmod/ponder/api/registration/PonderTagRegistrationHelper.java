package net.createmod.ponder.api.registration;

import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;

public interface PonderTagRegistrationHelper<T> {
   <S> PonderTagRegistrationHelper<S> withKeyFunction(Function<S, T> var1);

   TagBuilder registerTag(ResourceLocation var1);

   TagBuilder registerTag(String var1);

   void addTagToComponent(T var1, ResourceLocation var2);

   MultiTagBuilder.Tag<T> addToTag(ResourceLocation var1);

   MultiTagBuilder.Tag<T> addToTag(ResourceLocation... var1);

   MultiTagBuilder.Component addToComponent(T var1);

   MultiTagBuilder.Component addToComponent(T... var1);
}
