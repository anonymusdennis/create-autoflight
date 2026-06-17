package net.createmod.catnip.lang;

import com.mojang.blaze3d.vertex.PoseStack;
import java.text.BreakIterator;
import java.util.LinkedList;
import java.util.List;
import net.createmod.catnip.platform.CatnipClientServices;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import org.joml.Matrix4f;

public class ClientFontHelper {
   public static List<String> cutString(Font font, String text, int maxWidthPerLine) {
      List<String> words = new LinkedList<>();
      BreakIterator iterator = BreakIterator.getLineInstance(CatnipClientServices.CLIENT_HOOKS.getCurrentLocale());
      iterator.setText(text);
      int start = iterator.first();

      for (int end = iterator.next(); end != -1; end = iterator.next()) {
         String word = text.substring(start, end);
         words.add(word);
         start = end;
      }

      List<String> lines = new LinkedList<>();
      StringBuilder currentLine = new StringBuilder();
      int width = 0;

      for (String word : words) {
         int newWidth = font.width(word);
         if (width + newWidth > maxWidthPerLine) {
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

      return lines;
   }

   public static void drawSplitString(GuiGraphics graphics, PoseStack ms, Font font, String text, int x, int y, int width, int color) {
      List<String> list = cutString(font, text, width);
      Matrix4f matrix4f = ms.last().pose();

      for (String s : list) {
         float f = (float)x;
         if (font.isBidirectional()) {
            int i = font.width(font.bidirectionalShaping(s));
            f += (float)(width - i);
         }

         draw(graphics, font, s, f, (float)y, color, matrix4f, false);
         y += 9;
      }
   }

   private static int draw(
      GuiGraphics graphics, Font font, String p_228078_1_, float p_228078_2_, float p_228078_3_, int p_228078_4_, Matrix4f p_228078_5_, boolean p_228078_6_
   ) {
      if (p_228078_1_ == null) {
         return 0;
      } else {
         BufferSource irendertypebuffer$impl = graphics.bufferSource();
         int i = font.drawInBatch(
            p_228078_1_, p_228078_2_, p_228078_3_, p_228078_4_, p_228078_6_, p_228078_5_, irendertypebuffer$impl, DisplayMode.NORMAL, 0, 15728880
         );
         irendertypebuffer$impl.endBatch();
         return i;
      }
   }
}
