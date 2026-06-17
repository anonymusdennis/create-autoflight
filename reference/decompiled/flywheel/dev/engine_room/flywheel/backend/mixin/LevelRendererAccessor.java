package dev.engine_room.flywheel.backend.mixin;

import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({LevelRenderer.class})
public interface LevelRendererAccessor {
   @Accessor("ticks")
   int flywheel$getTicks();
}
