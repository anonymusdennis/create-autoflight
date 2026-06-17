package dev.ryanhcode.sable.mixin.sublevel_render.fancy;

import com.mojang.blaze3d.shaders.Program;
import dev.ryanhcode.sable.mixinterface.sublevel_render.fancy.ProgramExtension;
import org.lwjgl.opengl.GL20C;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin({Program.class})
public class ProgramMixin implements ProgramExtension {
   @Shadow
   private int id;

   @Override
   public String sable$getSource() {
      return GL20C.glGetShaderSource(this.id);
   }
}
