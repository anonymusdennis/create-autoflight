package net.createmod.catnip.config.ui.entries;

import java.util.Locale;
import net.createmod.catnip.config.ui.ConfigScreen;
import net.createmod.catnip.gui.UIRenderHelper;
import net.createmod.catnip.gui.element.BoxElement;
import net.createmod.catnip.gui.element.DelegatedStencilElement;
import net.createmod.catnip.gui.element.RenderElement;
import net.createmod.catnip.gui.element.TextStencilElement;
import net.createmod.catnip.gui.widget.BoxWidget;
import net.createmod.catnip.gui.widget.ElementWidget;
import net.createmod.ponder.enums.PonderGuiTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.ValueSpec;

public class EnumEntry extends ValueEntry<Enum<?>> {
   protected static final int cycleWidth = 34;
   protected TextStencilElement valueText = new TextStencilElement(Minecraft.getInstance().font, "YEP").centered(true, true);
   protected BoxWidget cycleLeft;
   protected BoxWidget cycleRight;

   public EnumEntry(String label, ConfigValue<Enum<?>> value, ValueSpec spec) {
      super(label, value, spec);
      this.valueText
         .withElementRenderer(
            (ms, width, height, alpha) -> UIRenderHelper.angledGradient(ms, 0.0F, 0, height / 2, (float)height, (float)width, UIRenderHelper.COLOR_TEXT)
         );
      DelegatedStencilElement l = PonderGuiTextures.ICON_CONFIG_PREV.asStencil();
      this.cycleLeft = new BoxWidget(0, 0, 42, 16)
         .<BoxWidget>withCustomBackground(BoxElement.COLOR_BACKGROUND_FLAT)
         .<ElementWidget>showingElement(l)
         .withCallback(() -> this.cycleValue(-1));
      l.withElementRenderer(BoxWidget.gradientFactory.apply(this.cycleLeft));
      DelegatedStencilElement r = PonderGuiTextures.ICON_CONFIG_NEXT.asStencil();
      this.cycleRight = new BoxWidget(0, 0, 42, 16)
         .<BoxWidget>withCustomBackground(BoxElement.COLOR_BACKGROUND_FLAT)
         .<ElementWidget>showingElement(r)
         .withCallback(() -> this.cycleValue(1));
      r.at(26.0F, 0.0F);
      r.withElementRenderer(BoxWidget.gradientFactory.apply(this.cycleRight));
      this.listeners.add(this.cycleLeft);
      this.listeners.add(this.cycleRight);
      this.onReset();
   }

   protected void cycleValue(int direction) {
      // $VF: Couldn't be decompiled
      // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
      // java.lang.NullPointerException: Cannot invoke "org.jetbrains.java.decompiler.struct.gen.VarType.isGeneric()" because "newRet" is null
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.InvocationExprent.getInferredExprType(InvocationExprent.java:634)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.FunctionExprent.getInferredExprType(FunctionExprent.java:243)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:966)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.AssignmentExprent.toJava(AssignmentExprent.java:154)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:895)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:90)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.RootStatement.toJava(RootStatement.java:36)
      //   at org.jetbrains.java.decompiler.main.ClassWriter.writeMethod(ClassWriter.java:1283)
      //
      // Bytecode:
      // 00: aload 0
      // 01: invokevirtual net/createmod/catnip/config/ui/entries/EnumEntry.getValue ()Ljava/lang/Object;
      // 04: checkcast java/lang/Enum
      // 07: astore 2
      // 08: aload 2
      // 09: invokevirtual java/lang/Enum.getDeclaringClass ()Ljava/lang/Class;
      // 0c: invokevirtual java/lang/Class.getEnumConstants ()[Ljava/lang/Object;
      // 0f: checkcast [Ljava/lang/Enum;
      // 12: astore 3
      // 13: aload 3
      // 14: aload 2
      // 15: invokevirtual java/lang/Enum.ordinal ()I
      // 18: iload 1
      // 19: iadd
      // 1a: aload 3
      // 1b: arraylength
      // 1c: invokestatic java/lang/Math.floorMod (II)I
      // 1f: aaload
      // 20: astore 2
      // 21: aload 0
      // 22: aload 2
      // 23: invokevirtual net/createmod/catnip/config/ui/entries/EnumEntry.setValue (Ljava/lang/Object;)V
      // 26: aload 0
      // 27: iload 1
      // 28: i2f
      // 29: ldc 15.0
      // 2b: fmul
      // 2c: invokevirtual net/createmod/catnip/config/ui/entries/EnumEntry.bumpCog (F)V
      // 2f: return
   }

   @Override
   protected void setEditable(boolean b) {
      super.setEditable(b);
      this.cycleLeft.active = b;
      this.cycleLeft.animateGradientFromState();
      this.cycleRight.active = b;
      this.cycleRight.animateGradientFromState();
   }

   @Override
   public void tick() {
      super.tick();
      this.cycleLeft.tick();
      this.cycleRight.tick();
   }

   @Override
   public void render(GuiGraphics graphics, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean p_230432_9_, float partialTicks) {
      super.render(graphics, index, y, x, width, height, mouseX, mouseY, p_230432_9_, partialTicks);
      this.cycleLeft.setX(x + this.getLabelWidth(width) + 4);
      this.cycleLeft.setY(y + 10);
      this.cycleLeft.render(graphics, mouseX, mouseY, partialTicks);
      this.valueText
         .<RenderElement>at((float)(this.cycleLeft.getX() + 34 - 8), (float)(y + 10), 200.0F)
         .<RenderElement>withBounds(width - this.getLabelWidth(width) - 68 - 28 - 4, 16)
         .render(graphics);
      this.cycleRight.setX(x + width - 68 - 28 + 10);
      this.cycleRight.setY(y + 10);
      this.cycleRight.render(graphics, mouseX, mouseY, partialTicks);
      new BoxElement()
         .<BoxElement>withBackground(BoxElement.COLOR_BACKGROUND_FLAT)
         .<BoxElement>flatBorder(16777216)
         .<RenderElement>withBounds(48, 6)
         .<RenderElement>at((float)(this.cycleLeft.getX() + 22), (float)(this.cycleLeft.getY() + 5))
         .render(graphics);
   }

   public void onValueChange(Enum<?> newValue) {
      super.onValueChange(newValue);
      this.valueText.withText(ConfigScreen.toHumanReadable(newValue.name().toLowerCase(Locale.ROOT)));
   }
}
