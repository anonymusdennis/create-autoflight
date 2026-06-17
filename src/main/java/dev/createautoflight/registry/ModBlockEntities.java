package dev.createautoflight.registry;

import dev.createautoflight.CreateAutoflight;
import dev.createautoflight.content.gyroscope.GyroscopeBlockEntity;
import dev.createautoflight.content.navigation.NavigationBlockEntity;
import dev.createautoflight.content.thrust.DynamicThrustControllerBlockEntity;
import dev.createautoflight.content.thrust.ThrustVectoringGearboxBlockEntity;
import dev.createautoflight.content.thruster.ThrusterBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CreateAutoflight.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GyroscopeBlockEntity>> GYROSCOPE =
            BLOCK_ENTITIES.register("gyroscope", () ->
                    BlockEntityType.Builder.of(
                            GyroscopeBlockEntity::new,
                            ModBlocks.GYROSCOPE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ThrusterBlockEntity>> THRUSTER =
            BLOCK_ENTITIES.register("thruster", () ->
                    BlockEntityType.Builder.of(
                            ThrusterBlockEntity::new,
                            ModBlocks.THRUSTER.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NavigationBlockEntity>> NAVIGATION =
            BLOCK_ENTITIES.register("navigation", () ->
                    BlockEntityType.Builder.of(
                            NavigationBlockEntity::new,
                            ModBlocks.NAVIGATION.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ThrustVectoringGearboxBlockEntity>> THRUST_VECTORING_GEARBOX =
            BLOCK_ENTITIES.register("thrust_vectoring_gearbox", () ->
                    BlockEntityType.Builder.of(
                            ThrustVectoringGearboxBlockEntity::new,
                            ModBlocks.THRUST_VECTORING_GEARBOX.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DynamicThrustControllerBlockEntity>> DYNAMIC_THRUST_CONTROLLER =
            BLOCK_ENTITIES.register("dynamic_thrust_controller", () ->
                    BlockEntityType.Builder.of(
                            DynamicThrustControllerBlockEntity::new,
                            ModBlocks.DYNAMIC_THRUST_CONTROLLER.get()
                    ).build(null)
            );

    private ModBlockEntities() {}

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }
}
