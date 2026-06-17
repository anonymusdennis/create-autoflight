package dev.simulated_team.simulated.content.entities.diagram.screen;

import dev.simulated_team.simulated.index.SimGUITextures;
import dev.simulated_team.simulated.index.SimSoundEvents;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;

public class DiagramButton extends AbstractWidget {
   private SimGUITextures texture;
   private final Runnable onClick;
   private Supplier<Component> diagramTooltip;
   private BooleanSupplier iconSwitch;

   public DiagramButton(SimGUITextures texture, int x, int y, Component message, Runnable onClick) {
      super(x, y, texture.width, texture.height, message);
      this.texture = texture;
      this.onClick = onClick;
      this.iconSwitch = this::isHovered;
   }

   public void onClick(double mouseX, double mouseY) {
      super.onClick(mouseX, mouseY);
      this.onClick.run();
   }

   public void setTexture(SimGUITextures texture) {
      this.texture = texture;
   }

   public SimGUITextures getTexture() {
      return this.texture;
   }

   protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
      this.texture
         .render(
            guiGraphics,
            this.getX() - 1,
            this.getY() - 1,
            !this.isHovered() && !this.iconSwitch.getAsBoolean() ? DiagramScreen.DULL_BUTTON_COLOR : DiagramScreen.BUTTON_COLOR
         );
      if (this.diagramTooltip != null && this.isHovered()) {
         List<FormattedText> lines = List.of((FormattedText)this.diagramTooltip.get());
         DiagramScreen.renderTooltip(guiGraphics, mouseX, mouseY, lines);
      }
   }

   public void playDownSound(SoundManager handler) {
      handler.play(SimpleSoundInstance.forUI(SimSoundEvents.DIAGRAM_TAP.event(), 1.0F));
   }

   protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
   }

   public DiagramButton setDiagramTooltip(Supplier<Component> diagramTooltip) {
      this.diagramTooltip = diagramTooltip;
      return this;
   }

   public DiagramButton setIconSwitch(BooleanSupplier switcher) {
      this.iconSwitch = switcher;
      return this;
   }
}
