package com.simibubi.create.foundation.data;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.api.behaviour.display.DisplayTarget;
import com.simibubi.create.api.registry.CreateRegistries;
import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.builders.BlockEntityBuilder;
import com.tterrag.registrate.builders.BuilderCallback;
import com.tterrag.registrate.builders.BlockEntityBuilder.BlockEntityFactory;
import com.tterrag.registrate.util.OneTimeEventReceiver;
import com.tterrag.registrate.util.entry.RegistryEntry;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer.Factory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.jetbrains.annotations.Nullable;

public class CreateBlockEntityBuilder<T extends BlockEntity, P> extends BlockEntityBuilder<T, P> {
   @Nullable
   private NonNullSupplier<Factory<T>> visualFactory;
   private Predicate<T> renderNormally;
   private Collection<NonNullSupplier<? extends Collection<NonNullSupplier<? extends Block>>>> deferredValidBlocks = new ArrayList<>();

   public static <T extends BlockEntity, P> BlockEntityBuilder<T, P> create(
      AbstractRegistrate<?> owner, P parent, String name, BuilderCallback callback, BlockEntityFactory<T> factory
   ) {
      return new CreateBlockEntityBuilder<>(owner, parent, name, callback, factory);
   }

   protected CreateBlockEntityBuilder(AbstractRegistrate<?> owner, P parent, String name, BuilderCallback callback, BlockEntityFactory<T> factory) {
      super(owner, parent, name, callback, factory);
   }

   public CreateBlockEntityBuilder<T, P> validBlocksDeferred(NonNullSupplier<? extends Collection<NonNullSupplier<? extends Block>>> blocks) {
      this.deferredValidBlocks.add(blocks);
      return this;
   }

   protected BlockEntityType<T> createEntry() {
      this.deferredValidBlocks.stream().map(Supplier::get).flatMap(Collection::stream).forEach(this::validBlock);
      return super.createEntry();
   }

   public CreateBlockEntityBuilder<T, P> displaySource(RegistryEntry<DisplaySource, ? extends DisplaySource> source) {
      this.onRegisterAfter(CreateRegistries.DISPLAY_SOURCE, type -> DisplaySource.BY_BLOCK_ENTITY.add(type, (DisplaySource)source.get()));
      return this;
   }

   public CreateBlockEntityBuilder<T, P> displayTarget(RegistryEntry<DisplayTarget, ? extends DisplayTarget> target) {
      this.onRegisterAfter(CreateRegistries.DISPLAY_TARGET, type -> DisplayTarget.BY_BLOCK_ENTITY.register(type, (DisplayTarget)target.get()));
      return this;
   }

   public CreateBlockEntityBuilder<T, P> visual(NonNullSupplier<Factory<T>> visualFactory) {
      return this.visual(visualFactory, true);
   }

   public CreateBlockEntityBuilder<T, P> visual(NonNullSupplier<Factory<T>> visualFactory, boolean renderNormally) {
      return this.visual(visualFactory, be -> renderNormally);
   }

   public CreateBlockEntityBuilder<T, P> visual(NonNullSupplier<Factory<T>> visualFactory, Predicate<T> renderNormally) {
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
               SimpleBlockEntityVisualizer.builder((BlockEntityType)this.getEntry())
                  .factory((Factory)visualFactory.get())
                  .skipVanillaRender(be -> !renderNormally.test((T)be))
                  .apply();
            }
         }
      );
   }
}
