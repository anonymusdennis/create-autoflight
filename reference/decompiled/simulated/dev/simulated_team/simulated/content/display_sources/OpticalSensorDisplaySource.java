package dev.simulated_team.simulated.content.display_sources;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.source.NumericSingleLineDisplaySource;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import dev.simulated_team.simulated.content.blocks.lasers.optical_sensor.OpticalSensorBlockEntity;
import dev.simulated_team.simulated.data.SimLang;
import net.minecraft.network.chat.MutableComponent;

public class OpticalSensorDisplaySource extends NumericSingleLineDisplaySource {
   protected MutableComponent provideLine(DisplayLinkContext context, DisplayTargetStats stats) {
      if (context.getSourceBlockEntity() instanceof OpticalSensorBlockEntity be) {
         switch (context.sourceConfig().getInt("OpticalSensorSelection")) {
            case 0:
               return be.hasHit() ? be.getHitBlock().getName() : SimLang.text("No Block Detected").component();
            case 1:
               if (!be.hasHit()) {
                  return SimLang.text("No Block Detected").component();
               }

               float rayDistance = be.getRayDistance();
               return SimLang.number((double)rayDistance).space().text("block" + (rayDistance != 1.0F ? "s" : "")).component();
            default:
               return ZERO.copy();
         }
      } else {
         return ZERO.copy();
      }
   }

   public void initConfigurationWidgets(DisplayLinkContext context, ModularGuiLineBuilder builder, boolean isFirstLine) {
      super.initConfigurationWidgets(context, builder, isFirstLine);
      if (!isFirstLine) {
         builder.addSelectionScrollInput(
            0,
            85,
            (selectionScrollInput, label) -> selectionScrollInput.forOptions(
                  SimLang.translatedOptions("display_source.optical_sensor", "detected_block", "block_distance")
               ),
            "OpticalSensorSelection"
         );
      }
   }

   protected String getTranslationKey() {
      return "optical_sensor.data";
   }

   protected boolean allowsLabeling(DisplayLinkContext context) {
      return true;
   }
}
