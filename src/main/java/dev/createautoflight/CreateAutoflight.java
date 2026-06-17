package dev.createautoflight;

import com.mojang.logging.LogUtils;
import dev.createautoflight.network.ModPackets;
import dev.createautoflight.registry.ModBlockEntities;
import dev.createautoflight.registry.ModBlocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(CreateAutoflight.MOD_ID)
public final class CreateAutoflight {
    public static final String MOD_ID = "create_autoflight";
    private static final Logger LOGGER = LogUtils.getLogger();

    public CreateAutoflight(IEventBus modBus) {
        ModBlocks.register(modBus);
        ModBlockEntities.register(modBus);
        ModCreativeTabs.register(modBus);
        ModPackets.register();
        LOGGER.info("Create AutoFlight loaded ({})", FMLEnvironment.dist);
    }
}
