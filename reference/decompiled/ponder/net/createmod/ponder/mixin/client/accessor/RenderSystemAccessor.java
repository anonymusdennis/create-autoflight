package net.createmod.ponder.mixin.client.accessor;

import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({RenderSystem.class})
public interface RenderSystemAccessor {
   @Accessor(
      value = "shaderLightDirections",
      remap = false
   )
   static Vector3f[] catnip$getShaderLightDirections() {
      throw new AssertionError();
   }
}
