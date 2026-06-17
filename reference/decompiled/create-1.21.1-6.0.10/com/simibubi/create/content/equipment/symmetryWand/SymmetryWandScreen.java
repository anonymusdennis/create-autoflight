package com.simibubi.create.content.equipment.symmetryWand;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.equipment.symmetryWand.mirror.CrossPlaneMirror;
import com.simibubi.create.content.equipment.symmetryWand.mirror.EmptyMirror;
import com.simibubi.create.content.equipment.symmetryWand.mirror.PlaneMirror;
import com.simibubi.create.content.equipment.symmetryWand.mirror.SymmetryMirror;
import com.simibubi.create.content.equipment.symmetryWand.mirror.TriplePlaneMirror;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.Label;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import com.simibubi.create.foundation.gui.widget.SelectionScrollInput;
import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.gui.AbstractSimiScreen;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class SymmetryWandScreen extends AbstractSimiScreen {
   private AllGuiTextures background;
   private ScrollInput areaType;
   private Label labelType;
   private ScrollInput areaAlign;
   private Label labelAlign;
   private IconButton confirmButton;
   private final Component mirrorType = CreateLang.translateDirect("gui.symmetryWand.mirrorType");
   private final Component orientation = CreateLang.translateDirect("gui.symmetryWand.orientation");
   private SymmetryMirror currentElement;
   private ItemStack wand;
   private InteractionHand hand;

   public SymmetryWandScreen(ItemStack wand, InteractionHand hand) {
      this.background = AllGuiTextures.WAND_OF_SYMMETRY;
      this.currentElement = SymmetryWandItem.getMirror(wand);
      if (this.currentElement instanceof EmptyMirror) {
         this.currentElement = new PlaneMirror(Vec3.ZERO);
      }

      this.hand = hand;
      this.wand = wand;
   }

   public void init() {
      this.setWindowSize(this.background.getWidth(), this.background.getHeight());
      this.setWindowOffset(-20, 0);
      super.init();
      int x = this.guiLeft;
      int y = this.guiTop;
      this.labelType = new Label(x + 51, y + 28, CommonComponents.EMPTY).colored(-1).withShadow();
      this.labelAlign = new Label(x + 51, y + 50, CommonComponents.EMPTY).colored(-1).withShadow();
      int state = this.currentElement instanceof TriplePlaneMirror ? 2 : (this.currentElement instanceof CrossPlaneMirror ? 1 : 0);
      this.areaType = new SelectionScrollInput(x + 45, y + 21, 109, 18)
         .forOptions(SymmetryMirror.getMirrors())
         .titled(this.mirrorType.plainCopy())
         .writingTo(this.labelType)
         .setState(state);
      this.areaType.calling(position -> {
         switch (position) {
            case 0:
               this.currentElement = new PlaneMirror(this.currentElement.getPosition());
               break;
            case 1:
               this.currentElement = new CrossPlaneMirror(this.currentElement.getPosition());
               break;
            case 2:
               this.currentElement = new TriplePlaneMirror(this.currentElement.getPosition());
         }

         this.initAlign(this.currentElement, x, y);
      });
      this.initAlign(this.currentElement, x, y);
      this.addRenderableWidget(this.labelAlign);
      this.addRenderableWidget(this.areaType);
      this.addRenderableWidget(this.labelType);
      this.confirmButton = new IconButton(x + this.background.getWidth() - 33, y + this.background.getHeight() - 24, AllIcons.I_CONFIRM);
      this.confirmButton.withCallback(() -> this.onClose());
      this.addRenderableWidget(this.confirmButton);
   }

   private void initAlign(SymmetryMirror element, int x, int y) {
      if (this.areaAlign != null) {
         this.removeWidget(this.areaAlign);
      }

      this.areaAlign = new SelectionScrollInput(x + 45, y + 43, 109, 18)
         .forOptions(element.getAlignToolTips())
         .titled(this.orientation.plainCopy())
         .writingTo(this.labelAlign)
         .setState(element.getOrientationIndex())
         .calling(element::setOrientation);
      this.addRenderableWidget(this.areaAlign);
   }

   protected void renderWindow(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      int x = this.guiLeft;
      int y = this.guiTop;
      this.background.render(graphics, x, y);
      graphics.drawString(
         this.font, this.wand.getHoverName(), x + (this.background.getWidth() - this.font.width(this.wand.getHoverName())) / 2, y + 4, 5841956, false
      );
      this.renderBlock(graphics, x, y);
      GuiGameElement.of(this.wand).scale(4.0).rotate(-70.0, 20.0, 20.0).at((float)(x + 178), (float)(y + 448), -150.0F).render(graphics);
   }

   protected void renderBlock(GuiGraphics graphics, int x, int y) {
      PoseStack ms = graphics.pose();
      ms.pushPose();
      ms.translate((float)(x + 26), (float)(y + 39), 20.0F);
      ms.scale(16.0F, 16.0F, 16.0F);
      ms.mulPose(Axis.of(new Vector3f(0.3F, 1.0F, 0.0F)).rotationDegrees(-22.5F));
      this.currentElement.applyModelTransform(ms);
      GuiGameElement.of(this.currentElement.getModel()).render(graphics);
      ms.popPose();
   }

   public void removed() {
      SymmetryWandItem.configureSettings(this.wand, this.currentElement);
      CatnipServices.NETWORK.sendToServer(new ConfigureSymmetryWandPacket(this.hand, this.currentElement));
   }
}
