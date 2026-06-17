package dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.screen;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.IconButton;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterBlock;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterBlockEntity;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterEntries;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterInteractionHandler;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.screen.widgets.ConfirmationWidgetBase;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.screen.widgets.KeyWidget;
import dev.simulated_team.simulated.data.SimLang;
import dev.simulated_team.simulated.index.SimGUITextures;
import dev.simulated_team.simulated.index.SimIcons;
import dev.simulated_team.simulated.network.packets.linked_typewriter.TypewriterDisconnectUser;
import dev.simulated_team.simulated.network.packets.linked_typewriter.TypewriterKeySavePacket;
import dev.simulated_team.simulated.util.SimColors;
import foundry.veil.api.network.VeilPacketManager;
import java.util.ArrayList;
import java.util.List;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.createmod.catnip.gui.element.ScreenElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector2i;

public class LinkedTypewriterScreen extends AbstractSimiContainerScreen<LinkedTypewriterMenuCommon> {
   public final LinkedTypewriterBlockEntity clientBe;
   private final LinkedTypewriterEntries newEntries;
   private final KeyEditorScreen keyEditorScreen;
   public final EntryModifierScreen modifier;
   private List<Rect2i> extraAreasMain;
   private final List<Rect2i> emptyExtraAreas = List.of();
   protected SimGUITextures backgroundMain;
   protected SimGUITextures backgroundBind;
   private IconButton mainScreenConfirm;
   public ConfirmationWidgetBase mainScreenResetAll;
   private IconButton mainScreenEditBinding;
   private boolean confirmingReset;
   private LinkedTypewriterScreen.KeyRow firstKeyRow;
   private LinkedTypewriterScreen.KeyRow secondKeyRow;
   private LinkedTypewriterScreen.KeyRow thirdKeyRow;
   private LinkedTypewriterScreen.KeyRow fourthKeyRow;
   private LinkedTypewriterScreen.KeyRow fifthKeyRow;
   private final List<LinkedTypewriterScreen.KeyRow> allKeys = new ArrayList<>();

   public LinkedTypewriterScreen(LinkedTypewriterMenuCommon container, Inventory inv, Component title) {
      super(container, inv, title);
      this.clientBe = (LinkedTypewriterBlockEntity)container.contentHolder;
      this.newEntries = new LinkedTypewriterEntries();
      this.newEntries.addAll(this.clientBe.getTypewriterEntries().getKeyMap());
      this.keyEditorScreen = new KeyEditorScreen(this);
      this.modifier = new EntryModifierScreen(this);
   }

   protected void init() {
      this.backgroundMain = SimGUITextures.LINKED_TYPEWRITER_MAIN;
      this.backgroundBind = SimGUITextures.LINKED_TYPEWRITER_BIND;
      this.setWindowSize(this.backgroundMain.width, this.backgroundMain.height);
      super.init();
      this.rebuildExtraAreas();
      Vector2i pos = new Vector2i(145, 102);
      int spacing = 8;
      this.firstKeyRow = new LinkedTypewriterScreen.KeyRow(pos.x(), pos.y(), this.clientBe);
      this.secondKeyRow = new LinkedTypewriterScreen.KeyRow(pos.x(), pos.y() + 8, this.clientBe);
      this.thirdKeyRow = new LinkedTypewriterScreen.KeyRow(pos.x(), pos.y() + 16, this.clientBe);
      this.fourthKeyRow = new LinkedTypewriterScreen.KeyRow(pos.x(), pos.y() + 24, this.clientBe);
      this.fifthKeyRow = new LinkedTypewriterScreen.KeyRow(pos.x(), pos.y() + 32, this.clientBe);
      this.setRows();
      this.modifier.init();
      this.mainScreenResetAll = (ConfirmationWidgetBase)new ConfirmationWidgetBase(
            this.getLeftPos() + 8, this.getTopPos() + this.backgroundMain.height - 24, AllIcons.I_TRASH
         )
         .<ConfirmationWidgetBase>withMessage(SimLang.translate("linked_typewriter.confirm_delete_all").component())
         .withCallback(() -> this.sendNewKeys(true));
      this.mainScreenConfirm = (IconButton)new IconButton(
            this.getLeftPos() + this.backgroundMain.width - 33, this.getTopPos() + this.backgroundMain.height - 24, AllIcons.I_CONFIRM
         )
         .withCallback(this::onClose);
      this.mainScreenEditBinding = (IconButton)new IconButton(
            this.getLeftPos() + this.backgroundMain.width - 62, this.getTopPos() + this.backgroundMain.height - 24, SimIcons.HAMBURGER
         )
         .withCallback(() -> this.switchScreen(true));
      this.addWidget(this.mainScreenResetAll);
      this.addWidget(this.mainScreenConfirm);
      this.addWidget(this.mainScreenEditBinding);

      for (LinkedTypewriterScreen.KeyRow keyRow : this.allKeys) {
         for (KeyWidget kwid : keyRow) {
            this.addWidget(kwid);
         }
      }
   }

   protected void rebuildWidgets() {
      this.clearFocus();
      this.rescaleWindow();
      this.keyEditorScreen.resetPositions();
      this.modifier.resetXYPositions();
      this.setInitialFocus();
   }

   private void rebuildExtraAreas() {
      this.extraAreasMain = ImmutableList.of(this.getTypewriterBlockRect());
   }

   public Rect2i getTypewriterBlockRect() {
      return new Rect2i(this.leftPos + this.backgroundMain.width - 30, this.topPos + this.backgroundMain.height - 30, 94, 94);
   }

   public void rescaleWindow() {
      this.leftPos = (this.width - this.imageWidth) / 2;
      this.topPos = (this.height - this.imageHeight) / 2;
      this.backgroundMain = SimGUITextures.LINKED_TYPEWRITER_MAIN;
      this.backgroundBind = SimGUITextures.LINKED_TYPEWRITER_BIND;
      this.setWindowSize(this.backgroundMain.width, this.backgroundMain.height);
      this.rebuildExtraAreas();
      int widgetHeight = this.getTopPos() + this.backgroundMain.height - 24;
      this.mainScreenConfirm.setX(this.getLeftPos() + this.backgroundMain.width - 33);
      this.mainScreenConfirm.setY(widgetHeight);
      this.mainScreenEditBinding.setX(this.getLeftPos() + this.backgroundMain.width - 62);
      this.mainScreenEditBinding.setY(widgetHeight);
   }

   public void switchScreen(boolean subScreen) {
      this.clearWidgets();
      if (subScreen) {
         this.keyEditorScreen.startEditing();
      } else {
         this.keyEditorScreen.endEditing();

         for (LinkedTypewriterScreen.KeyRow keyRow : this.allKeys) {
            for (KeyWidget kwid : keyRow) {
               this.addWidget(kwid);
            }
         }

         this.addWidget(this.mainScreenResetAll);
         this.mainScreenResetAll.confirmation = false;
         this.mainScreenResetAll.setX(this.getLeftPos() + 8);
         this.mainScreenResetAll.setY(this.getTopPos() + this.backgroundMain.height - 24);
         this.addWidget(this.mainScreenConfirm);
         this.addWidget(this.mainScreenEditBinding);
      }
   }

   public <T extends GuiEventListener & NarratableEntry> T addWidget(T listener) {
      return (T)super.addWidget(listener);
   }

   public void removeWidget(GuiEventListener listener) {
      super.removeWidget(listener);
   }

   public void onClose() {
      if (this.modifier.psuedoEntry != null) {
         this.modifier.psuedoEntry.finishModifications();
      }

      this.sendNewKeys(false);
      LinkedTypewriterInteractionHandler.setMode(LinkedTypewriterInteractionHandler.Mode.IDLE);
      LinkedTypewriterInteractionHandler.associateTypewriter(null);
      VeilPacketManager.server().sendPacket(new CustomPacketPayload[]{new TypewriterDisconnectUser(this.clientBe.getBlockPos())});
      super.onClose();
   }

   public void sendNewKeys(boolean clearServer) {
      if (clearServer) {
         this.newEntries.clearAll();
      }

      VeilPacketManager.server().sendPacket(new CustomPacketPayload[]{new TypewriterKeySavePacket(this.newEntries, this.clientBe.getBlockPos(), clearServer)});
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (this.keyEditorScreen.active && !this.modifier.modifying) {
         this.keyEditorScreen.shiftEntries(scrollY > 0.0);
      }

      return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
   }

   protected void containerTick() {
      super.containerTick();
      if (this.keyEditorScreen.active) {
         this.keyEditorScreen.tick();
      }
   }

   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float pt) {
      PoseStack ps = guiGraphics.pose();
      ps.pushPose();
      ps.translate(0.0F, 0.0F, -1.0F);
      super.render(guiGraphics, mouseX, mouseY, pt);
      if (this.hoveredSlot != null && this.hoveredSlot.isActive() && this.hoveredSlot.hasItem()) {
         guiGraphics.renderTooltip(this.font, this.hoveredSlot.getItem(), mouseX, mouseY);
      }

      ps.pushPose();
      if (this.keyEditorScreen.active) {
         this.keyEditorScreen.render(guiGraphics, mouseX, mouseY, pt, ps);
      }

      ps.popPose();
      ps.popPose();
   }

   protected void renderForeground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      PoseStack ps = graphics.pose();
      ps.pushPose();
      if (this.modifier.modifying) {
         ps.translate(0.0F, 0.0F, 200.0F);
         this.modifier.render(graphics, mouseX, mouseY, partialTicks, ps);
      }

      ps.popPose();
   }

   protected void renderBg(GuiGraphics guiGraphics, float pt, int mx, int my) {
      int titleX = this.backgroundMain.width / 2 - Minecraft.getInstance().font.width(this.getTitle()) / 2;
      if (!this.keyEditorScreen.active) {
         this.backgroundMain.render(guiGraphics, this.getLeftPos(), this.getTopPos());
         guiGraphics.drawString(
            Minecraft.getInstance().font, this.getTitle(), this.getLeftPos() + titleX, this.getTopPos() + 4, SimColors.TITLE_DARK_RED, false
         );
         int x = this.leftPos;
         int y = this.topPos;
         int rx = x + 8;
         int ry = y + 21;
         if (!this.modifier.modifying) {
            this.renderTypeWriter(guiGraphics, x, y);
         }

         int i = 0;

         for (LinkedTypewriterScreen.KeyRow keyRow : this.allKeys) {
            keyRow.render(guiGraphics, rx, ry + i * 14, mx, my, pt, true);
            i++;
         }

         this.mainScreenResetAll.render(guiGraphics, mx, my, pt);
         this.mainScreenConfirm.render(guiGraphics, mx, my, pt);
         this.mainScreenEditBinding.render(guiGraphics, mx, my, pt);
      } else {
         this.keyEditorScreen.renderBG(guiGraphics, pt, mx, my);
      }

      if (this.modifier.modifying) {
         this.modifier.renderBG(guiGraphics);
      }
   }

   private void renderTypeWriter(GuiGraphics graphics, int x, int y) {
      PoseStack ps = graphics.pose();
      TransformStack<PoseTransformStack> msr = TransformStack.of(ps);
      ps.pushPose();
      ((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)msr.pushPose())
               .translate((float)(x + this.backgroundMain.width + 4), (float)(y + this.backgroundMain.height + 4), 100.0F)
               .scale(40.0F))
            .rotateXDegrees(-22.0F))
         .rotateYDegrees(63.0F);
      GuiGameElement.of((BlockState)this.clientBe.getBlockState().setValue(LinkedTypewriterBlock.HORIZONTAL_FACING, Direction.WEST)).render(graphics);
      msr.scale(-1.0F);
      msr.translate(-1.0F, 0.0F, -1.0F);
      msr.rotateCentered((float) (-Math.PI / 2), Direction.UP);
      float yRot = ((Direction)this.clientBe.getBlockState().getValue(LinkedTypewriterBlock.FACING)).getOpposite().toYRot();
      msr.rotateCentered((float)Math.toRadians((double)yRot), Direction.UP);
      Minecraft.getInstance().getBlockEntityRenderDispatcher().renderItem(this.clientBe, ps, graphics.bufferSource(), 255, OverlayTexture.NO_OVERLAY);
      msr.popPose();
      ps.popPose();
   }

   private void switchStates(boolean newState) {
      for (LinkedTypewriterScreen.KeyRow allKey : this.allKeys) {
         for (KeyWidget key : allKey) {
            key.setActive(newState);
         }
      }

      this.mainScreenEditBinding.setActive(newState);
      this.mainScreenResetAll.setActive(newState);
      this.mainScreenConfirm.setActive(newState);
   }

   public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
      int x = this.leftPos;
      int y = this.topPos;
      if (this.confirmingReset
         && (
            !(pMouseX > (double)(x + 8))
               || !(pMouseX < (double)(x + 26))
               || !(pMouseY > (double)(y + this.backgroundMain.height - 24))
               || !(pMouseY < (double)(y + this.backgroundMain.height - 6))
         )) {
         this.confirmingReset = false;
      }

      return super.mouseClicked(pMouseX, pMouseY, pButton);
   }

   public LinkedTypewriterEntries getNewEntries() {
      return this.newEntries;
   }

   public List<Rect2i> getExtraAreas() {
      return !this.keyEditorScreen.active && !this.modifier.modifying ? this.extraAreasMain : this.emptyExtraAreas;
   }

   public int getTopPos() {
      return this.topPos;
   }

   public int getLeftPos() {
      return this.leftPos;
   }

   public void setRows() {
      int standardLength = 14;
      this.firstKeyRow.add(14, 96, null);
      this.firstKeyRow.add(14, 49, null);
      this.firstKeyRow.add(14, 50, null);
      this.firstKeyRow.add(14, 51, null);
      this.firstKeyRow.add(14, 52, null);
      this.firstKeyRow.add(14, 53, null);
      this.firstKeyRow.add(14, 54, null);
      this.firstKeyRow.add(14, 55, null);
      this.firstKeyRow.add(14, 56, null);
      this.firstKeyRow.add(14, 57, null);
      this.firstKeyRow.add(14, 48, null);
      this.firstKeyRow.add(14, 45, null);
      this.firstKeyRow.add(14, 61, null);
      this.firstKeyRow.add(26, 259, null);
      this.firstKeyRow.add(14, 261, null);
      this.secondKeyRow.add(20, 258, null);
      this.secondKeyRow.add(14, 81, null);
      this.secondKeyRow.add(14, 87, null);
      this.secondKeyRow.add(14, 69, null);
      this.secondKeyRow.add(14, 82, null);
      this.secondKeyRow.add(14, 84, null);
      this.secondKeyRow.add(14, 89, null);
      this.secondKeyRow.add(14, 85, null);
      this.secondKeyRow.add(14, 73, null);
      this.secondKeyRow.add(14, 79, null);
      this.secondKeyRow.add(14, 80, null);
      this.secondKeyRow.add(14, 91, null);
      this.secondKeyRow.add(14, 93, null);
      this.secondKeyRow.add(20, 92, null);
      this.secondKeyRow.add(14, 266, null);
      this.thirdKeyRow.add(26, 280, null);
      this.thirdKeyRow.add(14, 65, null);
      this.thirdKeyRow.add(14, 83, null);
      this.thirdKeyRow.add(14, 68, null);
      this.thirdKeyRow.add(14, 70, null);
      this.thirdKeyRow.add(14, 71, null);
      this.thirdKeyRow.add(14, 72, null);
      this.thirdKeyRow.add(14, 74, null);
      this.thirdKeyRow.add(14, 75, null);
      this.thirdKeyRow.add(14, 76, null);
      this.thirdKeyRow.add(14, 59, null);
      this.thirdKeyRow.add(14, 39, null);
      this.thirdKeyRow.add(28, 257, null);
      this.thirdKeyRow.add(14, 267, null);
      this.fourthKeyRow.add(32, 340, null);
      this.fourthKeyRow.add(14, 90, null);
      this.fourthKeyRow.add(14, 88, null);
      this.fourthKeyRow.add(14, 67, null);
      this.fourthKeyRow.add(14, 86, null);
      this.fourthKeyRow.add(14, 66, null);
      this.fourthKeyRow.add(14, 78, null);
      this.fourthKeyRow.add(14, 77, null);
      this.fourthKeyRow.add(14, 44, null);
      this.fourthKeyRow.add(14, 46, null);
      this.fourthKeyRow.add(14, 47, null);
      this.fourthKeyRow.add(22, 344, null);
      this.fourthKeyRow.add(14, 265, SimIcons.KEY_ARROW_UP);
      this.fourthKeyRow.add(14, 269, null);
      this.fifthKeyRow.add(18, 341, null);
      this.fifthKeyRow.add(14, 343, null);
      this.fifthKeyRow.add(14, 342, null);
      this.fifthKeyRow.add(88, 32, null);
      this.fifthKeyRow.add(14, 346, null);
      this.fifthKeyRow.add(14, 348, null);
      this.fifthKeyRow.add(18, 345, null);
      this.fifthKeyRow.add(14, 263, SimIcons.KEY_ARROW_LEFT);
      this.fifthKeyRow.add(14, 264, SimIcons.KEY_ARROW_DOWN);
      this.fifthKeyRow.add(14, 262, SimIcons.KEY_ARROW_RIGHT);
      this.allKeys.clear();
      this.allKeys.add(this.firstKeyRow);
      this.allKeys.add(this.secondKeyRow);
      this.allKeys.add(this.thirdKeyRow);
      this.allKeys.add(this.fourthKeyRow);
      this.allKeys.add(this.fifthKeyRow);
   }

   private class KeyRow extends ArrayList<KeyWidget> {
      Vector2i pos;
      LinkedTypewriterBlockEntity be;

      public KeyRow(final int x, final int y, final LinkedTypewriterBlockEntity be) {
         this.pos = new Vector2i(x, y);
         this.be = be;
      }

      public void add(int length, int glfwKey, ScreenElement icon) {
         KeyWidget kWid = new KeyWidget(2, 2, length, glfwKey, icon, LinkedTypewriterScreen.this);
         kWid.withCallback(() -> {
            LinkedTypewriterScreen.this.switchStates(false);
            LinkedTypewriterScreen.this.modifier.startModifying(LinkedTypewriterScreen.this.newEntries.getEntry(glfwKey), newEntry -> {
               LinkedTypewriterScreen.this.switchStates(true);
               if (newEntry != null) {
                  LinkedTypewriterScreen.this.getNewEntries().setKey(newEntry.glfwKeyCode, newEntry);
               }
            }).keyCode(glfwKey);
         });
         this.add(kWid);
      }

      public void render(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY, float pt, boolean keyboardActive) {
         int length = 0;

         for (KeyWidget key : this) {
            key.render(guiGraphics, x + length, y, mouseX, mouseY, pt, keyboardActive);
            length += key.getWidth();
         }
      }
   }
}
