package dev.createautoflight;

import dev.createautoflight.registry.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreateAutoflight.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.create_autoflight"))
                    .icon(() -> new ItemStack(ModBlocks.GYROSCOPE_ITEM.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModBlocks.GYROSCOPE_ITEM.get());
                        output.accept(ModBlocks.THRUSTER_ITEM.get());
                        output.accept(ModBlocks.NAVIGATION_ITEM.get());
                        output.accept(ModBlocks.THRUST_VECTORING_GEARBOX_ITEM.get());
                        output.accept(ModBlocks.DYNAMIC_THRUST_CONTROLLER_ITEM.get());
                    })
                    .build());

    private ModCreativeTabs() {}

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }
}
