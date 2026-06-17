package com.simibubi.create.content.logistics.stockTicker;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.AllTags;
import com.simibubi.create.compat.Mods;
import com.simibubi.create.compat.jei.CreateJEI;
import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;
import com.simibubi.create.content.equipment.clipboard.ClipboardEntry;
import com.simibubi.create.content.logistics.AddressEditBox;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelScreen;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerRenderer;
import com.simibubi.create.content.trains.station.NoShadowFontWrapper;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.infrastructure.config.AllConfigs;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import mezz.jei.api.runtime.IIngredientFilter;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.config.ConfigBase.ConfigEnum;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.gui.UIRenderHelper;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.createmod.catnip.lang.Lang;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.platform.CatnipServices;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.createmod.catnip.theme.Color;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public class StockKeeperRequestScreen extends AbstractSimiContainerScreen<StockKeeperRequestMenu> {
   private static final AllGuiTextures NUMBERS = AllGuiTextures.NUMBERS;
   private static final AllGuiTextures HEADER = AllGuiTextures.STOCK_KEEPER_REQUEST_HEADER;
   private static final AllGuiTextures BODY = AllGuiTextures.STOCK_KEEPER_REQUEST_BODY;
   private static final AllGuiTextures FOOTER = AllGuiTextures.STOCK_KEEPER_REQUEST_FOOTER;
   StockTickerBlockEntity blockEntity;
   public LerpedFloat itemScroll = LerpedFloat.linear().startWithValue(0.0);
   final int rows = 9;
   final int cols = 9;
   final int rowHeight = 20;
   final int colWidth = 20;
   final Couple<Integer> noneHovered = Couple.create(-1, -1);
   int itemsX;
   int itemsY;
   int orderY;
   int lockX;
   int besideSearchButtonY;
   int windowWidth;
   int windowHeight;
   int jeiSyncX;
   String previousJEISearchText = "";
   public EditBox searchBox;
   public AddressEditBox addressBox;
   int emptyTicks = 0;
   int successTicks = 0;
   public List<List<BigItemStack>> currentItemSource;
   public List<List<BigItemStack>> displayedItems = new ArrayList<>();
   public List<StockKeeperRequestScreen.CategoryEntry> categories = new ArrayList<>();
   public List<BigItemStack> itemsToOrder = new ArrayList<>();
   public List<CraftableBigItemStack> recipesToOrder = new ArrayList<>();
   WeakReference<LivingEntity> stockKeeper = new WeakReference<>(null);
   WeakReference<BlazeBurnerBlockEntity> blaze = new WeakReference<>(null);
   boolean encodeRequester;
   ItemStack itemToProgram;
   List<List<ClipboardEntry>> clipboardItem;
   private final boolean isAdmin;
   private boolean isLocked;
   private boolean scrollHandleActive;
   private boolean ignoreTextInput;
   public boolean refreshSearchNextTick;
   public boolean moveToTopNextTick;
   private List<Rect2i> extraAreas;
   private final Set<Integer> hiddenCategories;
   private InventorySummary forcedEntries;
   private boolean canRequestCraftingPackage;

   public StockKeeperRequestScreen(StockKeeperRequestMenu container, Inventory inv, Component title) {
      super(container, inv, title);
      this.isAdmin = ((StockKeeperRequestMenu)this.menu).isAdmin;
      this.isLocked = ((StockKeeperRequestMenu)this.menu).isLocked;
      this.refreshSearchNextTick = false;
      this.moveToTopNextTick = false;
      this.extraAreas = Collections.emptyList();
      this.forcedEntries = new InventorySummary();
      this.canRequestCraftingPackage = false;
      this.blockEntity = container.contentHolder;
      this.blockEntity.lastClientsideStockSnapshot = null;
      this.blockEntity.ticksSinceLastUpdate = 15;
      ((StockKeeperRequestMenu)this.menu).screenReference = this;
      this.hiddenCategories = new HashSet<>(
         this.blockEntity.hiddenCategoriesByPlayer.getOrDefault(((StockKeeperRequestMenu)this.menu).player.getUUID(), List.of())
      );
      this.itemToProgram = ((StockKeeperRequestMenu)this.menu).player.getMainHandItem();
      this.encodeRequester = AllTags.AllItemTags.TABLE_CLOTHS.matches(this.itemToProgram) || AllBlocks.REDSTONE_REQUESTER.isIn(this.itemToProgram);
      if (AllBlocks.CLIPBOARD.isIn(this.itemToProgram)) {
         this.clipboardItem = ClipboardEntry.readAll(this.itemToProgram);
         boolean anyItems = false;

         for (List<ClipboardEntry> list : this.clipboardItem) {
            for (ClipboardEntry entry : list) {
               if (!entry.icon.isEmpty()) {
                  anyItems = true;
                  break;
               }
            }
         }

         if (!anyItems) {
            this.clipboardItem = null;
         }
      }

      for (int yOffset : Iterate.zeroAndOne) {
         for (Direction side : Iterate.horizontalDirections) {
            BlockPos seatPos = this.blockEntity.getBlockPos().below(yOffset).relative(side);

            for (SeatEntity seatEntity : this.blockEntity.getLevel().getEntitiesOfClass(SeatEntity.class, new AABB(seatPos))) {
               if (!seatEntity.getPassengers().isEmpty() && seatEntity.getPassengers().get(0) instanceof LivingEntity keeper) {
                  this.stockKeeper = new WeakReference<>(keeper);
               }
            }

            if (yOffset == 0 && this.blockEntity.getLevel().getBlockEntity(seatPos) instanceof BlazeBurnerBlockEntity bbbe) {
               this.blaze = new WeakReference<>(bbbe);
               return;
            }
         }
      }
   }

   @Override
   protected void init() {
      int appropriateHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight() - 10;
      appropriateHeight -= Mth.positiveModulo(appropriateHeight - HEADER.getHeight() - FOOTER.getHeight(), BODY.getHeight());
      appropriateHeight = Math.min(appropriateHeight, HEADER.getHeight() + FOOTER.getHeight() + BODY.getHeight() * 17);
      this.setWindowSize(this.windowWidth = 226, this.windowHeight = appropriateHeight);
      super.init();
      this.clearWidgets();
      int x = this.getGuiLeft();
      int y = this.getGuiTop();
      this.itemsX = x + (this.windowWidth - 180) / 2 + 1;
      this.itemsY = y + 33;
      this.orderY = y + this.windowHeight - 72;
      this.jeiSyncX = x + 25;
      this.lockX = x + 186;
      this.besideSearchButtonY = y + 18;
      MutableComponent searchLabel = CreateLang.translateDirect("gui.stock_keeper.search_items");
      this.searchBox = new EditBox(new NoShadowFontWrapper(this.font), x + 71, y + 22, 100, 9, searchLabel);
      this.searchBox.setMaxLength(50);
      this.searchBox.setBordered(false);
      this.searchBox.setTextColor(4861233);
      this.addWidget(this.searchBox);
      this.refreshSearchNextTick = true;
      this.moveToTopNextTick = true;
      this.syncJEI(true);
      boolean initial = this.addressBox == null;
      String previouslyUsedAddress = initial ? this.blockEntity.previouslyUsedAddress : this.addressBox.getValue();
      this.addressBox = new AddressEditBox(this, new NoShadowFontWrapper(this.font), x + 27, y + this.windowHeight - 36, 92, 10, true);
      this.addressBox.setTextColor(7424576);
      this.addressBox.setValue(previouslyUsedAddress);
      this.addRenderableWidget(this.addressBox);
      this.extraAreas = new ArrayList<>();
      int leftHeight = 40;
      int rightHeight = 50;
      LivingEntity keeper = this.stockKeeper.get();
      if (keeper != null && keeper.isAlive()) {
         leftHeight = (int)(Math.max(0.0, keeper.getBoundingBox().getYsize()) * 50.0);
      }

      this.extraAreas.add(new Rect2i(0, y + this.windowHeight - 15 - leftHeight, x, this.height));
      if (this.encodeRequester) {
         this.extraAreas.add(new Rect2i(x + this.windowWidth, y + this.windowHeight - 15 - rightHeight, rightHeight + 10, rightHeight));
      }

      if (initial) {
         this.playUiSound(SoundEvents.WOOD_HIT, 0.5F, 1.5F);
         this.playUiSound(SoundEvents.BOOK_PAGE_TURN, 1.0F, 1.0F);
         this.syncJEI(false);
      }
   }

   private void refreshSearchResults(boolean scrollBackUp) {
      this.displayedItems = Collections.emptyList();
      if (scrollBackUp) {
         this.itemScroll.startWithValue(0.0);
      }

      if (this.currentItemSource == null) {
         this.clampScrollBar();
      } else if (this.isSchematicListMode()) {
         this.clampScrollBar();
         this.requestSchematicList();
      } else {
         this.categories = new ArrayList<>();

         for (int i = 0; i < this.blockEntity.categories.size(); i++) {
            ItemStack stack = this.blockEntity.categories.get(i);
            StockKeeperRequestScreen.CategoryEntry entry = new StockKeeperRequestScreen.CategoryEntry(
               i, stack.isEmpty() ? "" : stack.getHoverName().getString(), 0
            );
            entry.hidden = this.hiddenCategories.contains(i);
            this.categories.add(entry);
         }

         StockKeeperRequestScreen.CategoryEntry unsorted = new StockKeeperRequestScreen.CategoryEntry(
            -1, CreateLang.translate("gui.stock_keeper.unsorted_category").string(), 0
         );
         unsorted.hidden = this.hiddenCategories.contains(-1);
         this.categories.add(unsorted);
         String valueWithPrefix = this.searchBox.getValue();
         boolean anyItemsInCategory = false;
         if (valueWithPrefix.isBlank()) {
            this.displayedItems = new ArrayList<>(this.currentItemSource);
            int categoryY = 0;

            for (int categoryIndex = 0; categoryIndex < this.currentItemSource.size(); categoryIndex++) {
               this.categories.get(categoryIndex).y = categoryY;
               List<BigItemStack> displayedItemsInCategory = this.displayedItems.get(categoryIndex);
               if (!displayedItemsInCategory.isEmpty()) {
                  if (categoryIndex < this.currentItemSource.size() - 1) {
                     anyItemsInCategory = true;
                  }

                  categoryY += 20;
                  if (!this.categories.get(categoryIndex).hidden) {
                     categoryY = (int)((double)categoryY + Math.ceil((double)((float)displayedItemsInCategory.size() / 9.0F)) * 20.0);
                  }
               }
            }

            if (!anyItemsInCategory) {
               this.categories.clear();
            }

            this.clampScrollBar();
            this.updateCraftableAmounts();
         } else {
            boolean modSearch = false;
            boolean tagSearch = false;
            if ((modSearch = valueWithPrefix.startsWith("@")) || (tagSearch = valueWithPrefix.startsWith("#"))) {
               valueWithPrefix = valueWithPrefix.substring(1);
            }

            String value = valueWithPrefix.toLowerCase(Locale.ROOT);
            this.displayedItems = new ArrayList<>();
            this.currentItemSource.forEach($ -> this.displayedItems.add(new ArrayList<>()));
            int categoryY = 0;

            for (int categoryIndexx = 0; categoryIndexx < this.displayedItems.size(); categoryIndexx++) {
               List<BigItemStack> category = this.currentItemSource.get(categoryIndexx);
               this.categories.get(categoryIndexx).y = categoryY;
               if (this.displayedItems.size() <= categoryIndexx) {
                  break;
               }

               List<BigItemStack> displayedItemsInCategory = this.displayedItems.get(categoryIndexx);

               for (BigItemStack entry : category) {
                  ItemStack stack = entry.stack;
                  if (modSearch) {
                     if (BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().contains(value)) {
                        displayedItemsInCategory.add(entry);
                     }
                  } else if (tagSearch) {
                     if (stack.getTags().anyMatch(key -> key.location().toString().contains(value))) {
                        displayedItemsInCategory.add(entry);
                     }
                  } else if (stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(value)
                     || BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().contains(value)) {
                     displayedItemsInCategory.add(entry);
                  }
               }

               if (!displayedItemsInCategory.isEmpty()) {
                  if (categoryIndexx < this.currentItemSource.size() - 1) {
                     anyItemsInCategory = true;
                  }

                  categoryY += 20;
                  if (!this.categories.get(categoryIndexx).hidden) {
                     categoryY = (int)((double)categoryY + Math.ceil((double)((float)displayedItemsInCategory.size() / 9.0F)) * 20.0);
                  }
               }
            }

            if (!anyItemsInCategory) {
               this.categories.clear();
            }

            this.clampScrollBar();
            this.updateCraftableAmounts();
         }
      }
   }

   @Override
   protected void containerTick() {
      super.containerTick();
      this.addressBox.tick();
      if (!this.forcedEntries.isEmpty()) {
         InventorySummary summary = this.blockEntity.getLastClientsideStockSnapshotAsSummary();

         for (BigItemStack stack : this.forcedEntries.getStacks()) {
            int limitedAmount = -stack.count - 1;
            int actualAmount = summary.getCountOf(stack.stack);
            if (actualAmount <= limitedAmount) {
               this.forcedEntries.erase(stack.stack);
            }
         }
      }

      boolean allEmpty = true;

      for (List<BigItemStack> list : this.displayedItems) {
         allEmpty &= list.isEmpty();
      }

      if (allEmpty) {
         this.emptyTicks++;
      } else {
         this.emptyTicks = 0;
      }

      if (this.successTicks > 0 && this.itemsToOrder.isEmpty()) {
         this.successTicks++;
      } else {
         this.successTicks = 0;
      }

      List<List<BigItemStack>> clientStockSnapshot = this.blockEntity.getClientStockSnapshot();
      if (clientStockSnapshot != this.currentItemSource) {
         this.currentItemSource = clientStockSnapshot;
         this.refreshSearchResults(false);
         this.revalidateOrders();
      }

      if (this.shouldSyncFromJEI()) {
         this.refreshSearchNextTick = true;
         this.moveToTopNextTick = true;
         this.syncJEI(true);
      }

      if (this.refreshSearchNextTick) {
         this.refreshSearchNextTick = false;
         this.refreshSearchResults(this.moveToTopNextTick);
      }

      this.itemScroll.tickChaser();
      if (Math.abs(this.itemScroll.getValue() - this.itemScroll.getChaseTarget()) < 0.0625F) {
         this.itemScroll.setValue((double)this.itemScroll.getChaseTarget());
      }

      if (this.blockEntity.ticksSinceLastUpdate > 15) {
         this.blockEntity.refreshClientStockSnapshot();
      }

      LivingEntity keeper = this.stockKeeper.get();
      BlazeBurnerBlockEntity blazeKeeper = this.blaze.get();
      if ((keeper == null || !keeper.isAlive()) && (blazeKeeper == null || blazeKeeper.isRemoved())) {
         ((StockKeeperRequestMenu)this.menu).player.closeContainer();
      }
   }

   public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      PoseStack ms = guiGraphics.pose();
      ms.pushPose();
      ms.translate(0.0F, 0.0F, -300.0F);
      super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
      ms.popPose();
   }

   protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
      if (this == this.minecraft.screen) {
         PoseStack ms = graphics.pose();
         float currentScroll = this.itemScroll.getValue(partialTicks);
         Couple<Integer> hoveredSlot = this.getHoveredSlot(mouseX, mouseY);
         int x = this.getGuiLeft();
         int y = this.getGuiTop();
         HEADER.render(graphics, x - 15, y);
         y += HEADER.getHeight();

         for (int i = 0; i < (this.windowHeight - HEADER.getHeight() - FOOTER.getHeight()) / BODY.getHeight(); i++) {
            BODY.render(graphics, x - 15, y);
            y += BODY.getHeight();
         }

         FOOTER.render(graphics, x - 15, y);
         y = this.getGuiTop();
         if (this.addressBox.getValue().isBlank() && !this.addressBox.isFocused()) {
            graphics.drawString(
               Minecraft.getInstance().font,
               CreateLang.translate("gui.stock_keeper.package_address").style(ChatFormatting.ITALIC).component(),
               this.addressBox.getX(),
               this.addressBox.getY(),
               -3294040,
               false
            );
         }

         int entitySizeOffset = 0;
         LivingEntity keeper = this.stockKeeper.get();
         if (keeper != null && keeper.isAlive()) {
            ms.pushPose();
            ms.translate(0.0F, 0.0F, 50.0F);
            entitySizeOffset = (int)(Math.max(0.0, keeper.getBoundingBox().getXsize() - 1.0) * 50.0);
            int entitySizeOffsetY = (int)(Math.max(0.0, keeper.getBoundingBox().getYsize() - 1.0) * 25.0);
            int entityX = x - 35 - entitySizeOffset;
            int entityY = y + this.windowHeight - 47 - entitySizeOffsetY;
            InventoryScreen.renderEntityInInventoryFollowsMouse(
               graphics,
               entityX - 100,
               entityY - 100,
               entityX + 100,
               entityY + 100,
               50,
               0.0F,
               (float)mouseX,
               (float)Mth.clamp(mouseY, entityY - 50, entityY + 10),
               keeper
            );
            ms.popPose();
         }

         BlazeBurnerBlockEntity keeperBE = this.blaze.get();
         if (keeperBE != null && !keeperBE.isRemoved()) {
            ms.pushPose();
            int entityX = x - 35;
            int entityY = y + this.windowHeight - 43;
            ms.translate((float)entityX, (float)entityY, 0.0F);
            ms.mulPose(Axis.XP.rotationDegrees(-22.5F));
            ms.mulPose(Axis.YP.rotationDegrees(-45.0F));
            ms.scale(48.0F, -48.0F, 48.0F);
            float animation = keeperBE.headAnimation.getValue(AnimationTickHolder.getPartialTicks()) * 0.175F;
            float horizontalAngle = AngleHelper.rad(270.0);
            BlazeBurnerBlock.HeatLevel heatLevel = keeperBE.getHeatLevelForRender();
            boolean canDrawFlame = heatLevel.isAtLeast(BlazeBurnerBlock.HeatLevel.FADING);
            boolean drawGoggles = keeperBE.goggles;
            PartialModel drawHat = AllPartialModels.LOGISTICS_HAT;
            int hashCode = keeperBE.hashCode();
            Lighting.setupForEntityInInventory();
            VertexConsumer cutout = graphics.bufferSource().getBuffer(RenderType.cutoutMipped());
            ((SuperByteBuffer)CachedBuffers.partial(AllPartialModels.BLAZE_CAGE, keeperBE.getBlockState())
                  .rotateCentered(horizontalAngle + (float) Math.PI, Direction.UP))
               .light(15728880)
               .renderInto(ms, cutout);
            BlazeBurnerRenderer.renderShared(
               ms,
               null,
               graphics.bufferSource(),
               this.minecraft.level,
               keeperBE.getBlockState(),
               heatLevel,
               animation,
               horizontalAngle,
               canDrawFlame,
               drawGoggles,
               drawHat,
               hashCode
            );
            Lighting.setupFor3DItems();
            ms.popPose();
         }

         if (this.encodeRequester) {
            ms.pushPose();
            ms.translate((float)(x + this.windowWidth + 5), (float)(y + this.windowHeight - 70), 0.0F);
            ms.scale(3.5F, 3.5F, 3.5F);
            GuiGameElement.of(this.itemToProgram).render(graphics);
            ms.popPose();
         }

         for (int index = 0; index < 9 && this.itemsToOrder.size() > index; index++) {
            BigItemStack entry = this.itemsToOrder.get(index);
            boolean isStackHovered = index == (Integer)hoveredSlot.getSecond() && (Integer)hoveredSlot.getFirst() == -1;
            ms.pushPose();
            ms.translate((float)(this.itemsX + index * 20), (float)this.orderY, 0.0F);
            this.renderItemEntry(graphics, 1.0F, entry, isStackHovered, true);
            ms.popPose();
         }

         if (this.itemsToOrder.size() > 9) {
            graphics.drawString(
               this.font, Component.literal("[+" + (this.itemsToOrder.size() - 9) + "]"), x + this.windowWidth - 40, this.orderY + 21, 16316652
            );
         }

         boolean justSent = this.itemsToOrder.isEmpty() && this.successTicks > 0;
         if (this.isConfirmHovered(mouseX, mouseY) && !justSent) {
            AllGuiTextures.STOCK_KEEPER_REQUEST_SEND_HOVER.render(graphics, x + this.windowWidth - 81, y + this.windowHeight - 41);
         }

         MutableComponent headerTitle = CreateLang.translate("gui.stock_keeper.title").component();
         graphics.drawString(this.font, headerTitle, x + this.windowWidth / 2 - this.font.width(headerTitle) / 2, y + 4, 7424576, false);
         MutableComponent component = CreateLang.translate(this.encodeRequester ? "gui.stock_keeper.configure" : "gui.stock_keeper.send").component();
         if (justSent) {
            float alpha = Mth.clamp(((float)this.successTicks + partialTicks - 5.0F) / 5.0F, 0.0F, 1.0F);
            ms.pushPose();
            ms.translate(alpha * alpha * 50.0F, 0.0F, 0.0F);
            if (this.successTicks < 10) {
               graphics.drawString(
                  this.font,
                  component,
                  x + this.windowWidth - 42 - this.font.width(component) / 2,
                  y + this.windowHeight - 35,
                  new Color(2434341).setAlpha(1.0F - alpha * alpha).getRGB(),
                  false
               );
            }

            ms.popPose();
         } else {
            graphics.drawString(this.font, component, x + this.windowWidth - 42 - this.font.width(component) / 2, y + this.windowHeight - 35, 2434341, false);
         }

         if (justSent) {
            Component msg = CreateLang.translateDirect("gui.stock_keeper.request_sent");
            float alpha = Mth.clamp(((float)this.successTicks + partialTicks - 10.0F) / 5.0F, 0.0F, 1.0F);
            int msgX = x + this.windowWidth / 2 - (this.font.width(msg) + 10) / 2;
            int msgY = this.orderY + 5;
            if (alpha > 0.0F) {
               int c3 = new Color(9198923).setAlpha(alpha).getRGB();
               int w = this.font.width(msg) + 14;
               AllGuiTextures.STOCK_KEEPER_REQUEST_BANNER_L.render(graphics, msgX - 8, msgY - 4);
               UIRenderHelper.drawStretched(graphics, msgX, msgY - 4, w, 16, 0, AllGuiTextures.STOCK_KEEPER_REQUEST_BANNER_M);
               AllGuiTextures.STOCK_KEEPER_REQUEST_BANNER_R.render(graphics, msgX + this.font.width(msg) + 10, msgY - 4);
               graphics.drawString(this.font, msg, msgX + 5, msgY, c3, false);
            }
         }

         int itemWindowX = x + 21;
         int itemWindowX2 = itemWindowX + 184;
         int itemWindowY = y + 17;
         int itemWindowY2 = y + this.windowHeight - 80;
         graphics.enableScissor(itemWindowX - 5, itemWindowY, itemWindowX2 + 10, itemWindowY2);
         ms.pushPose();
         ms.translate(0.0F, -currentScroll * 20.0F, 0.0F);

         for (int sliceY = -2; sliceY < this.getMaxScroll() * 20 + this.windowHeight - 72; sliceY += AllGuiTextures.STOCK_KEEPER_REQUEST_BG.getHeight()) {
            if (!((float)sliceY - currentScroll * 20.0F < -20.0F) && !((float)sliceY - currentScroll * 20.0F > (float)(this.windowHeight - 72))) {
               AllGuiTextures.STOCK_KEEPER_REQUEST_BG.render(graphics, x + 22, y + sliceY + 18);
            }
         }

         AllGuiTextures.STOCK_KEEPER_REQUEST_SEARCH.render(graphics, x + 42, this.searchBox.getY() - 5);
         this.searchBox.render(graphics, mouseX, mouseY, partialTicks);
         if (this.searchBox.getValue().isBlank() && !this.searchBox.isFocused()) {
            graphics.drawString(
               this.font,
               this.searchBox.getMessage(),
               x + this.windowWidth / 2 - this.font.width(this.searchBox.getMessage()) / 2,
               this.searchBox.getY(),
               -11915983,
               false
            );
         }

         boolean allEmpty = true;

         for (List<BigItemStack> list : this.displayedItems) {
            allEmpty &= list.isEmpty();
         }

         if (allEmpty) {
            Component msg = this.getTroubleshootingMessage();
            float alpha = Mth.clamp(((float)this.emptyTicks - 10.0F) / 5.0F, 0.0F, 1.0F);
            if (alpha > 0.0F) {
               List<FormattedCharSequence> split = this.font.split(msg, 160);

               for (int i = 0; i < split.size(); i++) {
                  FormattedCharSequence sequence = split.get(i);
                  int lineWidth = this.font.width(sequence);
                  graphics.drawString(
                     this.font,
                     sequence,
                     x + this.windowWidth / 2 - lineWidth / 2 + 1,
                     this.itemsY + 20 + 1 + i * (9 + 1),
                     new Color(4861233).setAlpha(alpha).getRGB(),
                     false
                  );
                  graphics.drawString(
                     this.font,
                     sequence,
                     x + this.windowWidth / 2 - lineWidth / 2,
                     this.itemsY + 20 + i * (9 + 1),
                     new Color(16316652).setAlpha(alpha).getRGB(),
                     false
                  );
               }
            }
         }

         for (int categoryIndex = 0; categoryIndex < this.displayedItems.size(); categoryIndex++) {
            List<BigItemStack> category = this.displayedItems.get(categoryIndex);
            StockKeeperRequestScreen.CategoryEntry categoryEntry = this.categories.isEmpty() ? null : this.categories.get(categoryIndex);
            int categoryY = this.categories.isEmpty() ? 0 : categoryEntry.y;
            if (!category.isEmpty()) {
               if (!this.categories.isEmpty()) {
                  (categoryEntry.hidden ? AllGuiTextures.STOCK_KEEPER_CATEGORY_HIDDEN : AllGuiTextures.STOCK_KEEPER_CATEGORY_SHOWN)
                     .render(graphics, this.itemsX, this.itemsY + categoryY + 6);
                  graphics.drawString(this.font, categoryEntry.name, this.itemsX + 10, this.itemsY + categoryY + 8, 4861233, false);
                  graphics.drawString(this.font, categoryEntry.name, this.itemsX + 9, this.itemsY + categoryY + 7, 16316652, false);
                  if (categoryEntry.hidden) {
                     continue;
                  }
               }

               for (int index = 0; index < category.size(); index++) {
                  int pY = this.itemsY + categoryY + (this.categories.isEmpty() ? 4 : 20) + index / 9 * 20;
                  float cullY = (float)pY - currentScroll * 20.0F;
                  if (!(cullY < (float)y)) {
                     if (cullY > (float)(y + this.windowHeight - 72)) {
                        break;
                     }

                     boolean isStackHovered = index == (Integer)hoveredSlot.getSecond() && categoryIndex == (Integer)hoveredSlot.getFirst();
                     BigItemStack entry = category.get(index);
                     ms.pushPose();
                     ms.translate((float)(this.itemsX + index % 9 * 20), (float)pY, 0.0F);
                     this.renderItemEntry(graphics, 1.0F, entry, isStackHovered, false);
                     ms.popPose();
                  }
               }
            }
         }

         if (Mods.JEI.isLoaded()) {
            ((StockKeeperRequestScreen.SearchSyncMode)AllConfigs.client().syncRecipeViewerSearch.get())
               .buttonTexture
               .render(graphics, this.jeiSyncX, this.besideSearchButtonY);
         }

         if (this.isAdmin) {
            (this.isLocked ? AllGuiTextures.STOCK_KEEPER_REQUEST_LOCKED : AllGuiTextures.STOCK_KEEPER_REQUEST_UNLOCKED)
               .render(graphics, this.lockX, this.besideSearchButtonY);
         }

         ms.popPose();
         graphics.disableScissor();
         int windowH = this.windowHeight - 92;
         int totalH = this.getMaxScroll() * 20 + windowH;
         int barSize = Math.max(5, Mth.floor((float)windowH / (float)totalH * (float)(windowH - 2)));
         if (barSize < windowH - 2) {
            int barX = this.itemsX + 180;
            int barY = y + 15;
            ms.pushPose();
            ms.translate(0.0F, currentScroll * 20.0F / (float)totalH * (float)(windowH - 2), 0.0F);
            AllGuiTextures pad = AllGuiTextures.STOCK_KEEPER_REQUEST_SCROLL_PAD;
            graphics.blit(
               pad.location, barX, barY, pad.getWidth(), barSize, (float)pad.getStartX(), (float)pad.getStartY(), pad.getWidth(), pad.getHeight(), 256, 256
            );
            AllGuiTextures.STOCK_KEEPER_REQUEST_SCROLL_TOP.render(graphics, barX, barY);
            if (barSize > 16) {
               AllGuiTextures.STOCK_KEEPER_REQUEST_SCROLL_MID.render(graphics, barX, barY + barSize / 2 - 4);
            }

            AllGuiTextures.STOCK_KEEPER_REQUEST_SCROLL_BOT.render(graphics, barX, barY + barSize - 5);
            ms.popPose();
         }

         if (this.recipesToOrder.size() > 0) {
            int jeiX = x + (this.windowWidth - 20 * this.recipesToOrder.size()) / 2 + 1;
            int jeiY = this.orderY - 31;
            ms.pushPose();
            ms.translate((float)jeiX, (float)jeiY, 200.0F);
            int xoffset = -3;
            AllGuiTextures.STOCK_KEEPER_REQUEST_BLUEPRINT_LEFT.render(graphics, xoffset, -3);
            xoffset += 10;

            for (int i = 0; i <= (this.recipesToOrder.size() - 1) * 5; i++) {
               AllGuiTextures.STOCK_KEEPER_REQUEST_BLUEPRINT_MIDDLE.render(graphics, xoffset, -3);
               xoffset += 4;
            }

            AllGuiTextures.STOCK_KEEPER_REQUEST_BLUEPRINT_RIGHT.render(graphics, xoffset, -3);

            for (int indexx = 0; indexx < this.recipesToOrder.size(); indexx++) {
               CraftableBigItemStack craftableBigItemStack = this.recipesToOrder.get(indexx);
               boolean isStackHovered = indexx == (Integer)hoveredSlot.getSecond() && -2 == (Integer)hoveredSlot.getFirst();
               ms.pushPose();
               ms.translate((float)(indexx * 20), 0.0F, 0.0F);
               this.renderItemEntry(graphics, 1.0F, craftableBigItemStack, isStackHovered, true);
               ms.popPose();
            }

            ms.popPose();
         }
      }
   }

   @Override
   protected void renderForeground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      super.renderForeground(graphics, mouseX, mouseY, partialTicks);
      float currentScroll = this.itemScroll.getValue(partialTicks);
      Couple<Integer> hoveredSlot = this.getHoveredSlot(mouseX, mouseY);
      if (hoveredSlot != this.noneHovered) {
         int slot = (Integer)hoveredSlot.getSecond();
         boolean recipeHovered = (Integer)hoveredSlot.getFirst() == -2;
         boolean orderHovered = (Integer)hoveredSlot.getFirst() == -1;
         BigItemStack entry = recipeHovered
            ? this.recipesToOrder.get(slot)
            : (orderHovered ? this.itemsToOrder.get(slot) : this.displayedItems.get((Integer)hoveredSlot.getFirst()).get(slot));
         if (recipeHovered) {
            ArrayList<Component> lines = new ArrayList<>(
               entry.stack.getTooltipLines(TooltipContext.of(this.minecraft.level), this.minecraft.player, TooltipFlag.NORMAL)
            );
            if (lines.size() > 0) {
               lines.set(0, CreateLang.translateDirect("gui.stock_keeper.craft", lines.get(0).copy()));
            }

            graphics.renderComponentTooltip(this.font, lines, mouseX, mouseY);
         } else {
            graphics.renderTooltip(this.font, entry.stack, mouseX, mouseY);
         }
      }

      if (currentScroll < 1.0F && mouseY > this.besideSearchButtonY && mouseY <= this.besideSearchButtonY + 15) {
         if (Mods.JEI.isLoaded() && mouseX > this.jeiSyncX && mouseX <= this.jeiSyncX + 15) {
            StockKeeperRequestScreen.SearchSyncMode mode = (StockKeeperRequestScreen.SearchSyncMode)AllConfigs.client().syncRecipeViewerSearch.get();
            String langKey = "gui.stock_keeper.jei_sync." + mode.getSerializedName();
            graphics.renderComponentTooltip(
               this.font,
               List.of(
                  CreateLang.translate(langKey).component(),
                  CreateLang.translate(langKey + ".description").style(ChatFormatting.GRAY).component(),
                  CreateLang.translate("gui.stock_keeper.click_to_cycle").style(ChatFormatting.DARK_GRAY).style(ChatFormatting.ITALIC).component()
               ),
               mouseX,
               mouseY
            );
         }

         if (this.isAdmin && mouseX > this.lockX && mouseX <= this.lockX + 15) {
            graphics.renderComponentTooltip(
               this.font,
               List.of(
                  CreateLang.translate(this.isLocked ? "gui.stock_keeper.network_locked" : "gui.stock_keeper.network_open").component(),
                  CreateLang.translate("gui.stock_keeper.network_lock_tip").style(ChatFormatting.GRAY).component(),
                  CreateLang.translate("gui.stock_keeper.network_lock_tip_1").style(ChatFormatting.GRAY).component(),
                  CreateLang.translate("gui.stock_keeper.network_lock_tip_2").style(ChatFormatting.DARK_GRAY).style(ChatFormatting.ITALIC).component()
               ),
               mouseX,
               mouseY
            );
         }
      }

      if (this.addressBox.getValue().isBlank() && !this.addressBox.isFocused() && this.addressBox.isHovered()) {
         graphics.renderComponentTooltip(
            this.font,
            List.of(
               CreateLang.translate("gui.factory_panel.restocker_address").color(ScrollInput.HEADER_RGB).component(),
               CreateLang.translate("gui.schedule.lmb_edit").style(ChatFormatting.DARK_GRAY).style(ChatFormatting.ITALIC).component()
            ),
            mouseX,
            mouseY
         );
      }
   }

   private void renderItemEntry(GuiGraphics graphics, float scale, BigItemStack entry, boolean isStackHovered, boolean isRenderingOrders) {
      int customCount = entry.count;
      ItemStack stackWithCount = entry.stack.copyWithCount(customCount);
      if (!isRenderingOrders) {
         BigItemStack order = this.getOrderForItem(stackWithCount);
         if (entry.count < 1000000000) {
            int forcedCount = this.forcedEntries.getCountOf(stackWithCount);
            if (forcedCount != 0) {
               customCount = Math.min(customCount, -forcedCount - 1);
            }

            if (order != null) {
               customCount -= order.count;
            }

            customCount = Math.max(0, customCount);
         }

         AllGuiTextures.STOCK_KEEPER_REQUEST_SLOT.render(graphics, 0, 0);
      }

      boolean craftable = entry instanceof CraftableBigItemStack;
      PoseStack ms = graphics.pose();
      ms.pushPose();
      float scaleFromHover = 1.0F;
      if (isStackHovered) {
         scaleFromHover += 0.075F;
      }

      ms.translate(1.0, 1.0, 0.0);
      ms.translate(9.0, 9.0, 0.0);
      ms.scale(scale, scale, scale);
      ms.scale(scaleFromHover, scaleFromHover, scaleFromHover);
      ms.translate(-9.0, -9.0, 0.0);
      if (customCount != 0 || craftable) {
         GuiGameElement.of(stackWithCount).render(graphics);
      }

      ms.popPose();
      ms.pushPose();
      ms.translate(0.0F, 0.0F, 190.0F);
      if (customCount != 0 || craftable) {
         graphics.renderItemDecorations(this.font, stackWithCount, 1, 1, "");
      }

      ms.translate(0.0F, 0.0F, 10.0F);
      if (customCount > 1 || craftable) {
         this.drawItemCount(graphics, entry.count, customCount);
      }

      ms.popPose();
   }

   private void drawItemCount(GuiGraphics graphics, int count, int customCount) {
      String text = customCount >= 1000000
         ? customCount / 1000000 + "m"
         : (
            customCount >= 10000
               ? customCount / 1000 + "k"
               : (customCount >= 1000 ? (float)(customCount * 10 / 1000) / 10.0F + "k" : (customCount >= 100 ? customCount + "" : " " + customCount))
         );
      if (customCount >= 1000000000) {
         text = "+";
      }

      if (!text.isBlank()) {
         int x = (int)Math.floor((double)(-text.length()) * 2.5);

         for (char c : text.toCharArray()) {
            int index = c - '0';
            int xOffset = index * 6;
            int spriteWidth = NUMBERS.getWidth();
            switch (c) {
               case ' ':
                  x += 4;
                  continue;
               case '+':
                  spriteWidth = 9;
                  xOffset = 84;
                  break;
               case '.':
                  spriteWidth = 3;
                  xOffset = 60;
                  break;
               case 'k':
                  xOffset = 64;
                  break;
               case 'm':
                  spriteWidth = 7;
                  xOffset = 70;
            }

            RenderSystem.enableBlend();
            graphics.blit(
               NUMBERS.location, 14 + x, 10, 0, (float)(NUMBERS.getStartX() + xOffset), (float)NUMBERS.getStartY(), spriteWidth, NUMBERS.getHeight(), 256, 256
            );
            x += spriteWidth - 1;
         }
      }
   }

   @Nullable
   private BigItemStack getOrderForItem(ItemStack stack) {
      for (BigItemStack entry : this.itemsToOrder) {
         if (ItemStack.isSameItemSameComponents(stack, entry.stack)) {
            return entry;
         }
      }

      return null;
   }

   private void revalidateOrders() {
      Set<BigItemStack> invalid = new HashSet<>(this.itemsToOrder);
      InventorySummary summary = this.blockEntity.lastClientsideStockSnapshotAsSummary;
      if (this.currentItemSource != null && summary != null) {
         for (BigItemStack entry : this.itemsToOrder) {
            entry.count = Math.min(summary.getCountOf(entry.stack), entry.count);
            if (entry.count > 0) {
               invalid.remove(entry);
            }
         }

         this.itemsToOrder.removeAll(invalid);
      } else {
         this.itemsToOrder.removeAll(invalid);
      }
   }

   private Couple<Integer> getHoveredSlot(int x, int y) {
      x++;
      if (x < this.itemsX || x >= this.itemsX + 180 || this.isSchematicListMode()) {
         return this.noneHovered;
      } else if (y >= this.orderY && y < this.orderY + 20) {
         int col = (x - this.itemsX) / 20;
         return this.itemsToOrder.size() > col && col >= 0 ? Couple.create(-1, col) : this.noneHovered;
      } else {
         if (y >= this.orderY - 31 && y < this.orderY - 31 + 20) {
            int jeiX = this.getGuiLeft() + (this.windowWidth - 20 * this.recipesToOrder.size()) / 2 + 1;
            int col = Mth.floorDiv(x - jeiX, 20);
            if (this.recipesToOrder.size() > col && col >= 0) {
               return Couple.create(-2, col);
            }
         }

         if (y >= this.getGuiTop() + 16 && y <= this.getGuiTop() + this.windowHeight - 80) {
            if (!this.itemScroll.settled()) {
               return this.noneHovered;
            } else {
               int localY = y - this.itemsY;

               for (int categoryIndex = 0; categoryIndex < this.displayedItems.size(); categoryIndex++) {
                  StockKeeperRequestScreen.CategoryEntry entry = this.categories.isEmpty()
                     ? new StockKeeperRequestScreen.CategoryEntry(0, "", 0)
                     : this.categories.get(categoryIndex);
                  if (!entry.hidden) {
                     int row = Mth.floor((float)(localY - (this.categories.isEmpty() ? 4 : 20) - entry.y) / 20.0F + this.itemScroll.getChaseTarget());
                     int col = (x - this.itemsX) / 20;
                     int slot = row * 9 + col;
                     if (slot < 0) {
                        return this.noneHovered;
                     }

                     if (this.displayedItems.get(categoryIndex).size() > slot) {
                        return Couple.create(categoryIndex, slot);
                     }
                  }
               }

               return this.noneHovered;
            }
         } else {
            return this.noneHovered;
         }
      }
   }

   public Optional<Pair<ItemStack, Rect2i>> getHoveredIngredient(int mouseX, int mouseY) {
      Couple<Integer> hoveredSlot = this.getHoveredSlot(mouseX, mouseY);
      if (hoveredSlot != this.noneHovered) {
         int index = (Integer)hoveredSlot.getSecond();
         boolean recipeHovered = (Integer)hoveredSlot.getFirst() == -2;
         boolean orderHovered = (Integer)hoveredSlot.getFirst() == -1;
         int x;
         int y;
         BigItemStack entry;
         if (recipeHovered) {
            int jeiX = this.getGuiLeft() + (this.windowWidth - 20 * this.recipesToOrder.size()) / 2 + 1;
            int jeiY = this.orderY - 31;
            x = jeiX + index * 20;
            y = jeiY;
            entry = this.recipesToOrder.get(index);
         } else if (orderHovered) {
            x = this.itemsX + index * 20;
            y = this.orderY;
            entry = this.itemsToOrder.get(index);
         } else {
            int categoryIndex = (Integer)hoveredSlot.getFirst();
            int categoryY = this.categories.isEmpty() ? 0 : this.categories.get(categoryIndex).y;
            x = this.itemsX + index % 9 * 20;
            y = this.itemsY + categoryY + (this.categories.isEmpty() ? 4 : 20) + index / 9 * 20;
            entry = this.displayedItems.get(categoryIndex).get(index);
         }

         Rect2i bounds = new Rect2i(x, y, x + 18, y + 18);
         return Optional.of(Pair.of(entry.stack.copy(), bounds));
      } else {
         return Optional.empty();
      }
   }

   private boolean isConfirmHovered(int mouseX, int mouseY) {
      int confirmX = this.getGuiLeft() + 143;
      int confirmY = this.getGuiTop() + this.windowHeight - 39;
      int confirmW = 78;
      int confirmH = 18;
      return mouseX < confirmX || mouseX >= confirmX + confirmW ? false : mouseY >= confirmY && mouseY < confirmY + confirmH;
   }

   private Component getTroubleshootingMessage() {
      if (this.currentItemSource == null) {
         return CreateLang.translate("gui.stock_keeper.checking_stocks").component();
      } else if (this.blockEntity.activeLinks == 0) {
         return CreateLang.translate("gui.stock_keeper.no_packagers_linked").component();
      } else if (this.currentItemSource.isEmpty()) {
         return CreateLang.translate("gui.stock_keeper.inventories_empty").component();
      } else {
         return this.isSchematicListMode()
            ? CreateLang.translate(this.itemsToOrder.isEmpty() ? "gui.stock_keeper.schematic_list.no_results" : "gui.stock_keeper.schematic_list.requesting")
               .component()
            : CreateLang.translate("gui.stock_keeper.no_search_results").component();
      }
   }

   @Override
   public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
      boolean lmb = pButton == 0;
      boolean rmb = pButton == 1;
      if (rmb && this.searchBox.isMouseOver(pMouseX, pMouseY)) {
         this.searchBox.setValue("");
         this.refreshSearchNextTick = true;
         this.moveToTopNextTick = true;
         this.searchBox.setFocused(true);
         this.syncJEI(false);
         return true;
      } else {
         if (this.addressBox.isFocused()) {
            boolean result = this.addressBox.mouseClicked(pMouseX, pMouseY, pButton);
            if (this.addressBox.isHovered() || result) {
               return result;
            }

            this.addressBox.setFocused(false);
         }

         if (this.searchBox.isFocused()) {
            if (this.searchBox.isHovered()) {
               return this.searchBox.mouseClicked(pMouseX, pMouseY, pButton);
            }

            this.searchBox.setFocused(false);
         }

         int barX = this.itemsX + 180 - 1;
         if (this.getMaxScroll() > 0
            && lmb
            && pMouseX > (double)barX
            && pMouseX <= (double)(barX + 8)
            && pMouseY > (double)(this.getGuiTop() + 15)
            && pMouseY < (double)(this.getGuiTop() + this.windowHeight - 82)) {
            this.scrollHandleActive = true;
            if (this.minecraft.isWindowActive()) {
               GLFW.glfwSetInputMode(this.minecraft.getWindow().getWindow(), 208897, 212994);
            }

            return true;
         } else {
            Couple<Integer> hoveredSlot = this.getHoveredSlot((int)pMouseX, (int)pMouseY);
            if (this.itemScroll.getChaseTarget() == 0.0F
               && lmb
               && pMouseY > (double)this.besideSearchButtonY
               && pMouseY <= (double)(this.besideSearchButtonY + 15)) {
               if (pMouseX > (double)this.jeiSyncX && pMouseX <= (double)(this.jeiSyncX + 15)) {
                  StockKeeperRequestScreen.SearchSyncMode.cycleConfig();
                  this.refreshSearchNextTick = true;
                  this.moveToTopNextTick = true;
                  this.syncJEI(false);
                  this.playUiSound((SoundEvent)SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 1.0F);
                  return true;
               }

               if (this.isAdmin && pMouseX > (double)this.lockX && pMouseX <= (double)(this.lockX + 15)) {
                  this.isLocked = !this.isLocked;
                  CatnipServices.NETWORK.sendToServer(new StockKeeperLockPacket(this.blockEntity.getBlockPos(), this.isLocked));
                  this.playUiSound((SoundEvent)SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 1.0F);
                  return true;
               }
            }

            if (lmb && this.isConfirmHovered((int)pMouseX, (int)pMouseY)) {
               this.sendIt();
               this.playUiSound((SoundEvent)SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 1.0F);
               return true;
            } else {
               int localY = (int)(pMouseY - (double)this.itemsY);
               if (this.itemScroll.settled()
                  && lmb
                  && !this.categories.isEmpty()
                  && pMouseX >= (double)this.itemsX
                  && pMouseX < (double)(this.itemsX + 180)
                  && pMouseY >= (double)(this.getGuiTop() + 16)
                  && pMouseY <= (double)(this.getGuiTop() + this.windowHeight - 80)) {
                  for (int categoryIndex = 0; categoryIndex < this.displayedItems.size(); categoryIndex++) {
                     StockKeeperRequestScreen.CategoryEntry entry = this.categories.get(categoryIndex);
                     if (Mth.floor((float)(localY - entry.y) / 20.0F + this.itemScroll.getChaseTarget()) == 0
                        && !this.displayedItems.get(categoryIndex).isEmpty()) {
                        int indexOf = entry.targetBECategory;
                        if (indexOf < this.blockEntity.categories.size()) {
                           if (!entry.hidden) {
                              this.hiddenCategories.add(indexOf);
                              this.playUiSound(SoundEvents.ITEM_FRAME_ROTATE_ITEM, 1.0F, 1.5F);
                           } else {
                              this.hiddenCategories.remove(indexOf);
                              this.playUiSound(SoundEvents.ITEM_FRAME_ROTATE_ITEM, 1.0F, 0.675F);
                           }

                           this.refreshSearchNextTick = true;
                           this.moveToTopNextTick = false;
                           return true;
                        }
                     }
                  }
               }

               if (hoveredSlot != this.noneHovered && (lmb || rmb)) {
                  boolean orderClicked = (Integer)hoveredSlot.getFirst() == -1;
                  boolean recipeClicked = (Integer)hoveredSlot.getFirst() == -2;
                  BigItemStack entry = recipeClicked
                     ? this.recipesToOrder.get((Integer)hoveredSlot.getSecond())
                     : (
                        orderClicked
                           ? this.itemsToOrder.get((Integer)hoveredSlot.getSecond())
                           : this.displayedItems.get((Integer)hoveredSlot.getFirst()).get((Integer)hoveredSlot.getSecond())
                     );
                  ItemStack itemStack = entry.stack;
                  int transfer = hasShiftDown() ? itemStack.getMaxStackSize() : (hasControlDown() ? 10 : 1);
                  if (!recipeClicked || !(entry instanceof CraftableBigItemStack cbis)) {
                     BigItemStack existingOrder = this.getOrderForItem(entry.stack);
                     if (existingOrder == null) {
                        if (this.itemsToOrder.size() >= 9 || rmb) {
                           return true;
                        }

                        this.itemsToOrder.add(existingOrder = new BigItemStack(itemStack.copyWithCount(1), 0));
                        this.playUiSound(SoundEvents.WOOL_STEP, 0.75F, 1.2F);
                        this.playUiSound(SoundEvents.BAMBOO_WOOD_STEP, 0.75F, 0.8F);
                     }

                     int current = existingOrder.count;
                     if (!rmb && !orderClicked) {
                        existingOrder.count = current + Math.min(transfer, entry.count - current);
                        return true;
                     }

                     existingOrder.count = current - transfer;
                     if (existingOrder.count <= 0) {
                        this.itemsToOrder.remove(existingOrder);
                        this.playUiSound(SoundEvents.WOOL_STEP, 0.75F, 1.8F);
                        this.playUiSound(SoundEvents.BAMBOO_WOOD_STEP, 0.75F, 1.8F);
                     }

                     return true;
                  }

                  if (rmb && cbis.count == 0) {
                     this.recipesToOrder.remove(cbis);
                     return true;
                  } else {
                     this.requestCraftable(cbis, rmb ? -transfer : transfer);
                     return true;
                  }
               } else {
                  return super.mouseClicked(pMouseX, pMouseY, pButton);
               }
            }
         }
      }
   }

   public boolean mouseReleased(double pMouseX, double pMouseY, int pButton) {
      if (pButton == 0 && this.scrollHandleActive) {
         this.scrollHandleActive = false;
         if (this.minecraft.isWindowActive()) {
            GLFW.glfwSetInputMode(this.minecraft.getWindow().getWindow(), 208897, 212993);
         }
      }

      return super.mouseReleased(pMouseX, pMouseY, pButton);
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (this.addressBox.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
         return true;
      } else {
         Couple<Integer> hoveredSlot = this.getHoveredSlot((int)mouseX, (int)mouseY);
         boolean noHover = hoveredSlot == this.noneHovered;
         if (!noHover && ((Integer)hoveredSlot.getFirst() < 0 || hasShiftDown() || this.getMaxScroll() == 0)) {
            boolean orderClicked = (Integer)hoveredSlot.getFirst() == -1;
            boolean recipeClicked = (Integer)hoveredSlot.getFirst() == -2;
            BigItemStack entry = recipeClicked
               ? this.recipesToOrder.get((Integer)hoveredSlot.getSecond())
               : (
                  orderClicked
                     ? this.itemsToOrder.get((Integer)hoveredSlot.getSecond())
                     : this.displayedItems.get((Integer)hoveredSlot.getFirst()).get((Integer)hoveredSlot.getSecond())
               );
            boolean remove = scrollY < 0.0;
            int transfer = Mth.ceil(Math.abs(scrollY)) * (hasControlDown() ? 10 : 1);
            if (recipeClicked && entry instanceof CraftableBigItemStack cbis) {
               this.requestCraftable(cbis, remove ? -transfer : transfer);
               return true;
            } else {
               BigItemStack existingOrder = orderClicked ? entry : this.getOrderForItem(entry.stack);
               if (existingOrder == null) {
                  if (this.itemsToOrder.size() >= 9 || remove) {
                     return true;
                  }

                  this.itemsToOrder.add(existingOrder = new BigItemStack(entry.stack.copyWithCount(1), 0));
                  this.playUiSound(SoundEvents.WOOL_STEP, 0.75F, 1.2F);
                  this.playUiSound(SoundEvents.BAMBOO_WOOD_STEP, 0.75F, 0.8F);
               }

               int current = existingOrder.count;
               if (remove) {
                  existingOrder.count = current - transfer;
                  if (existingOrder.count <= 0) {
                     this.itemsToOrder.remove(existingOrder);
                     this.playUiSound(SoundEvents.WOOL_STEP, 0.75F, 1.8F);
                     this.playUiSound(SoundEvents.BAMBOO_WOOD_STEP, 0.75F, 1.8F);
                  } else if (existingOrder.count != current) {
                     this.playUiSound(AllSoundEvents.SCROLL_VALUE.getMainEvent(), 0.25F, 1.2F);
                  }

                  return true;
               } else {
                  existingOrder.count = current
                     + Math.min(transfer, this.blockEntity.getLastClientsideStockSnapshotAsSummary().getCountOf(entry.stack) - current);
                  if (existingOrder.count != current && current != 0) {
                     this.playUiSound(AllSoundEvents.SCROLL_VALUE.getMainEvent(), 0.25F, 1.2F);
                  }

                  return true;
               }
            }
         } else {
            int maxScroll = this.getMaxScroll();
            int direction = (int)(Math.ceil(Math.abs(scrollY)) * -Math.signum(scrollY));
            float newTarget = (float)Mth.clamp(Math.round(this.itemScroll.getChaseTarget() + (float)direction), 0, maxScroll);
            this.itemScroll.chase((double)newTarget, 0.5, Chaser.EXP);
            return true;
         }
      }
   }

   private void clampScrollBar() {
      int maxScroll = this.getMaxScroll();
      float prevTarget = this.itemScroll.getChaseTarget();
      float newTarget = Mth.clamp(prevTarget, 0.0F, (float)maxScroll);
      if (prevTarget != newTarget) {
         this.itemScroll.startWithValue((double)newTarget);
      }
   }

   private int getMaxScroll() {
      int visibleHeight = this.windowHeight - 84;
      int totalRows = 2;

      for (int i = 0; i < this.displayedItems.size(); i++) {
         List<BigItemStack> list = this.displayedItems.get(i);
         if (!list.isEmpty()) {
            totalRows++;
            if (this.categories.size() <= i || !this.categories.get(i).hidden) {
               totalRows = (int)((double)totalRows + Math.ceil((double)((float)list.size() / 9.0F)));
            }
         }
      }

      return Math.max(0, (totalRows * 20 - visibleHeight + 50) / 20);
   }

   public boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY) {
      if (pButton == 0 && this.scrollHandleActive) {
         Window window = this.minecraft.getWindow();
         double scaleX = (double)window.getGuiScaledWidth() / (double)window.getScreenWidth();
         double scaleY = (double)window.getGuiScaledHeight() / (double)window.getScreenHeight();
         int windowH = this.windowHeight - 92;
         int totalH = this.getMaxScroll() * 20 + windowH;
         int barSize = Math.max(5, Mth.floor((float)windowH / (float)totalH * (float)(windowH - 2)));
         int minY = this.getGuiTop() + 15 + barSize / 2;
         int maxY = this.getGuiTop() + 15 + windowH - barSize / 2;
         if (barSize >= windowH - 2) {
            return true;
         } else {
            int barX = this.itemsX + 180;
            double target = (pMouseY - (double)this.getGuiTop() - 15.0 - (double)barSize / 2.0) * (double)totalH / (double)(windowH - 2) / 20.0;
            this.itemScroll.chase(Mth.clamp(target, 0.0, (double)this.getMaxScroll()), 0.8, Chaser.EXP);
            if (this.minecraft.isWindowActive()) {
               double forceX = (double)(barX + 2) / scaleX;
               double forceY = Mth.clamp(pMouseY, (double)minY, (double)maxY) / scaleY;
               GLFW.glfwSetCursorPos(window.getWindow(), forceX, forceY);
            }

            return true;
         }
      } else {
         return super.mouseDragged(pMouseX, pMouseY, pButton, pDragX, pDragY);
      }
   }

   public boolean charTyped(char pCodePoint, int pModifiers) {
      if (this.ignoreTextInput) {
         return false;
      } else if (this.addressBox.isFocused() && this.addressBox.charTyped(pCodePoint, pModifiers)) {
         return true;
      } else {
         String s = this.searchBox.getValue();
         if (!this.searchBox.charTyped(pCodePoint, pModifiers)) {
            return false;
         } else {
            if (!Objects.equals(s, this.searchBox.getValue())) {
               this.refreshSearchNextTick = true;
               this.moveToTopNextTick = true;
               this.syncJEI(false);
            }

            return true;
         }
      }
   }

   @Override
   public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
      this.ignoreTextInput = false;
      if (!this.addressBox.isFocused() && !this.searchBox.isFocused() && this.minecraft.options.keyChat.matches(pKeyCode, pScanCode)) {
         this.ignoreTextInput = true;
         this.searchBox.setFocused(true);
         return true;
      } else if (pKeyCode == 257 && this.searchBox.isFocused()) {
         this.searchBox.setFocused(false);
         return true;
      } else if (pKeyCode == 257 && hasShiftDown()) {
         this.sendIt();
         return true;
      } else if (this.addressBox.isFocused() && this.addressBox.keyPressed(pKeyCode, pScanCode, pModifiers)) {
         return true;
      } else {
         String s = this.searchBox.getValue();
         if (this.searchBox.keyPressed(pKeyCode, pScanCode, pModifiers)) {
            if (!Objects.equals(s, this.searchBox.getValue())) {
               this.refreshSearchNextTick = true;
               this.moveToTopNextTick = true;
               this.syncJEI(false);
            }

            return true;
         } else {
            return this.searchBox.isFocused() && this.searchBox.isVisible() && pKeyCode != 256 || super.keyPressed(pKeyCode, pScanCode, pModifiers);
         }
      }
   }

   public void removed() {
      BlockPos pos = this.blockEntity.getBlockPos();
      CatnipServices.NETWORK.sendToServer(new PackageOrderRequestPacket(pos, PackageOrderWithCrafts.empty(), this.addressBox.getValue(), false));
      CatnipServices.NETWORK.sendToServer(new StockKeeperCategoryHidingPacket(pos, new ArrayList<>(this.hiddenCategories)));
      super.removed();
   }

   private void sendIt() {
      this.revalidateOrders();
      if (!this.itemsToOrder.isEmpty()) {
         this.forcedEntries = new InventorySummary();
         InventorySummary summary = this.blockEntity.getLastClientsideStockSnapshotAsSummary();

         for (BigItemStack toOrder : this.itemsToOrder) {
            int countOf = summary.getCountOf(toOrder.stack);
            if (countOf != 1000000000) {
               this.forcedEntries.add(toOrder.stack.copy(), -1 - Math.max(0, countOf - toOrder.count));
            }
         }

         PackageOrderWithCrafts order = PackageOrderWithCrafts.simple(this.itemsToOrder);
         if (this.canRequestCraftingPackage && !this.itemsToOrder.isEmpty() && !this.recipesToOrder.isEmpty()) {
            List<PackageOrderWithCrafts.CraftingEntry> craftList = new ArrayList<>();

            for (CraftableBigItemStack cbis : this.recipesToOrder) {
               Recipe craftedCount = cbis.recipe;
               if (craftedCount instanceof CraftingRecipe) {
                  CraftingRecipe cr = (CraftingRecipe)craftedCount;
                  int craftedCountx = 0;
                  int targetCount = cbis.count / cbis.getOutputCount(this.blockEntity.getLevel());
                  List<BigItemStack> mutableOrder = BigItemStack.duplicateWrappers(this.itemsToOrder);

                  while (craftedCountx < targetCount) {
                     PackageOrder pattern = new PackageOrder(FactoryPanelScreen.convertRecipeToPackageOrderContext(cr, mutableOrder, true));
                     int maxCrafts = targetCount - craftedCountx;
                     int availableCrafts = 0;

                     label84:
                     for (boolean itemsExhausted = false; availableCrafts < maxCrafts && !itemsExhausted; availableCrafts++) {
                        List<BigItemStack> previousSnapshot = BigItemStack.duplicateWrappers(mutableOrder);
                        itemsExhausted = true;

                        label81:
                        for (BigItemStack patternStack : pattern.stacks()) {
                           if (!patternStack.stack.isEmpty()) {
                              for (BigItemStack ordered : mutableOrder) {
                                 if (ItemStack.isSameItemSameComponents(ordered.stack, patternStack.stack) && ordered.count != 0) {
                                    ordered.count--;
                                    itemsExhausted = false;
                                    continue label81;
                                 }
                              }

                              mutableOrder = previousSnapshot;
                              break label84;
                           }
                        }
                     }

                     if (availableCrafts == 0) {
                        break;
                     }

                     craftList.add(new PackageOrderWithCrafts.CraftingEntry(pattern, availableCrafts));
                     craftedCountx += availableCrafts;
                  }
               }
            }

            order = new PackageOrderWithCrafts(order.orderedStacks(), craftList);
         }

         CatnipServices.NETWORK
            .sendToServer(new PackageOrderRequestPacket(this.blockEntity.getBlockPos(), order, this.addressBox.getValue(), this.encodeRequester));
         this.itemsToOrder = new ArrayList<>();
         this.recipesToOrder = new ArrayList<>();
         this.blockEntity.ticksSinceLastUpdate = 10;
         this.successTicks = 1;
         if (this.isSchematicListMode()) {
            ((StockKeeperRequestMenu)this.menu).player.closeContainer();
         }
      }
   }

   public boolean keyReleased(int pKeyCode, int pScanCode, int pModifiers) {
      this.ignoreTextInput = false;
      return super.keyReleased(pKeyCode, pScanCode, pModifiers);
   }

   @Override
   public List<Rect2i> getExtraAreas() {
      return this.extraAreas;
   }

   public boolean isSchematicListMode() {
      return this.clipboardItem != null;
   }

   public void requestSchematicList() {
      this.itemsToOrder.clear();
      InventorySummary availableItems = this.blockEntity.getLastClientsideStockSnapshotAsSummary();

      for (List<ClipboardEntry> list : this.clipboardItem) {
         for (ClipboardEntry entry : list) {
            ItemStack stack = entry.icon;
            int toOrder = Math.min(entry.itemAmount, availableItems.getCountOf(stack));
            if (toOrder != 0) {
               this.itemsToOrder.add(new BigItemStack(stack, toOrder));
            }
         }
      }
   }

   public void requestCraftable(CraftableBigItemStack cbis, int requestedDifference) {
      boolean takeOrdersAway = requestedDifference < 0;
      if (takeOrdersAway) {
         requestedDifference = Math.max(-cbis.count, requestedDifference);
      }

      if (requestedDifference != 0) {
         InventorySummary availableItems = this.blockEntity.getLastClientsideStockSnapshotAsSummary();
         Function<ItemStack, Integer> countModifier = stack -> {
            BigItemStack ordered = this.getOrderForItem(stack);
            return ordered == null ? 0 : -ordered.count;
         };
         if (takeOrdersAway) {
            availableItems = new InventorySummary();

            for (BigItemStack ordered : this.itemsToOrder) {
               availableItems.add(ordered.stack, ordered.count);
            }

            countModifier = stack -> 0;
         }

         Pair<Integer, List<List<BigItemStack>>> craftingResult = this.maxCraftable(
            cbis, availableItems, countModifier, takeOrdersAway ? -1 : 9 - this.itemsToOrder.size()
         );
         int outputCount = cbis.getOutputCount(this.blockEntity.getLevel());
         int adjustToRecipeAmount = Mth.ceil((float)Math.abs(requestedDifference) / (float)outputCount) * outputCount;
         int maxCraftable = Math.min(adjustToRecipeAmount, (Integer)craftingResult.getFirst());
         if (maxCraftable != 0) {
            cbis.count += takeOrdersAway ? -maxCraftable : maxCraftable;

            for (List<BigItemStack> list : (List)craftingResult.getSecond()) {
               int remaining = maxCraftable / outputCount;

               for (BigItemStack entry : list) {
                  if (remaining <= 0) {
                     break;
                  }

                  int toTransfer = Math.min(remaining, entry.count);
                  BigItemStack order = this.getOrderForItem(entry.stack);
                  if (takeOrdersAway) {
                     if (order != null) {
                        order.count -= toTransfer;
                        if (order.count == 0) {
                           this.itemsToOrder.remove(order);
                        }
                     }
                  } else {
                     if (order == null) {
                        this.itemsToOrder.add(order = new BigItemStack(entry.stack.copyWithCount(1), 0));
                     }

                     order.count += toTransfer;
                  }

                  remaining -= entry.count;
               }
            }

            this.updateCraftableAmounts();
         }
      }
   }

   private void updateCraftableAmounts() {
      InventorySummary usedItems = new InventorySummary();
      InventorySummary availableItems = new InventorySummary();

      for (BigItemStack ordered : this.itemsToOrder) {
         availableItems.add(ordered.stack, ordered.count);
      }

      for (CraftableBigItemStack cbis : this.recipesToOrder) {
         Pair<Integer, List<List<BigItemStack>>> craftingResult = this.maxCraftable(cbis, availableItems, stack -> -usedItems.getCountOf(stack), -1);
         int maxCraftable = (Integer)craftingResult.getFirst();
         List<List<BigItemStack>> validEntriesByIngredient = (List<List<BigItemStack>>)craftingResult.getSecond();
         int outputCount = cbis.getOutputCount(this.blockEntity.getLevel());
         cbis.count = Math.min(cbis.count, maxCraftable);

         for (List<BigItemStack> list : validEntriesByIngredient) {
            int remaining = cbis.count / outputCount;

            for (BigItemStack entry : list) {
               if (remaining <= 0) {
                  break;
               }

               usedItems.add(entry.stack, Math.min(remaining, entry.count));
               remaining -= entry.count;
            }
         }
      }

      this.canRequestCraftingPackage = false;

      for (BigItemStack ordered : this.itemsToOrder) {
         if (usedItems.getCountOf(ordered.stack) != ordered.count) {
            return;
         }
      }

      this.canRequestCraftingPackage = true;
   }

   private Pair<Integer, List<List<BigItemStack>>> maxCraftable(
      CraftableBigItemStack cbis, InventorySummary summary, Function<ItemStack, Integer> countModifier, int newTypeLimit
   ) {
      List<Ingredient> ingredients = cbis.getIngredients();
      List<List<BigItemStack>> validEntriesByIngredient = new ArrayList<>();
      List<BigItemStack> alreadyCreated = new ArrayList<>();

      for (Ingredient ingredient : ingredients) {
         if (!ingredient.isEmpty()) {
            List<BigItemStack> valid = new ArrayList<>();

            for (List<BigItemStack> list : summary.getItemMap().values()) {
               label85:
               for (BigItemStack entry : list) {
                  if (ingredient.test(entry.stack)) {
                     for (BigItemStack visitedStack : alreadyCreated) {
                        if (ItemStack.isSameItemSameComponents(visitedStack.stack, entry.stack)) {
                           valid.add(visitedStack);
                           continue label85;
                        }
                     }

                     BigItemStack asBis = new BigItemStack(entry.stack, summary.getCountOf(entry.stack) + countModifier.apply(entry.stack));
                     if (asBis.count > 0) {
                        valid.add(asBis);
                        alreadyCreated.add(asBis);
                     }
                  }
               }
            }

            if (valid.isEmpty()) {
               return Pair.of(0, List.of());
            }

            Collections.sort(valid, (bis1, bis2) -> -Integer.compare(summary.getCountOf(bis1.stack), summary.getCountOf(bis2.stack)));
            validEntriesByIngredient.add(valid);
         }
      }

      if (newTypeLimit != -1) {
         int toRemove = (int)validEntriesByIngredient.stream()
               .flatMap(l -> l.stream())
               .filter(entryx -> this.getOrderForItem(entryx.stack) == null)
               .distinct()
               .count()
            - newTypeLimit;

         for (int i = 0; i < toRemove; i++) {
            this.removeLeastEssentialItemStack(validEntriesByIngredient);
         }
      }

      validEntriesByIngredient = this.resolveIngredientAmounts(validEntriesByIngredient);
      int minCount = Integer.MAX_VALUE;

      for (List<BigItemStack> list : validEntriesByIngredient) {
         int sum = 0;

         for (BigItemStack entryx : list) {
            sum += entryx.count;
         }

         minCount = Math.min(sum, minCount);
      }

      if (minCount == 0) {
         return Pair.of(0, List.of());
      } else {
         int outputCount = cbis.getOutputCount(this.blockEntity.getLevel());
         return Pair.of(minCount * outputCount, validEntriesByIngredient);
      }
   }

   private void removeLeastEssentialItemStack(List<List<BigItemStack>> validIngredients) {
      List<BigItemStack> longest = null;
      int most = 0;

      for (List<BigItemStack> list : validIngredients) {
         int count = (int)list.stream().filter(entry -> this.getOrderForItem(entry.stack) == null).count();
         if (longest == null || count > most) {
            longest = list;
            most = count;
         }
      }

      if (!longest.isEmpty()) {
         BigItemStack chosen = null;

         for (int i = 0; i < longest.size(); i++) {
            BigItemStack entry = longest.get(longest.size() - 1 - i);
            if (this.getOrderForItem(entry.stack) == null) {
               chosen = entry;
               break;
            }
         }

         for (List<BigItemStack> listx : validIngredients) {
            listx.remove(chosen);
         }
      }
   }

   private List<List<BigItemStack>> resolveIngredientAmounts(List<List<BigItemStack>> validIngredients) {
      List<List<BigItemStack>> resolvedIngredients = new ArrayList<>();

      for (int i = 0; i < validIngredients.size(); i++) {
         resolvedIngredients.add(new ArrayList<>());
      }

      boolean everythingTaken = false;

      while (!everythingTaken) {
         everythingTaken = true;

         label41:
         for (int i = 0; i < validIngredients.size(); i++) {
            List<BigItemStack> list = validIngredients.get(i);
            List<BigItemStack> resolvedList = resolvedIngredients.get(i);

            for (BigItemStack bigItemStack : list) {
               if (bigItemStack.count != 0) {
                  bigItemStack.count--;
                  everythingTaken = false;

                  for (BigItemStack resolvedItemStack : resolvedList) {
                     if (resolvedItemStack.stack == bigItemStack.stack) {
                        resolvedItemStack.count++;
                        continue label41;
                     }
                  }

                  resolvedList.add(new BigItemStack(bigItemStack.stack, 1));
                  break;
               }
            }
         }
      }

      return resolvedIngredients;
   }

   private boolean shouldSyncFromJEI() {
      if (!Mods.JEI.isLoaded()) {
         return false;
      } else {
         boolean hasFocus = CreateJEI.runtime.getIngredientListOverlay().hasKeyboardFocus();
         return hasFocus && !this.previousJEISearchText.equals(CreateJEI.runtime.getIngredientFilter().getFilterText());
      }
   }

   private void syncJEI(boolean fromJei) {
      if (Mods.JEI.isLoaded()) {
         StockKeeperRequestScreen.SearchSyncMode mode = (StockKeeperRequestScreen.SearchSyncMode)AllConfigs.client().syncRecipeViewerSearch.get();
         if (mode != StockKeeperRequestScreen.SearchSyncMode.NONE) {
            IIngredientFilter filter = CreateJEI.runtime.getIngredientFilter();
            if (mode.isBothOr(StockKeeperRequestScreen.SearchSyncMode.SYNC_FROM_JEI) && fromJei) {
               this.previousJEISearchText = filter.getFilterText();
               this.searchBox.setValue(this.previousJEISearchText);
            } else if (mode.isBothOr(StockKeeperRequestScreen.SearchSyncMode.SYNC_FROM_STOCK_KEEPER) && !fromJei) {
               filter.setFilterText(this.searchBox.getValue());
            }
         }
      }
   }

   public static class CategoryEntry {
      boolean hidden;
      String name;
      int y;
      int targetBECategory;

      public CategoryEntry(int targetBECategory, String name, int y) {
         this.targetBECategory = targetBECategory;
         this.name = name;
         this.hidden = false;
         this.y = y;
      }
   }

   public static enum SearchSyncMode implements StringRepresentable {
      SYNC_BOTH(AllGuiTextures.STOCK_KEEPER_SEARCH_SYNC_BOTH),
      SYNC_FROM_JEI(AllGuiTextures.STOCK_KEEPER_SEARCH_SYNC_FROM_JEI),
      SYNC_FROM_STOCK_KEEPER(AllGuiTextures.STOCK_KEEPER_SEARCH_SYNC_FROM_STOCK_KEEPER),
      NONE(AllGuiTextures.STOCK_KEEPER_SEARCH_SYNC_DISABLED);

      public final AllGuiTextures buttonTexture;

      private SearchSyncMode(AllGuiTextures buttonTexture) {
         this.buttonTexture = buttonTexture;
      }

      public boolean isBothOr(StockKeeperRequestScreen.SearchSyncMode mode) {
         return this == SYNC_BOTH || this == mode;
      }

      public StockKeeperRequestScreen.SearchSyncMode next() {
         StockKeeperRequestScreen.SearchSyncMode[] vals = values();
         return vals[(this.ordinal() + 1) % vals.length];
      }

      public static void cycleConfig() {
         ConfigEnum<StockKeeperRequestScreen.SearchSyncMode> modeConfig = AllConfigs.client().syncRecipeViewerSearch;
         modeConfig.set(((StockKeeperRequestScreen.SearchSyncMode)modeConfig.get()).next());
      }

      public String getSerializedName() {
         return Lang.asId(this.name());
      }
   }
}
