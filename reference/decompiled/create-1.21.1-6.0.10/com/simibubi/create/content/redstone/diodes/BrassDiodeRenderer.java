package com.simibubi.create.content.redstone.diodes;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.blockEntity.renderer.ColoredOverlayBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;

public class BrassDiodeRenderer extends ColoredOverlayBlockEntityRenderer<BrassDiodeBlockEntity> {
   public BrassDiodeRenderer(Context context) {
      super(context);
   }

   protected int getColor(BrassDiodeBlockEntity be, float partialTicks) {
      return Color.mixColors(2884352, 13434880, be.getProgress());
   }

   protected SuperByteBuffer getOverlayBuffer(BrassDiodeBlockEntity be) {
      return CachedBuffers.partial(AllPartialModels.FLEXPEATER_INDICATOR, be.getBlockState());
   }
}
