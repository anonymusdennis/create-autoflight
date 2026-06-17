package dev.ryanhcode.sable.mixin.sublevel_render;

import java.util.Set;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher.RenderSection;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({RenderSection.class})
public interface RenderSectionAccessor {
   @Accessor
   Set<BlockEntity> getGlobalBlockEntities();
}
