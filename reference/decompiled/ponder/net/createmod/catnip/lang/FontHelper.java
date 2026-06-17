package net.createmod.catnip.lang;

import com.google.common.base.Strings;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.platform.CatnipClientServices;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

public final class FontHelper {
   public static final int MAX_WIDTH_PER_LINE = 200;

   private FontHelper() {
   }

   public static Style styleFromColor(ChatFormatting color) {
      return Style.EMPTY.applyFormat(color);
   }

   public static Style styleFromColor(int hex) {
      return Style.EMPTY.withColor(hex);
   }

   public static List<Component> cutStringTextComponent(String s, FontHelper.Palette palette) {
      return cutTextComponent(Component.literal(s), palette);
   }

   public static List<Component> cutTextComponent(Component c, FontHelper.Palette palette) {
      return cutTextComponent(c, palette.primary(), palette.highlight());
   }

   public static List<Component> cutStringTextComponent(String s, Style primaryStyle, Style highlightStyle) {
      return cutTextComponent(Component.literal(s), primaryStyle, highlightStyle);
   }

   public static List<Component> cutTextComponent(Component c, Style primaryStyle, Style highlightStyle) {
      return cutTextComponent(c, primaryStyle, highlightStyle, 0);
   }

   public static List<Component> cutStringTextComponent(String c, Style primaryStyle, Style highlightStyle, int indent) {
      return cutTextComponent(Component.literal(c), primaryStyle, highlightStyle, indent);
   }

   public static List<Component> cutTextComponent(Component c, Style primaryStyle, Style highlightStyle, int indent) {
      String s = c.getString();
      List<String> words = new LinkedList<>();
      BreakIterator iterator = BreakIterator.getLineInstance(CatnipClientServices.CLIENT_HOOKS.getCurrentLocale());
      iterator.setText(s);
      int start = iterator.first();

      for (int end = iterator.next(); end != -1; end = iterator.next()) {
         String word = s.substring(start, end);
         words.add(word);
         start = end;
      }

      Font font = Minecraft.getInstance().font;
      List<String> lines = new LinkedList<>();
      StringBuilder currentLine = new StringBuilder();
      int width = 0;

      for (String word : words) {
         int newWidth = font.width(word.replaceAll("_", ""));
         if (width + newWidth > 200) {
            if (width <= 0) {
               lines.add(word);
               continue;
            }

            String line = currentLine.toString();
            lines.add(line);
            currentLine = new StringBuilder();
            width = 0;
         }

         currentLine.append(word);
         width += newWidth;
      }

      if (width > 0) {
         lines.add(currentLine.toString());
      }

      MutableComponent lineStart = Component.literal(Strings.repeat(" ", indent));
      lineStart.withStyle(primaryStyle);
      List<Component> formattedLines = new ArrayList<>(lines.size());
      Couple<Style> styles = Couple.create(highlightStyle, primaryStyle);
      boolean currentlyHighlighted = false;

      for (String string : lines) {
         MutableComponent currentComponent = lineStart.plainCopy();
         String[] split = string.split("_");

         for (String part : split) {
            currentComponent.append(Component.literal(part).withStyle(styles.get(currentlyHighlighted)));
            currentlyHighlighted = !currentlyHighlighted;
         }

         formattedLines.add(currentComponent);
         currentlyHighlighted = !currentlyHighlighted;
      }

      return formattedLines;
   }

   public static record Palette(Style primary, Style highlight) {
      public static final FontHelper.Palette STANDARD_CREATE = new FontHelper.Palette(FontHelper.styleFromColor(13211468), FontHelper.styleFromColor(15850873));
      public static final FontHelper.Palette BLUE = ofColors(ChatFormatting.BLUE, ChatFormatting.AQUA);
      public static final FontHelper.Palette GREEN = ofColors(ChatFormatting.DARK_GREEN, ChatFormatting.GREEN);
      public static final FontHelper.Palette YELLOW = ofColors(ChatFormatting.GOLD, ChatFormatting.YELLOW);
      public static final FontHelper.Palette RED = ofColors(ChatFormatting.DARK_RED, ChatFormatting.RED);
      public static final FontHelper.Palette PURPLE = ofColors(ChatFormatting.DARK_PURPLE, ChatFormatting.LIGHT_PURPLE);
      public static final FontHelper.Palette GRAY = ofColors(ChatFormatting.DARK_GRAY, ChatFormatting.GRAY);
      public static final FontHelper.Palette ALL_GRAY = ofColors(ChatFormatting.GRAY, ChatFormatting.GRAY);
      public static final FontHelper.Palette GRAY_AND_BLUE = ofColors(ChatFormatting.GRAY, ChatFormatting.BLUE);
      public static final FontHelper.Palette GRAY_AND_WHITE = ofColors(ChatFormatting.GRAY, ChatFormatting.WHITE);
      public static final FontHelper.Palette GRAY_AND_GOLD = ofColors(ChatFormatting.GRAY, ChatFormatting.GOLD);
      public static final FontHelper.Palette GRAY_AND_RED = ofColors(ChatFormatting.GRAY, ChatFormatting.RED);

      public static FontHelper.Palette ofColors(ChatFormatting primary, ChatFormatting highlight) {
         return new FontHelper.Palette(FontHelper.styleFromColor(primary), FontHelper.styleFromColor(highlight));
      }
   }
}
