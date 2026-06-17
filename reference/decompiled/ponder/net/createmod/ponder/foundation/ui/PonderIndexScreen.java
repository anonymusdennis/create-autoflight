package net.createmod.ponder.foundation.ui;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.createmod.catnip.gui.NavigatableSimiScreen;
import net.createmod.catnip.gui.ScreenOpener;
import net.createmod.catnip.gui.UIRenderHelper;
import net.createmod.catnip.gui.widget.AbstractSimiWidget;
import net.createmod.catnip.gui.widget.BoxWidget;
import net.createmod.catnip.gui.widget.ElementWidget;
import net.createmod.catnip.layout.LayoutHelper;
import net.createmod.catnip.layout.PaginationState;
import net.createmod.catnip.registry.RegisteredObjectsHelper;
import net.createmod.ponder.enums.PonderGuiTextures;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.foundation.registration.PonderIndexExclusionHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public class PonderIndexScreen extends AbstractPonderScreen {
   protected final List<PonderIndexScreen.ItemEntry> items;
   protected List<PonderButton> paginatedWidgets = new ArrayList<>();
   protected PaginationState paginationState = new PaginationState();
   protected Rect2i maxScreenArea = new Rect2i(0, 0, 0, 0);
   protected Rect2i usedArea = new Rect2i(0, 0, 0, 0);
   protected int maxItemRows;
   protected int maxItemsPerRow;
   protected int maxItemsPerPage;
   @Nullable
   protected PonderButton nextPage;
   @Nullable
   protected PonderButton prevPage;
   private ItemStack hoveredItem = ItemStack.EMPTY;
   private final List<Predicate<ItemLike>> exclusions;

   public PonderIndexScreen() {
      this.items = new ArrayList<>();
      this.exclusions = PonderIndex.streamPlugins().flatMap(PonderIndexExclusionHelper::pluginToExclusions).toList();
   }

   @Override
   protected void init() {
      super.init();
      this.items.clear();
      PonderIndex.getSceneAccess()
         .getRegisteredEntries()
         .stream()
         .map(Entry::getKey)
         .distinct()
         .map(key -> new PonderIndexScreen.ItemEntry(RegisteredObjectsHelper.getItemOrBlock(key), key))
         .filter(entry -> entry.item != null)
         .filter(this::isItemIncluded)
         .forEach(this.items::add);
      this.items.sort(Comparator.comparing(PonderIndexScreen.ItemEntry::key));
      int centerX = this.width / 2;
      int centerY = this.height / 2;
      int targetWidth = Mth.clamp(this.width - 180, 250, 400);
      int targetHeight = Mth.clamp(this.height - 140, 150, 300);
      this.maxScreenArea = new Rect2i(centerX - targetWidth / 2, centerY - targetHeight / 2, targetWidth, targetHeight);
      this.maxItemRows = (this.maxScreenArea.getHeight() + 8) / 36;
      this.maxItemsPerRow = (this.maxScreenArea.getWidth() + 8) / 36;
      this.maxItemsPerPage = this.maxItemRows * this.maxItemsPerRow;
      this.paginationState = new PaginationState(this.items.size() > this.maxItemsPerPage, this.maxItemsPerPage, this.items.size());
      this.setupItemsForPage();
      if (this.paginationState.usesPagination()) {
         this.addRenderableWidget(
            this.prevPage = new PonderButton(centerX - 100, this.maxScreenArea.getY() + this.maxScreenArea.getHeight() + 10)
               .<ElementWidget>showing(PonderGuiTextures.ICON_PONDER_LEFT)
               .<AbstractSimiWidget>withCallback(() -> {
                  this.paginationState.previousPage();
                  this.updateAfterPaginationChange();
               })
               .setActive(false)
         );
         this.addRenderableWidget(
            this.nextPage = new PonderButton(centerX + 80, this.maxScreenArea.getY() + this.maxScreenArea.getHeight() + 10)
               .<ElementWidget>showing(PonderGuiTextures.ICON_PONDER_RIGHT)
               .<AbstractSimiWidget>withCallback(() -> {
                  this.paginationState.nextPage();
                  this.updateAfterPaginationChange();
               })
               .setActive(true)
         );
         this.prevPage.updateGradientFromState();
         this.nextPage.updateGradientFromState();
      }
   }

   protected void setupItemsForPage() {
      this.removeWidgets(this.paginatedWidgets);
      int itemCount = this.paginationState.getCurrentPageElementCount();
      int actualItemRows = Mth.clamp((int)Math.ceil((double)itemCount / (double)this.maxItemsPerRow), 1, this.maxItemRows);
      LayoutHelper layoutHelper = LayoutHelper.centeredHorizontal(itemCount, actualItemRows, 28, 28, 8);
      this.usedArea = layoutHelper.getArea();
      int centerX = this.width / 2;
      int centerY = this.height / 2;
      this.paginationState
         .iterateForCurrentPage(
            (iPage, iOverall) -> {
               PonderIndexScreen.ItemEntry entry = this.items.get(iOverall);
               PonderButton b = new PonderButton(centerX + layoutHelper.getX() + 4, centerY + layoutHelper.getY() + 4)
                  .<PonderButton>showing(new ItemStack(entry.item))
                  .withCallback((x, y) -> {
                     if (PonderIndex.getSceneAccess().doScenesExistForId(entry.key)) {
                        this.centerScalingOn(x, y);
                        ScreenOpener.transitionTo(PonderUI.of(new ItemStack(entry.item)));
                     }
                  });
               this.paginatedWidgets.add(b);
               this.addRenderableWidget(b);
               layoutHelper.next();
            }
         );
   }

   protected void updateAfterPaginationChange() {
      this.setupItemsForPage();
      this.prevPage.<PonderButton>setActive(this.paginationState.hasPreviousPage()).animateGradientFromState();
      this.nextPage.<PonderButton>setActive(this.paginationState.hasNextPage()).animateGradientFromState();
   }

   @Override
   protected void initBackTrackIcon(BoxWidget backTrack) {
      backTrack.showing(PonderGuiTextures.ICON_PONDER_IDENTIFY);
   }

   private boolean isItemIncluded(PonderIndexScreen.ItemEntry entry) {
      return this.exclusions.stream().noneMatch(predicate -> predicate.test(entry.item));
   }

   @Override
   public void tick() {
      super.tick();
      PonderUI.ponderTicks++;
      this.hoveredItem = ItemStack.EMPTY;
      Window w = this.minecraft.getWindow();
      double mouseX = this.minecraft.mouseHandler.xpos() * (double)w.getGuiScaledWidth() / (double)w.getScreenWidth();
      double mouseY = this.minecraft.mouseHandler.ypos() * (double)w.getGuiScaledHeight() / (double)w.getScreenHeight();

      for (GuiEventListener child : this.children()) {
         if (child instanceof PonderButton) {
            PonderButton button = (PonderButton)child;
            if (button.isMouseOver(mouseX, mouseY)) {
               this.hoveredItem = button.getItem() != null ? button.getItem() : ItemStack.EMPTY;
            }
         }
      }
   }

   @Override
   protected void renderWindow(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      super.renderWindow(graphics, mouseX, mouseY, partialTicks);
      int centerX = this.width / 2;
      int centerY = this.height / 2;
      PoseStack poseStack = graphics.pose();
      poseStack.pushPose();
      poseStack.translate((float)centerX, (float)centerY, 0.0F);
      UIRenderHelper.streak(graphics, 0.0F, this.usedArea.getX() - 10, this.usedArea.getY() - 20, 20, 220);
      graphics.drawString(
         this.font, "Items to inspect", this.usedArea.getX() - 5, this.usedArea.getY() - 25, UIRenderHelper.COLOR_TEXT.getFirst().getRGB(), false
      );
      poseStack.popPose();
      if (this.paginationState.usesPagination()) {
         poseStack.pushPose();
         poseStack.translate((float)centerX, (float)(this.maxScreenArea.getY() + this.maxScreenArea.getHeight() + 14), 0.0F);
         poseStack.scale(1.5F, 1.5F, 1.0F);
         String pageString = "Page " + (this.paginationState.getPageIndex() + 1) + "/" + this.paginationState.getMaxPages();
         int stringWidth = this.font.width(pageString);
         UIRenderHelper.streak(graphics, 0.0F, 0, 4, 14, 85);
         UIRenderHelper.streak(graphics, 180.0F, 0, 4, 14, 85);
         graphics.drawString(this.font, pageString, (int)((float)(-stringWidth) / 2.0F), 0, UIRenderHelper.COLOR_TEXT.getFirst().getRGB(), false);
         poseStack.popPose();
      }
   }

   @Override
   protected void renderWindowForeground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      if (!this.hoveredItem.isEmpty()) {
         PoseStack poseStack = graphics.pose();
         poseStack.pushPose();
         poseStack.translate(0.0F, 0.0F, 200.0F);
         graphics.renderTooltip(this.font, this.hoveredItem, mouseX, mouseY);
         poseStack.popPose();
      }
   }

   @Override
   public boolean isEquivalentTo(NavigatableSimiScreen other) {
      return other instanceof PonderIndexScreen;
   }

   public ItemStack getHoveredTooltipItem() {
      return this.hoveredItem;
   }

   @Override
   public boolean isPauseScreen() {
      return true;
   }

   public static record ItemEntry(@Nullable ItemLike item, ResourceLocation key) {
   }
}
