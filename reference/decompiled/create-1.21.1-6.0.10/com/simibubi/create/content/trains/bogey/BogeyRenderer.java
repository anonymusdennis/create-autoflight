package com.simibubi.create.content.trains.bogey;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.nbt.CompoundTag;

public interface BogeyRenderer {
   void render(CompoundTag var1, float var2, float var3, PoseStack var4, MultiBufferSource var5, int var6, int var7, boolean var8);
}
