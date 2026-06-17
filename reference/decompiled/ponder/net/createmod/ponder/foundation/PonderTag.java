package net.createmod.ponder.foundation;

import com.mojang.blaze3d.vertex.PoseStack;
import javax.annotation.Nullable;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.createmod.catnip.gui.element.ScreenElement;
import net.createmod.ponder.Ponder;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class PonderTag implements ScreenElement {
   private final ResourceLocation id;
   @Nullable
   private final ResourceLocation textureIconLocation;
   private final ItemStack itemIcon;
   private final ItemStack mainItem;

   public PonderTag(ResourceLocation id, @Nullable ResourceLocation textureIconLocation, ItemStack itemIcon, ItemStack mainItem) {
      this.id = id;
      this.textureIconLocation = textureIconLocation;
      this.itemIcon = itemIcon;
      this.mainItem = mainItem;
   }

   public ResourceLocation getId() {
      return this.id;
   }

   public ItemStack getMainItem() {
      return this.mainItem;
   }

   public String getTitle() {
      return PonderIndex.getLangAccess().getTagName(this.id);
   }

   public String getDescription() {
      return PonderIndex.getLangAccess().getTagDescription(this.id);
   }

   @Override
   public void render(GuiGraphics graphics, int x, int y) {
      PoseStack poseStack = graphics.pose();
      poseStack.pushPose();
      poseStack.translate((float)x, (float)y, 0.0F);
      if (this.textureIconLocation != null) {
         poseStack.scale(0.25F, 0.25F, 1.0F);
         graphics.blit(this.textureIconLocation, 0, 0, 0, 0.0F, 0.0F, 64, 64, 64, 64);
      } else if (!this.itemIcon.isEmpty()) {
         poseStack.translate(-2.0F, -2.0F, 0.0F);
         poseStack.scale(1.25F, 1.25F, 1.25F);
         GuiGameElement.of(this.itemIcon).render(graphics);
      }

      poseStack.popPose();
   }

   @Override
   public boolean equals(Object other) {
      if (this == other) {
         return true;
      } else {
         return other instanceof PonderTag otherTag ? this.getId().equals(otherTag.getId()) : false;
      }
   }

   public static final class Highlight {
      public static final ResourceLocation ALL = Ponder.asResource("_all");
   }
}
