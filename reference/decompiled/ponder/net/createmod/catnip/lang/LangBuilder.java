package net.createmod.catnip.lang;

import java.util.List;
import javax.annotation.Nullable;
import joptsimple.internal.Strings;
import net.createmod.catnip.theme.Color;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component.Serializer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

public class LangBuilder {
   String namespace;
   @Nullable
   MutableComponent component;
   public static final float DEFAULT_SPACE_WIDTH = 4.0F;

   public LangBuilder(String namespace) {
      this.namespace = namespace;
   }

   public LangBuilder space() {
      return this.text(" ");
   }

   public LangBuilder newLine() {
      return this.text("\n");
   }

   public LangBuilder translate(String langKey, Object... args) {
      Object[] args1 = resolveBuilders(args);
      return this.add(Component.translatable(this.namespace + "." + langKey, args1));
   }

   public LangBuilder text(String literalText) {
      return this.add(Component.literal(literalText));
   }

   public LangBuilder text(ChatFormatting format, String literalText) {
      return this.add(Component.literal(literalText).withStyle(format));
   }

   public LangBuilder text(int color, String literalText) {
      return this.add(Component.literal(literalText).withStyle(s -> s.withColor(color)));
   }

   public LangBuilder add(LangBuilder otherBuilder) {
      return this.add(otherBuilder.component());
   }

   public LangBuilder add(MutableComponent customComponent) {
      this.component = this.component == null ? customComponent : this.component.append(customComponent);
      return this;
   }

   public LangBuilder add(Component component) {
      return component instanceof MutableComponent mutableComponent ? this.add(mutableComponent) : this.add(component.copy());
   }

   public LangBuilder style(ChatFormatting format) {
      this.assertComponent();
      this.component = this.component.withStyle(format);
      return this;
   }

   public LangBuilder color(int color) {
      this.assertComponent();
      this.component = this.component.withStyle(s -> s.withColor(color));
      return this;
   }

   public LangBuilder color(Color color) {
      return this.color(color.getRGB());
   }

   public MutableComponent component() {
      this.assertComponent();
      return this.component;
   }

   public String string() {
      return this.component().getString();
   }

   public String json() {
      return Serializer.toJson(this.component(), RegistryAccess.EMPTY);
   }

   public void sendStatus(Player player) {
      player.displayClientMessage(this.component(), true);
   }

   public void sendChat(Player player) {
      player.displayClientMessage(this.component(), false);
   }

   public void addTo(List<? super MutableComponent> tooltip) {
      tooltip.add(this.component());
   }

   public void forGoggles(List<? super MutableComponent> tooltip) {
      this.forGoggles(tooltip, 0);
   }

   public void forGoggles(List<? super MutableComponent> tooltip, int indents) {
      tooltip.add(new LangBuilder(this.namespace).text(Strings.repeat(' ', getIndents(Minecraft.getInstance().font, 4 + indents))).add(this).component());
   }

   static int getIndents(Font font, int defaultIndents) {
      int spaceWidth = font.width(" ");
      return 4.0F == (float)spaceWidth ? defaultIndents : Mth.ceil(4.0F * (float)defaultIndents / (float)spaceWidth);
   }

   private void assertComponent() {
      if (this.component == null) {
         throw new IllegalStateException("No components were added to builder");
      }
   }

   public static Object[] resolveBuilders(Object[] args) {
      for (int i = 0; i < args.length; i++) {
         if (args[i] instanceof LangBuilder cb) {
            args[i] = cb.component();
         }
      }

      return args;
   }
}
