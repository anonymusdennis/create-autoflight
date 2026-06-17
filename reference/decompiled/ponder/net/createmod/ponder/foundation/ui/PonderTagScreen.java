package net.createmod.ponder.foundation.ui;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.createmod.catnip.gui.NavigatableSimiScreen;
import net.createmod.catnip.gui.ScreenOpener;
import net.createmod.catnip.gui.UIRenderHelper;
import net.createmod.catnip.gui.element.BoxElement;
import net.createmod.catnip.gui.element.RenderElement;
import net.createmod.catnip.gui.widget.BoxWidget;
import net.createmod.catnip.lang.ClientFontHelper;
import net.createmod.catnip.layout.LayoutHelper;
import net.createmod.catnip.registry.RegisteredObjectsHelper;
import net.createmod.ponder.Ponder;
import net.createmod.ponder.foundation.PonderChapter;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.foundation.PonderTag;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public class PonderTagScreen extends AbstractPonderScreen {
   private final PonderTag tag;
   protected final List<PonderTagScreen.ItemEntry> items = new ArrayList<>();
   private final double itemXmult = 0.5;
   @Nullable
   protected Rect2i itemArea;
   protected final List<PonderChapter> chapters = new ArrayList<>();
   private final double chapterXmult = 0.5;
   private final double chapterYmult = 0.75;
   @Nullable
   protected Rect2i chapterArea;
   private final double mainYmult = 0.15;
   private ItemStack hoveredItem = ItemStack.EMPTY;

   public PonderTagScreen(ResourceLocation tag) {
      this.tag = PonderIndex.getTagAccess().getRegisteredTag(tag);
   }

   public PonderTagScreen(PonderTag tag) {
      this.tag = tag;
   }

   @Override
   protected void init() {
      super.init();
      this.items.clear();
      PonderIndex.getTagAccess()
         .getItems(this.tag)
         .stream()
         .map(key -> new PonderTagScreen.ItemEntry(RegisteredObjectsHelper.getItemOrBlock(key), key))
         .filter(entry -> entry.item != null)
         .forEach(this.items::add);
      if (!this.tag.getMainItem().isEmpty()) {
         this.items.removeIf(entry -> entry.item == this.tag.getMainItem().getItem());
      }

      int rowCount = Mth.clamp((int)Math.ceil((double)this.items.size() / 11.0), 1, 3);
      LayoutHelper layout = LayoutHelper.centeredHorizontal(this.items.size(), rowCount, 28, 28, 8);
      this.itemArea = layout.getArea();
      int itemCenterX = (int)((double)this.width * 0.5);
      int itemCenterY = this.getItemsY();

      for (PonderTagScreen.ItemEntry entry : this.items) {
         PonderButton b = new PonderButton(itemCenterX + layout.getX() + 4, itemCenterY + layout.getY() + 4).showing(new ItemStack(entry.item));
         if (PonderIndex.getSceneAccess().doScenesExistForId(entry.key)) {
            b.withCallback((mouseX, mouseY) -> {
               this.centerScalingOn(mouseX, mouseY);
               ScreenOpener.transitionTo(PonderUI.of(new ItemStack(entry.item), this.tag));
            });
         } else {
            b.<BoxWidget>withBorderColors(entry.key.getNamespace().equals("minecraft") ? PonderUI.MISSING_VANILLA_ENTRY : PonderUI.MISSING_MODDED_ENTRY)
               .animateColors(false);
         }

         this.addRenderableWidget(b);
         layout.next();
      }

      if (!this.tag.getMainItem().isEmpty()) {
         ResourceLocation registryName = RegisteredObjectsHelper.getKeyOrThrow(this.tag.getMainItem().getItem());
         PonderButton b = new PonderButton(itemCenterX - layout.getTotalWidth() / 2 - 48, itemCenterY - 10).showing(this.tag.getMainItem());
         if (PonderIndex.getSceneAccess().doScenesExistForId(registryName)) {
            b.withCallback((mouseX, mouseY) -> {
               this.centerScalingOn(mouseX, mouseY);
               ScreenOpener.transitionTo(PonderUI.of(this.tag.getMainItem(), this.tag));
            });
         } else {
            b.<BoxWidget>withBorderColors(registryName.getNamespace().equals("minecraft") ? PonderUI.MISSING_VANILLA_ENTRY : PonderUI.MISSING_MODDED_ENTRY)
               .animateColors(false);
         }

         this.addRenderableWidget(b);
      }
   }

   @Override
   protected void initBackTrackIcon(BoxWidget backTrack) {
      backTrack.showing(this.tag);
   }

   @Override
   public void tick() {
      super.tick();
      PonderUI.ponderTicks++;
      this.hoveredItem = ItemStack.EMPTY;
      Window w = this.minecraft.getWindow();
      int mX = (int)(this.minecraft.mouseHandler.xpos() * (double)w.getGuiScaledWidth() / (double)w.getScreenWidth());
      int mY = (int)(this.minecraft.mouseHandler.ypos() * (double)w.getGuiScaledHeight() / (double)w.getScreenHeight());

      for (GuiEventListener child : this.children()) {
         if (child != this.backTrack && child instanceof PonderButton) {
            PonderButton button = (PonderButton)child;
            if (button.isMouseOver((double)mX, (double)mY)) {
               this.hoveredItem = button.getItem();
            }
         }
      }
   }

   @Override
   protected void renderWindow(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      super.renderWindow(graphics, mouseX, mouseY, partialTicks);
      this.renderItems(graphics, mouseX, mouseY, partialTicks);
      this.renderChapters(graphics, mouseX, mouseY, partialTicks);
      PoseStack poseStack = graphics.pose();
      poseStack.pushPose();
      poseStack.translate((double)(this.width / 2 - 120), (double)this.height * 0.15 - 40.0, 0.0);
      poseStack.pushPose();
      int x = 59;
      int y = 31;
      String title = this.tag.getTitle();
      int streakHeight = 35;
      UIRenderHelper.streak(graphics, 0.0F, x - 4, y - 12 + streakHeight / 2, streakHeight, 240);
      new BoxElement()
         .<BoxElement>withBackground(PonderUI.BACKGROUND_FLAT)
         .<BoxElement>gradientBorder(PonderUI.COLOR_IDLE)
         .<RenderElement>at(21.0F, 21.0F, 100.0F)
         .<RenderElement>withBounds(30, 30)
         .render(graphics);
      graphics.drawString(
         this.font, Ponder.lang().translate("ui.pondering_tag").component(), x, y - 6, UIRenderHelper.COLOR_TEXT_DARKER.getFirst().getRGB(), false
      );
      y += 8;
      x += 0;
      poseStack.translate((float)x, (float)y, 0.0F);
      poseStack.translate(0.0F, 0.0F, 5.0F);
      graphics.drawString(this.font, title, 0, 0, UIRenderHelper.COLOR_TEXT.getFirst().getRGB(), false);
      poseStack.popPose();
      poseStack.pushPose();
      poseStack.translate(23.0F, 23.0F, 10.0F);
      poseStack.scale(1.66F, 1.66F, 1.66F);
      this.tag.render(graphics, 0, 0);
      poseStack.popPose();
      poseStack.popPose();
      poseStack.pushPose();
      int w = (int)((double)this.width * 0.45);
      x = (this.width - w) / 2;
      y = this.getItemsY() - 10 + Math.max(this.itemArea.getHeight(), 48);
      String desc = this.tag.getDescription();
      int h = this.font.wordWrapHeight(desc, w);
      new BoxElement()
         .<BoxElement>withBackground(PonderUI.BACKGROUND_FLAT)
         .<BoxElement>gradientBorder(PonderUI.COLOR_IDLE)
         .<RenderElement>at((float)(x - 3), (float)(y - 3), 90.0F)
         .<RenderElement>withBounds(w + 6, h + 6)
         .render(graphics);
      poseStack.translate(0.0F, 0.0F, 100.0F);
      ClientFontHelper.drawSplitString(graphics, poseStack, this.font, desc, x, y, w, UIRenderHelper.COLOR_TEXT.getFirst().getRGB());
      poseStack.popPose();
   }

   protected void renderItems(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      if (!this.items.isEmpty()) {
         int x = (int)((double)this.width * 0.5);
         int y = this.getItemsY();
         String relatedTitle = Ponder.lang().translate("ui.associated").string();
         int stringWidth = this.font.width(relatedTitle);
         PoseStack poseStack = graphics.pose();
         poseStack.pushPose();
         poseStack.translate((float)x, (float)y, 0.0F);
         new BoxElement()
            .<BoxElement>withBackground(PonderUI.BACKGROUND_FLAT)
            .<BoxElement>gradientBorder(PonderUI.COLOR_IDLE)
            .<RenderElement>at((float)(this.windowWidth - stringWidth) / 2.0F - 5.0F, (float)(this.itemArea.getY() - 21), 100.0F)
            .<RenderElement>withBounds(stringWidth + 10, 10)
            .render(graphics);
         poseStack.translate(0.0F, 0.0F, 200.0F);
         graphics.drawCenteredString(this.font, relatedTitle, this.windowWidth / 2, this.itemArea.getY() - 20, UIRenderHelper.COLOR_TEXT.getFirst().getRGB());
         poseStack.translate(0.0F, 0.0F, -200.0F);
         UIRenderHelper.streak(graphics, 0.0F, 0, 0, this.itemArea.getHeight() + 10, this.itemArea.getWidth() / 2 + 75);
         UIRenderHelper.streak(graphics, 180.0F, 0, 0, this.itemArea.getHeight() + 10, this.itemArea.getWidth() / 2 + 75);
         poseStack.popPose();
      }
   }

   public int getItemsY() {
      return (int)(0.15 * (double)this.height + 85.0);
   }

   protected void renderChapters(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      if (!this.chapters.isEmpty()) {
         int chapterX = (int)((double)this.width * 0.5);
         int chapterY = (int)((double)this.height * 0.75);
         graphics.pose().pushPose();
         graphics.pose().translate((float)chapterX, (float)chapterY, 0.0F);
         UIRenderHelper.streak(graphics, 0.0F, this.chapterArea.getX() - 10, this.chapterArea.getY() - 20, 20, 220);
         graphics.drawString(
            this.font,
            "More Topics to Ponder about",
            this.chapterArea.getX() - 5,
            this.chapterArea.getY() - 25,
            UIRenderHelper.COLOR_TEXT_ACCENT.getFirst().getRGB(),
            false
         );
         graphics.pose().popPose();
      }
   }

   @Override
   protected void renderWindowForeground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      RenderSystem.disableDepthTest();
      PoseStack poseStack = graphics.pose();
      poseStack.pushPose();
      poseStack.translate(0.0F, 0.0F, 200.0F);
      if (!this.hoveredItem.isEmpty()) {
         graphics.renderTooltip(this.font, this.hoveredItem, mouseX, mouseY);
      }

      poseStack.popPose();
      RenderSystem.enableDepthTest();
   }

   @Override
   protected String getBreadcrumbTitle() {
      return this.tag.getTitle();
   }

   public ItemStack getHoveredTooltipItem() {
      return this.hoveredItem;
   }

   @Override
   public boolean isEquivalentTo(NavigatableSimiScreen other) {
      return other instanceof PonderTagScreen ? this.tag == ((PonderTagScreen)other).tag : super.isEquivalentTo(other);
   }

   @Override
   public boolean isPauseScreen() {
      return true;
   }

   public PonderTag getTag() {
      return this.tag;
   }

   public void removed() {
      super.removed();
      this.hoveredItem = ItemStack.EMPTY;
   }

   public static record ItemEntry(@Nullable ItemLike item, ResourceLocation key) {
   }
}
