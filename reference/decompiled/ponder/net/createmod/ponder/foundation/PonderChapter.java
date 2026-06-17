package net.createmod.ponder.foundation;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.createmod.catnip.gui.element.ScreenElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class PonderChapter implements ScreenElement {
   private final ResourceLocation id;
   private final ResourceLocation icon;

   private PonderChapter(ResourceLocation id) {
      this.id = id;
      this.icon = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "textures/ponder/chapter/" + id.getPath() + ".png");
   }

   public ResourceLocation getId() {
      return this.id;
   }

   public String getTitle() {
      return "";
   }

   @Override
   public void render(GuiGraphics graphics, int x, int y) {
      PoseStack ms = graphics.pose();
      ms.pushPose();
      RenderSystem.setShaderTexture(0, this.icon);
      ms.scale(0.25F, 0.25F, 1.0F);
      graphics.blit(this.icon, x, y, 0, 0.0F, 0.0F, 64, 64, 64, 64);
      ms.popPose();
   }

   @Deprecated
   public static PonderChapter of(ResourceLocation id) {
      return null;
   }
}
