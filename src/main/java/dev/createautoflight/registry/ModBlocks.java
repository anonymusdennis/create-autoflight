package dev.createautoflight.registry;

import dev.createautoflight.CreateAutoflight;
import dev.createautoflight.content.gyroscope.GyroscopeBlock;
import dev.createautoflight.content.navigation.NavigationBlock;
import dev.createautoflight.content.thrust.DynamicThrustControllerBlock;
import dev.createautoflight.content.thrust.ThrustVectoringGearboxBlock;
import dev.createautoflight.content.thruster.ThrusterBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(CreateAutoflight.MOD_ID);
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(CreateAutoflight.MOD_ID);

    public static final DeferredBlock<GyroscopeBlock> GYROSCOPE = BLOCKS.register(
            "gyroscope",
            () -> new GyroscopeBlock(BlockBehaviour.Properties.of().strength(3.5f).noOcclusion())
    );

    public static final DeferredBlock<ThrusterBlock> THRUSTER = BLOCKS.register(
            "thruster",
            () -> new ThrusterBlock(BlockBehaviour.Properties.of().strength(3.5f))
    );

    public static final DeferredBlock<NavigationBlock> NAVIGATION = BLOCKS.register(
            "navigation",
            () -> new NavigationBlock(BlockBehaviour.Properties.of().strength(3.5f))
    );

    public static final DeferredBlock<ThrustVectoringGearboxBlock> THRUST_VECTORING_GEARBOX = BLOCKS.register(
            "thrust_vectoring_gearbox",
            () -> new ThrustVectoringGearboxBlock(BlockBehaviour.Properties.of().strength(3.5f).noOcclusion())
    );

    public static final DeferredBlock<DynamicThrustControllerBlock> DYNAMIC_THRUST_CONTROLLER = BLOCKS.register(
            "dynamic_thrust_controller",
            () -> new DynamicThrustControllerBlock(BlockBehaviour.Properties.of().strength(3.5f).noOcclusion())
    );

    public static final DeferredItem<BlockItem> GYROSCOPE_ITEM = ITEMS.register(
            "gyroscope",
            () -> new BlockItem(GYROSCOPE.get(), new Item.Properties())
    );

    public static final DeferredItem<BlockItem> THRUSTER_ITEM = ITEMS.register(
            "thruster",
            () -> new BlockItem(THRUSTER.get(), new Item.Properties())
    );

    public static final DeferredItem<BlockItem> NAVIGATION_ITEM = ITEMS.register(
            "navigation",
            () -> new BlockItem(NAVIGATION.get(), new Item.Properties())
    );

    public static final DeferredItem<BlockItem> THRUST_VECTORING_GEARBOX_ITEM = ITEMS.register(
            "thrust_vectoring_gearbox",
            () -> new BlockItem(THRUST_VECTORING_GEARBOX.get(), new Item.Properties())
    );

    public static final DeferredItem<BlockItem> DYNAMIC_THRUST_CONTROLLER_ITEM = ITEMS.register(
            "dynamic_thrust_controller",
            () -> new BlockItem(DYNAMIC_THRUST_CONTROLLER.get(), new Item.Properties())
    );

    private ModBlocks() {}

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }
}
