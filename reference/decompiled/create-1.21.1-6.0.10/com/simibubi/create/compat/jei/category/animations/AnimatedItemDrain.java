package com.simibubi.create.compat.jei.category.animations;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllBlocks;
import net.createmod.catnip.gui.UIRenderHelper;
import net.createmod.catnip.platform.NeoForgeCatnipServices;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.fluids.FluidStack;

public class AnimatedItemDrain extends AnimatedKinetics {
   private FluidStack fluid;

   public AnimatedItemDrain withFluid(FluidStack fluid) {
      this.fluid = fluid;
      return this;
   }

   public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
      PoseStack matrixStack = graphics.pose();
      matrixStack.pushPose();
      matrixStack.translate((float)xOffset, (float)yOffset, 100.0F);
      matrixStack.mulPose(Axis.XP.rotationDegrees(-15.5F));
      matrixStack.mulPose(Axis.YP.rotationDegrees(22.5F));
      int scale = 20;
      this.blockElement(AllBlocks.ITEM_DRAIN.getDefaultState()).scale((double)scale).render(graphics);
      UIRenderHelper.flipForGuiRender(matrixStack);
      matrixStack.scale((float)scale, (float)scale, (float)scale);
      float from = 0.125F;
      float to = 1.0F - from;
      NeoForgeCatnipServices.FLUID_RENDERER
         .renderFluidBox(this.fluid, from, from, from, to, 0.75F, to, graphics.bufferSource(), matrixStack, 15728880, false, true);
      graphics.flush();
      matrixStack.popPose();
   }
}
