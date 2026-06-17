package net.createmod.ponder.foundation.ui;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.createmod.catnip.gui.ScreenOpener;
import net.createmod.catnip.gui.UIRenderHelper;
import net.createmod.catnip.gui.element.BoxElement;
import net.createmod.catnip.gui.element.RenderElement;
import net.createmod.catnip.gui.widget.AbstractSimiWidget;
import net.createmod.catnip.gui.widget.BoxWidget;
import net.createmod.catnip.gui.widget.ElementWidget;
import net.createmod.catnip.lang.ClientFontHelper;
import net.createmod.catnip.lang.FontHelper;
import net.createmod.catnip.layout.LayoutHelper;
import net.createmod.catnip.layout.PaginationState;
import net.createmod.catnip.platform.CatnipServices;
import net.createmod.ponder.Ponder;
import net.createmod.ponder.enums.PonderGuiTextures;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.foundation.PonderTag;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class PonderTagIndexScreen extends AbstractPonderScreen {
   protected List<PonderTagIndexScreen.ModTagsEntry> currentModTagEntries = new LinkedList<>();
   protected List<Entry<String, List<PonderTag>>> sortedModTags = List.of();
   protected PaginationState paginationState = new PaginationState();
   @Nullable
   protected PonderButton pageNext;
   @Nullable
   protected PonderButton pagePrev;
   @Nullable
   private PonderTag hoveredItem = null;

   @Override
   protected void init() {
      super.init();
      Map<String, List<PonderTag>> tagsByModID = PonderIndex.getTagAccess()
         .getListedTags()
         .stream()
         .collect(Collectors.groupingBy(tag -> tag.getId().getNamespace()));
      this.sortedModTags = new TreeMap<>(tagsByModID).entrySet().stream().toList();
      int modCount = this.sortedModTags.size();
      int maxModsOnScreen = (this.height - 140 - 40) / 58;
      this.paginationState = new PaginationState(modCount > 1 && modCount > maxModsOnScreen, maxModsOnScreen, modCount);
      this.setupModTagEntries();
      if (this.paginationState.usesPagination()) {
         int xOffset = (int)((double)this.width * 0.5);
         this.addRenderableWidget(
            this.pagePrev = new PonderButton(xOffset - 120, this.height - 32)
               .<ElementWidget>showing(PonderGuiTextures.ICON_PONDER_LEFT)
               .<AbstractSimiWidget>withCallback(() -> {
                  this.paginationState.previousPage();
                  this.updateAfterPaginationChange();
               })
               .setActive(false)
         );
         this.pagePrev.updateGradientFromState();
         this.addRenderableWidget(
            this.pageNext = new PonderButton(xOffset + 100, this.height - 32)
               .<ElementWidget>showing(PonderGuiTextures.ICON_PONDER_RIGHT)
               .<AbstractSimiWidget>withCallback(() -> {
                  this.paginationState.nextPage();
                  this.updateAfterPaginationChange();
               })
               .setActive(true)
         );
      }
   }

   protected void setupModTagEntries() {
      this.removeWidgets(this.children().stream().filter(widget -> widget instanceof PonderButton ponderButton ? ponderButton.tag != null : false).toList());
      this.currentModTagEntries.clear();
      AtomicInteger yOffset = new AtomicInteger(140);
      int xOffset = (int)((double)this.width * 0.5);
      this.paginationState
         .iterateForCurrentPage(
            (iPage, iOverall) -> {
               Entry<String, List<PonderTag>> entry = this.sortedModTags.get(iOverall);
               String modName = CatnipServices.PLATFORM.getModDisplayName(entry.getKey());
               List<PonderTag> tags = entry.getValue();
               LayoutHelper layout = LayoutHelper.centeredHorizontal(tags.size(), 1, 28, 28, 8);
               Rect2i layoutArea = layout.getArea();

               for (PonderTag tag : tags) {
                  PonderButton button = new PonderButton(xOffset + layout.getX() + 4, yOffset.get() + layout.getY() + 18)
                     .<PonderButton>showingTag(tag)
                     .withCallback((mouseX, mouseY) -> {
                        this.centerScalingOn(mouseX, mouseY);
                        ScreenOpener.transitionTo(new PonderTagScreen(tag));
                     });
                  this.addRenderableWidget(button);
                  layout.next();
               }

               this.currentModTagEntries.add(new PonderTagIndexScreen.ModTagsEntry(modName, tags.size(), layoutArea, yOffset.get()));
               yOffset.addAndGet(68);
            }
         );
      int i = 0;

      while (i < this.paginationState.getElementsPerPage() && this.paginationState.getStartIndex() + i < this.sortedModTags.size()) {
         i++;
      }
   }

   protected void updateAfterPaginationChange() {
      this.setupModTagEntries();
      this.pagePrev.<PonderButton>setActive(this.paginationState.hasPreviousPage()).animateGradientFromState();
      this.pageNext.<PonderButton>setActive(this.paginationState.hasNextPage()).animateGradientFromState();
   }

   @Override
   protected void initBackTrackIcon(BoxWidget backTrack) {
      backTrack.showing(PonderGuiTextures.ICON_PONDER_IDENTIFY);
   }

   @Override
   public void tick() {
      super.tick();
      PonderUI.ponderTicks++;
      this.hoveredItem = null;
      Window w = this.minecraft.getWindow();
      double mouseX = this.minecraft.mouseHandler.xpos() * (double)w.getGuiScaledWidth() / (double)w.getScreenWidth();
      double mouseY = this.minecraft.mouseHandler.ypos() * (double)w.getGuiScaledHeight() / (double)w.getScreenHeight();

      for (GuiEventListener child : this.children()) {
         if (child != this.backTrack && child instanceof PonderButton) {
            PonderButton button = (PonderButton)child;
            if (button.isMouseOver(mouseX, mouseY)) {
               this.hoveredItem = button.getTag();
            }
         }
      }
   }

   @Override
   protected void renderWindow(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      super.renderWindow(graphics, mouseX, mouseY, partialTicks);
      PoseStack poseStack = graphics.pose();
      poseStack.pushPose();
      poseStack.translate((double)this.width / 2.0, 30.0, 0.0);
      poseStack.pushPose();
      poseStack.translate(-120.0F, 0.0F, 0.0F);
      String title = Ponder.lang().translate("ui.welcome").string();
      new BoxElement()
         .<BoxElement>withBackground(PonderUI.BACKGROUND_FLAT)
         .<BoxElement>gradientBorder(PonderUI.COLOR_IDLE)
         .<RenderElement>at(0.0F, 0.0F, 0.0F)
         .<RenderElement>withBounds(30, 30)
         .render(graphics);
      PonderGuiTextures.LOGO.render(graphics, -1, -1);
      poseStack.translate(34.0F, -3.0F, 0.0F);
      int streakHeight = 36;
      UIRenderHelper.streak(graphics, 0.0F, 0, streakHeight / 2, streakHeight, 280);
      poseStack.scale(2.0F, 2.0F, 2.0F);
      graphics.drawString(this.font, title, 3, 5, UIRenderHelper.COLOR_TEXT.getFirst().getRGB(), false);
      poseStack.popPose();
      poseStack.translate(0.0F, 50.0F, 0.0F);
      poseStack.pushPose();
      int maxWidth = (int)((float)this.width * 0.5F);
      String desc = Ponder.lang().translate("ui.index_description").string();
      int descWidth = this.font.width(desc);
      if (descWidth + 2 < maxWidth) {
         maxWidth = descWidth + 2;
      }

      int descHeight = this.font.wordWrapHeight(desc, maxWidth);
      poseStack.translate((float)(-maxWidth) / 2.0F, 0.0F, 0.0F);
      new BoxElement()
         .<BoxElement>withBackground(PonderUI.BACKGROUND_FLAT)
         .<BoxElement>gradientBorder(PonderUI.COLOR_IDLE)
         .<RenderElement>at(-3.0F, -3.0F, 0.0F)
         .<RenderElement>withBounds(maxWidth + 6, descHeight + 5)
         .render(graphics);
      ClientFontHelper.drawSplitString(graphics, poseStack, this.font, desc, 0, 0, maxWidth, UIRenderHelper.COLOR_TEXT.getFirst().getRGB());
      poseStack.popPose();
      poseStack.translate(0.0F, -80.0F, 0.0F);

      for (PonderTagIndexScreen.ModTagsEntry entry : this.currentModTagEntries) {
         poseStack.pushPose();
         this.renderTagsEntry(graphics, entry);
         poseStack.popPose();
      }

      poseStack.popPose();
   }

   protected void renderTagsEntry(GuiGraphics graphics, PonderTagIndexScreen.ModTagsEntry entry) {
      PoseStack poseStack = graphics.pose();
      int layoutWidth = entry.layoutArea().getWidth();
      int layoutHeight = entry.layoutArea().getHeight();
      poseStack.translate(0.0F, (float)entry.yPos(), 0.0F);
      String categories = Ponder.lang().translate("ui.categories", entry.modName()).string();
      int stringWidth = this.font.width(categories);
      poseStack.pushPose();
      poseStack.translate((float)(-stringWidth) / 2.0F, -20.0F, 0.0F);
      new BoxElement()
         .<BoxElement>withBackground(PonderUI.BACKGROUND_FLAT)
         .<BoxElement>gradientBorder(PonderUI.COLOR_IDLE)
         .<RenderElement>at(-3.0F, -1.0F, 0.0F)
         .<RenderElement>withBounds(stringWidth + 6, 10)
         .render(graphics);
      graphics.drawString(this.font, categories, 0, 0, UIRenderHelper.COLOR_TEXT.getFirst().getRGB(), false);
      poseStack.popPose();
      int extraLength = Mth.clamp(entry.tagCount, 2, 8);
      UIRenderHelper.streak(graphics, 0.0F, 0, layoutHeight / 2, layoutHeight + 6, layoutWidth / 2 + extraLength * 15);
      UIRenderHelper.streak(graphics, 180.0F, 0, layoutHeight / 2, layoutHeight + 6, layoutWidth / 2 + extraLength * 15);
   }

   @Override
   protected void renderWindowForeground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      RenderSystem.disableDepthTest();
      PoseStack poseStack = graphics.pose();
      poseStack.pushPose();
      poseStack.translate(0.0F, 0.0F, 200.0F);
      if (this.hoveredItem != null) {
         List<Component> list = FontHelper.cutStringTextComponent(this.hoveredItem.getDescription(), FontHelper.Palette.ALL_GRAY);
         list.add(0, Component.literal(this.hoveredItem.getTitle()));
         graphics.renderComponentTooltip(this.font, list, mouseX, mouseY);
      }

      poseStack.popPose();
      RenderSystem.enableDepthTest();
   }

   @Override
   public boolean isPauseScreen() {
      return true;
   }

   public void removed() {
      super.removed();
      this.hoveredItem = null;
   }

   public static record ModTagsEntry(String modName, int tagCount, Rect2i layoutArea, int yPos) {
   }
}
