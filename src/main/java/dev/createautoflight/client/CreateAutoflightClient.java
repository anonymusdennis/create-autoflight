package dev.createautoflight.client;

import com.simibubi.create.content.kinetics.transmission.SplitShaftRenderer;
import com.simibubi.create.content.kinetics.transmission.SplitShaftVisual;
import dev.createautoflight.CreateAutoflight;
import dev.createautoflight.registry.ModBlockEntities;
import dev.engine_room.flywheel.api.visualization.VisualizerRegistry;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;

@EventBusSubscriber(modid = CreateAutoflight.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CreateAutoflightClient {
    private CreateAutoflightClient() {}

    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        // The gyroscope renderer draws these as standalone models on top of the static base;
        // they only bake (and therefore render) if explicitly registered here.
        event.register(GyroscopeRenderer.RING_YAW);
        event.register(GyroscopeRenderer.RING_ROLL);
        event.register(GyroscopeRenderer.RING_PITCH);
        event.register(GyroscopeRenderer.GLASS);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.GYROSCOPE.get(), GyroscopeRenderer::new);
        event.registerBlockEntityRenderer(
                ModBlockEntities.THRUST_VECTORING_GEARBOX.get(),
                SplitShaftRenderer::new
        );

        VisualizerRegistry.setVisualizer(
                ModBlockEntities.THRUST_VECTORING_GEARBOX.get(),
                SimpleBlockEntityVisualizer.builder(ModBlockEntities.THRUST_VECTORING_GEARBOX.get())
                        .factory(SplitShaftVisual::new)
                        .apply()
        );
    }
}
