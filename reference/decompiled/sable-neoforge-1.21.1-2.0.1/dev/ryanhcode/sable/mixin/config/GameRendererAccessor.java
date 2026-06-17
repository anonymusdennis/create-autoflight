package dev.ryanhcode.sable.mixin.config;

import java.util.Map;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({GameRenderer.class})
public interface GameRendererAccessor {
   @Accessor
   Map<String, ShaderInstance> getShaders();
}
