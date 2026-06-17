package net.createmod.catnip.config.ui.entries;

import com.mojang.blaze3d.platform.ClipboardManager;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import net.createmod.catnip.config.ui.ConfigAnnotations;
import net.createmod.catnip.config.ui.ConfigHelper;
import net.createmod.catnip.config.ui.ConfigScreen;
import net.createmod.catnip.config.ui.ConfigScreenList;
import net.createmod.catnip.config.ui.SubMenuConfigScreen;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.gui.element.DelegatedStencilElement;
import net.createmod.catnip.gui.widget.BoxWidget;
import net.createmod.catnip.gui.widget.ElementWidget;
import net.createmod.catnip.lang.FontHelper;
import net.createmod.ponder.enums.PonderGuiTextures;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.ValueSpec;

public class ValueEntry<T> extends ConfigScreenList.LabeledEntry {
   protected static final int resetWidth = 28;
   public static final ClipboardManager clipboardHelper = new ClipboardManager();
   protected ConfigValue<T> value;
   protected ValueSpec spec;
   protected BoxWidget resetButton;
   protected boolean editable = true;

   public ValueEntry(String label, ConfigValue<T> value, ValueSpec spec) {
      super(label);
      this.value = value;
      this.spec = spec;
      this.path = String.join(".", value.getPath());
      this.resetButton = new BoxWidget(0, 0, 16, 16).<ElementWidget>showingElement(PonderGuiTextures.ICON_CONFIG_RESET.asStencil()).withCallback(() -> {
         this.setValue((T)spec.getDefault());
         this.onReset();
      });
      this.resetButton.modifyElement(e -> ((DelegatedStencilElement)e).withElementRenderer(BoxWidget.gradientFactory.apply(this.resetButton)));
      this.listeners.add(this.resetButton);
      List<String> path = value.getPath();
      this.labelTooltip.add(Component.literal(label).withStyle(ChatFormatting.WHITE));
      String comment = spec.getComment();
      if (comment != null && !comment.isEmpty()) {
         List<String> commentLines = new ArrayList<>(Arrays.asList(comment.split("\n")));
         Pair<String, Map<String, String>> metadata = ConfigHelper.readMetadataFromComment(commentLines);
         if (metadata.getFirst() != null) {
            this.unit = metadata.getFirst();
         }

         if (metadata.getSecond() != null && !metadata.getSecond().isEmpty()) {
            this.annotations.putAll(metadata.getSecond());
         }

         this.labelTooltip
            .addAll(
               commentLines.stream()
                  .filter(s -> !s.startsWith("Range"))
                  .map(s -> s.equals(".") ? " " : s)
                  .map(str -> Component.literal(str))
                  .flatMap(stc -> FontHelper.cutTextComponent(stc, FontHelper.Palette.ALL_GRAY).stream())
                  .toList()
            );
         if (this.annotations.containsKey(ConfigAnnotations.RequiresRelog.TRUE.getName())) {
            this.labelTooltip
               .addAll(FontHelper.cutTextComponent(Component.translatable("catnip.ui.value_entry.relog_required"), FontHelper.Palette.GRAY_AND_GOLD));
         }

         if (this.annotations.containsKey(ConfigAnnotations.RequiresRestart.CLIENT.getName())) {
            this.labelTooltip
               .addAll(FontHelper.cutTextComponent(Component.translatable("catnip.ui.value_entry.restart_required"), FontHelper.Palette.GRAY_AND_RED));
         }

         this.labelTooltip.add(Component.literal(ConfigScreen.modID + ":" + path.get(path.size() - 1)).withStyle(ChatFormatting.DARK_GRAY));
      }
   }

   @Override
   protected void setEditable(boolean b) {
      this.editable = b;
      this.resetButton.active = this.editable && !this.isCurrentValueDefault();
      this.resetButton.animateGradientFromState();
   }

   @Override
   public void tick() {
      super.tick();
      this.resetButton.tick();
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (super.mouseClicked(mouseX, mouseY, button)) {
         return true;
      } else if (button != 0) {
         return false;
      } else {
         long handle = Minecraft.getInstance().getWindow().getWindow();
         if (!InputConstants.isKeyDown(handle, 341)) {
            return false;
         } else {
            Type configType = Type.CLIENT;
            if (Minecraft.getInstance().screen instanceof SubMenuConfigScreen subMenuScreen) {
               configType = subMenuScreen.type;
            }

            this.annotations.put("highlight", ":)");
            clipboardHelper.setClipboard(handle, ConfigScreen.modID + ":" + configType.extension() + "." + this.path);
            return true;
         }
      }
   }

   @Override
   public void render(GuiGraphics graphics, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean p_230432_9_, float partialTicks) {
      super.render(graphics, index, y, x, width, height, mouseX, mouseY, p_230432_9_, partialTicks);
      this.resetButton.setX(x + width - 28 + 6);
      this.resetButton.setX(y + 10);
      this.resetButton.render(graphics, mouseX, mouseY, partialTicks);
   }

   @Override
   protected int getLabelWidth(int totalWidth) {
      return (int)((float)totalWidth * 0.4F) + 30;
   }

   public void setValue(@Nonnull T value) {
      ConfigHelper.setValue(this.path, this.value, value, this.annotations);
      this.onValueChange(value);
   }

   @Nonnull
   public T getValue() {
      return ConfigHelper.getValue(this.path, this.value);
   }

   protected boolean isCurrentValueDefault() {
      return this.spec.getDefault().equals(this.getValue());
   }

   public void onReset() {
      this.onValueChange(this.getValue());
   }

   public void onValueChange() {
      this.onValueChange(this.getValue());
   }

   public void onValueChange(T newValue) {
      this.resetButton.active = this.editable && !this.isCurrentValueDefault();
      this.resetButton.animateGradientFromState();
   }

   protected void bumpCog() {
      this.bumpCog(10.0F);
   }

   protected void bumpCog(float force) {
      ConfigScreen.cogSpin.bump(3, (double)force);
   }
}
