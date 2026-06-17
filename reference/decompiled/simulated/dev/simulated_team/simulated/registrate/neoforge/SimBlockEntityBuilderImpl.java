package dev.simulated_team.simulated.registrate.neoforge;

import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.builders.BlockEntityBuilder;
import com.tterrag.registrate.builders.BuilderCallback;
import com.tterrag.registrate.builders.BlockEntityBuilder.BlockEntityFactory;
import com.tterrag.registrate.util.OneTimeEventReceiver;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer.Factory;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.mixin.accessor.CreateBlockEntityBuilderAccessor;
import dev.simulated_team.simulated.registrate.SimBlockEntityBuilder;
import java.util.function.Predicate;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

public class SimBlockEntityBuilderImpl<T extends BlockEntity, P> extends SimBlockEntityBuilder<T, P> {
   protected SimBlockEntityBuilderImpl(AbstractRegistrate<?> owner, P parent, String name, BuilderCallback callback, BlockEntityFactory<T> factory) {
      super(owner, parent, name, callback, factory);
   }

   public static <T extends BlockEntity, P> BlockEntityBuilder<T, P> create(
      AbstractRegistrate<?> owner, P parent, String name, BuilderCallback callback, BlockEntityFactory<T> factory
   ) {
      return new SimBlockEntityBuilderImpl<T, P>(owner, parent, name, callback, factory);
   }

   protected void registerVisualizer() {
      OneTimeEventReceiver.addModListener(
         Simulated.getRegistrate(),
         FMLClientSetupEvent.class,
         $ -> {
            NonNullSupplier<Factory<T>> visualFactory = ((CreateBlockEntityBuilderAccessor)this).getVisualFactory();
            if (visualFactory != null) {
               Predicate<T> renderNormally = ((CreateBlockEntityBuilderAccessor)this).getRenderNormally();
               SimpleBlockEntityVisualizer.builder((BlockEntityType)this.getEntry())
                  .factory((Factory)visualFactory.get())
                  .skipVanillaRender(be -> !renderNormally.test((T)be))
                  .apply();
            }
         }
      );
   }
}
