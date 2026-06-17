package dev.eriksonn.aeronautics.content.display_sources;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.source.NumericSingleLineDisplaySource;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import dev.eriksonn.aeronautics.content.blocks.hot_air.BlockEntityLiftingGasProvider;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.ServerBalloon;
import dev.eriksonn.aeronautics.data.AeroLang;
import joptsimple.internal.Strings;
import net.createmod.catnip.lang.LangNumberFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;

public class GasDisplaySource extends NumericSingleLineDisplaySource {
   protected MutableComponent provideLine(DisplayLinkContext displayLinkContext, DisplayTargetStats displayTargetStats) {
      if (displayLinkContext.getSourceBlockEntity() instanceof BlockEntityLiftingGasProvider provider) {
         if (provider.getBalloon() instanceof ServerBalloon info) {
            switch (displayLinkContext.sourceConfig().getInt("GasDataSelection")) {
               case 0:
                  int totalBar = 15;
                  int capacity = info.getCapacity();
                  int targetBar = (int)Math.ceil(15.0 * info.getTotalTargetVolume() / (double)capacity);
                  int volume = Mth.clamp((int)Math.ceil(15.0 * (info.getTotalFilledVolume() + info.getTotalVolumeChange()) / (double)capacity), 0, 15);
                  return barComponent(volume, targetBar, 15);
               case 1:
                  return AeroLang.text(LangNumberFormat.format(info.getTotalLift())).component();
               default:
                  return ZERO.copy();
            }
         } else {
            return noBalloon();
         }
      } else {
         return ZERO.copy();
      }
   }

   private static MutableComponent noBalloon() {
      return AeroLang.text("No Balloon above").component();
   }

   static MutableComponent barComponent(int amount, int target, int total) {
      int lower = Math.min(amount, target - 1);
      int upper = Math.max(amount - target, 0);
      char filledChar = 9608;
      char halfFillChar = 9618;
      char emptyChar = 9617;
      return Component.empty()
         .append(bars(Math.max(0, lower), ChatFormatting.DARK_AQUA, '█'))
         .append(bars(Math.max(0, target - lower - 1), ChatFormatting.DARK_GRAY, '▒'));
   }

   private static MutableComponent bars(int count, ChatFormatting format, char ch) {
      return Component.literal(Strings.repeat(ch, count)).withStyle(format);
   }

   public void initConfigurationWidgets(DisplayLinkContext context, ModularGuiLineBuilder builder, boolean isFirstLine) {
      super.initConfigurationWidgets(context, builder, isFirstLine);
      if (!isFirstLine) {
         builder.addSelectionScrollInput(
            0,
            60,
            (selectionScrollInput, label) -> selectionScrollInput.forOptions(AeroLang.translatedOptions("display_source.lifting_gas", "volume", "total_lift")),
            "GasDataSelection"
         );
      }
   }

   protected boolean allowsLabeling(DisplayLinkContext displayLinkContext) {
      return true;
   }

   protected String getTranslationKey() {
      return "lifting_gas.data";
   }
}
