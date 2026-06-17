package com.simibubi.create.foundation.data;

import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.builders.BuilderCallback;
import com.tterrag.registrate.builders.EntityBuilder;
import com.tterrag.registrate.util.OneTimeEventReceiver;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import dev.engine_room.flywheel.lib.visualization.SimpleEntityVisualizer;
import dev.engine_room.flywheel.lib.visualization.SimpleEntityVisualizer.Factory;
import java.util.function.Predicate;
import javax.annotation.ParametersAreNonnullByDefault;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType.EntityFactory;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.jetbrains.annotations.Nullable;

@ParametersAreNonnullByDefault
public class CreateEntityBuilder<T extends Entity, P> extends EntityBuilder<T, P> {
   @Nullable
   private NonNullSupplier<Factory<T>> visualFactory;
   private Predicate<T> renderNormally;

   public static <T extends Entity, P> EntityBuilder<T, P> create(
      AbstractRegistrate<?> owner, P parent, String name, BuilderCallback callback, EntityFactory<T> factory, MobCategory classification
   ) {
      return new CreateEntityBuilder(owner, parent, name, callback, factory, classification).defaultLang();
   }

   public CreateEntityBuilder(
      AbstractRegistrate<?> owner, P parent, String name, BuilderCallback callback, EntityFactory<T> factory, MobCategory classification
   ) {
      super(owner, parent, name, callback, factory, classification);
   }

   public CreateEntityBuilder<T, P> visual(NonNullSupplier<Factory<T>> visualFactory) {
      return this.visual(visualFactory, true);
   }

   public CreateEntityBuilder<T, P> visual(NonNullSupplier<Factory<T>> visualFactory, boolean renderNormally) {
      return this.visual(visualFactory, entity -> renderNormally);
   }

   public CreateEntityBuilder<T, P> visual(NonNullSupplier<Factory<T>> visualFactory, Predicate<T> renderNormally) {
      if (this.visualFactory == null) {
         CatnipServices.PLATFORM.executeOnClientOnly(() -> this::registerVisualizer);
      }

      this.visualFactory = visualFactory;
      this.renderNormally = renderNormally;
      return this;
   }

   protected void registerVisualizer() {
      OneTimeEventReceiver.addModListener(
         this.getOwner(),
         FMLClientSetupEvent.class,
         $ -> {
            NonNullSupplier<Factory<T>> visualFactory = this.visualFactory;
            if (visualFactory != null) {
               Predicate<T> renderNormally = this.renderNormally;
               SimpleEntityVisualizer.builder((EntityType)this.getEntry())
                  .factory((Factory)visualFactory.get())
                  .skipVanillaRender(entity -> !renderNormally.test((T)entity))
                  .apply();
            }
         }
      );
   }
}
