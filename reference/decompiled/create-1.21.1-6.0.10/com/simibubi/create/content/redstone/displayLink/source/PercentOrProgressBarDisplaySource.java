package com.simibubi.create.content.redstone.displayLink.source;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import com.simibubi.create.content.trains.display.FlapDisplayBlockEntity;
import com.simibubi.create.content.trains.display.FlapDisplaySection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.jetbrains.annotations.Nullable;

public abstract class PercentOrProgressBarDisplaySource extends NumericSingleLineDisplaySource {
   @Override
   protected MutableComponent provideLine(DisplayLinkContext context, DisplayTargetStats stats) {
      Float rawProgress = this.getProgress(context);
      if (rawProgress == null) {
         return EMPTY_LINE;
      } else if (!this.progressBarActive(context)) {
         return this.formatNumeric(context, rawProgress);
      } else {
         String label = context.sourceConfig().getString("Label");
         int labelSize = label.isEmpty() ? 0 : label.length() + 1;
         int length = Math.min(stats.maxColumns() - labelSize, 128);
         if (context.getTargetBlockEntity() instanceof SignBlockEntity) {
            length = (int)((float)length * 6.0F / 9.0F);
         }

         if (context.getTargetBlockEntity() instanceof FlapDisplayBlockEntity) {
            length = this.sizeForWideChars(length);
         }

         float currentLevel = Mth.clamp(rawProgress, 0.0F, 1.0F);
         int filledLength = (int)(currentLevel * (float)length);
         if (length < 1) {
            return EMPTY_LINE;
         } else {
            int emptySpaces = length - filledLength;
            String s = "█".repeat(Math.max(0, filledLength)) + "▒".repeat(Math.max(0, emptySpaces));
            return Component.literal(s);
         }
      }
   }

   protected MutableComponent formatNumeric(DisplayLinkContext context, Float currentLevel) {
      return Component.literal(Mth.clamp((int)(currentLevel * 100.0F), 0, 100) + "%");
   }

   @Nullable
   protected abstract Float getProgress(DisplayLinkContext var1);

   protected abstract boolean progressBarActive(DisplayLinkContext var1);

   @Override
   protected String getFlapDisplayLayoutName(DisplayLinkContext context) {
      return !this.progressBarActive(context) ? super.getFlapDisplayLayoutName(context) : "Progress";
   }

   @Override
   protected FlapDisplaySection createSectionForValue(DisplayLinkContext context, int size) {
      return !this.progressBarActive(context)
         ? super.createSectionForValue(context, size)
         : new FlapDisplaySection((float)size * 7.0F, "pixel", false, false).wideFlaps();
   }

   private int sizeForWideChars(int size) {
      return (int)((float)size * 7.0F / 9.0F);
   }
}
