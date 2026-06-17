package net.createmod.catnip.client.render.model;

import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.client.renderer.RenderType;

public interface ShadeSeparatedResultConsumer {
   void accept(RenderType var1, boolean var2, MeshData var3);
}
