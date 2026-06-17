package com.simibubi.create.compat.jei;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.Supplier;
import mezz.jei.api.gui.drawable.IDrawable;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

public class ItemIcon implements IDrawable {
   private Supplier<ItemStack> supplier;
   private ItemStack stack;

   public ItemIcon(Supplier<ItemStack> stack) {
      this.supplier = stack;
   }

   public int getWidth() {
      return 18;
   }

   public int getHeight() {
      return 18;
   }

   public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
      PoseStack matrixStack = graphics.pose();
      if (this.stack == null) {
         this.stack = this.supplier.get();
      }

      RenderSystem.enableDepthTest();
      matrixStack.pushPose();
      matrixStack.translate((float)(xOffset + 1), (float)(yOffset + 1), 0.0F);
      GuiGameElement.of(this.stack).render(graphics);
      matrixStack.popPose();
   }
}
