package dev.createautoflight.client;

import dev.createautoflight.CreateAutoflight;
import net.minecraft.resources.ResourceLocation;

public final class ResourceHelper {
    private ResourceHelper() {}

    public static ResourceLocation gyroModel(String path) {
        return ResourceLocation.fromNamespaceAndPath(CreateAutoflight.MOD_ID, "block/" + path);
    }
}
