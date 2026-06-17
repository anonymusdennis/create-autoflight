package net.createmod.ponder.api.registration;

import net.minecraft.resources.ResourceLocation;

public interface MultiTagBuilder {
   public interface Component {
      MultiTagBuilder.Component add(ResourceLocation var1);
   }

   public interface Tag<T> {
      MultiTagBuilder.Tag<T> add(T var1);
   }
}
