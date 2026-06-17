package dev.engine_room.flywheel.lib.material;

import dev.engine_room.flywheel.api.material.CutoutShader;
import net.minecraft.resources.ResourceLocation;

public record SimpleCutoutShader(ResourceLocation source) implements CutoutShader {
}
