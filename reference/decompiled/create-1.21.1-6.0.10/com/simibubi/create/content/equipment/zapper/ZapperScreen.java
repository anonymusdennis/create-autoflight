package com.simibubi.create.content.equipment.zapper;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.utility.CreateLang;
import java.util.ArrayList;
import java.util.List;
import net.createmod.catnip.gui.AbstractSimiScreen;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public abstract class ZapperScreen extends AbstractSimiScreen {
   protected final Component patternSection = CreateLang.translateDirect("gui.terrainzapper.patternSection");
   protected AllGuiTextures background;
   protected ItemStack zapper;
   protected InteractionHand hand;
   protected float animationProgress;
   protected Component title;
   protected List<IconButton> patternButtons = new ArrayList<>(6);
   private IconButton confirmButton;
   protected int brightColor;
   protected int fontColor;
   protected PlacementPatterns currentPattern;

   public ZapperScreen(AllGuiTextures background, ItemStack zapper, InteractionHand hand) {
      this.background = background;
      this.zapper = zapper;
      this.hand = hand;
      this.title = CommonComponents.EMPTY;
      this.brightColor = 16711422;
      this.fontColor = 5726074;
      this.currentPattern = (PlacementPatterns)zapper.getOrDefault(AllDataComponents.PLACEMENT_PATTERN, PlacementPatterns.Solid);
   }

   protected void init() {
      this.setWindowSize(this.background.getWidth(), this.background.getHeight());
      this.setWindowOffset(-10, 0);
      super.init();
      this.animationProgress = 0.0F;
      int x = this.guiLeft;
      int y = this.guiTop;
      this.confirmButton = new IconButton(x + this.background.getWidth() - 33, y + this.background.getHeight() - 24, AllIcons.I_CONFIRM);
      this.confirmButton.withCallback(this::onClose);
      this.addRenderableWidget(this.confirmButton);
      this.patternButtons.clear();

      for (int row = 0; row <= 1; row++) {
         for (int col = 0; col <= 2; col++) {
            int id = this.patternButtons.size();
            PlacementPatterns pattern = PlacementPatterns.values()[id];
            IconButton patternButton = new IconButton(x + this.background.getWidth() - 76 + col * 18, y + 21 + row * 18, pattern.icon);
            patternButton.withCallback(() -> {
               this.patternButtons.forEach(b -> b.green = false);
               patternButton.green = true;
               this.currentPattern = pattern;
            });
            patternButton.setToolTip(CreateLang.translateDirect("gui.terrainzapper.pattern." + pattern.translationKey));
            this.patternButtons.add(patternButton);
         }
      }

      this.patternButtons.get(this.currentPattern.ordinal()).green = true;
      this.addRenderableWidgets(this.patternButtons);
   }

   protected void renderWindow(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      int x = this.guiLeft;
      int y = this.guiTop;
      this.background.render(graphics, x, y);
      this.drawOnBackground(graphics, x, y);
      this.renderBlock(graphics, x, y);
      this.renderZapper(graphics, x, y);
   }

   protected void drawOnBackground(GuiGraphics graphics, int x, int y) {
      graphics.drawString(this.font, this.title, x + (this.background.getWidth() - this.font.width(this.title)) / 2, y + 4, 5513551, false);
   }

   public void tick() {
      super.tick();
      this.animationProgress += 5.0F;
   }

   public void removed() {
      ConfigureZapperPacket packet = this.getConfigurationPacket();
      packet.configureZapper(this.zapper);
      CatnipServices.NETWORK.sendToServer(packet);
   }

   protected void renderZapper(GuiGraphics graphics, int x, int y) {
      GuiGameElement.of(this.zapper)
         .scale(4.0)
         .at((float)(x + this.background.getWidth()), (float)(y + this.background.getHeight() - 48), -200.0F)
         .render(graphics);
   }

   protected void renderBlock(GuiGraphics graphics, int x, int y) {
      PoseStack ms = graphics.pose();
      ms.pushPose();
      ms.translate((float)(x + 32), (float)(y + 42), 120.0F);
      ms.mulPose(Axis.XP.rotationDegrees(-25.0F));
      ms.mulPose(Axis.YP.rotationDegrees(-45.0F));
      ms.scale(20.0F, 20.0F, 20.0F);
      BlockState state = (BlockState)this.zapper.getOrDefault(AllDataComponents.SHAPER_BLOCK_USED, Blocks.AIR.defaultBlockState());
      GuiGameElement.of(state).render(graphics);
      ms.popPose();
   }

   protected abstract ConfigureZapperPacket getConfigurationPacket();
}
