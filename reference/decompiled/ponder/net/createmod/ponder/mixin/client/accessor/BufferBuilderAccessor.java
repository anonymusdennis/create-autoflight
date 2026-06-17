package net.createmod.ponder.mixin.client.accessor;

import com.mojang.blaze3d.vertex.BufferBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({BufferBuilder.class})
public interface BufferBuilderAccessor {
   @Accessor("vertices")
   int catnip$getVertices();
}
