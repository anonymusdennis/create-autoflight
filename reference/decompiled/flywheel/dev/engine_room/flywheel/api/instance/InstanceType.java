package dev.engine_room.flywheel.api.instance;

import dev.engine_room.flywheel.api.layout.Layout;
import net.minecraft.resources.ResourceLocation;

public interface InstanceType<I extends Instance> {
   I create(InstanceHandle var1);

   Layout layout();

   InstanceWriter<I> writer();

   ResourceLocation vertexShader();

   ResourceLocation cullShader();
}
