package dev.engine_room.flywheel.lib.model.baked;

import dev.engine_room.flywheel.api.material.Material;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.Nullable;

public interface BlockMaterialFunction {
   @Nullable
   Material apply(RenderType var1, boolean var2, boolean var3);
}
