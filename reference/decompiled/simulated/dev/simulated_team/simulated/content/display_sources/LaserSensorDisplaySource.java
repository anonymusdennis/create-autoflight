package dev.simulated_team.simulated.content.display_sources;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.source.NumericSingleLineDisplaySource;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import dev.simulated_team.simulated.content.blocks.lasers.laser_sensor.LaserSensorBlockEntity;
import dev.simulated_team.simulated.data.SimLang;
import net.minecraft.network.chat.MutableComponent;

public class LaserSensorDisplaySource extends NumericSingleLineDisplaySource {
   protected MutableComponent provideLine(DisplayLinkContext context, DisplayTargetStats displayTargetStats) {
      if (context.getSourceBlockEntity() instanceof LaserSensorBlockEntity be) {
         return be.closestHitDistance == Double.MAX_VALUE
            ? (MutableComponent)EMPTY.getFirst()
            : SimLang.number(be.closestHitDistance).space().text("block" + (be.closestHitDistance != 1.0 ? "s" : "")).component();
      } else {
         return (MutableComponent)EMPTY.getFirst();
      }
   }

   public void initConfigurationWidgets(DisplayLinkContext context, ModularGuiLineBuilder builder, boolean isFirstLine) {
      super.initConfigurationWidgets(context, builder, isFirstLine);
      if (!isFirstLine) {
         builder.addSelectionScrollInput(
            0,
            88,
            (selectionScrollInput, label) -> selectionScrollInput.forOptions(SimLang.translatedOptions("display_source.laser_sensor", "laser_distance")),
            "LaserSensorSelection"
         );
      }
   }

   protected String getTranslationKey() {
      return "laser_sensor.data";
   }

   protected boolean allowsLabeling(DisplayLinkContext displayLinkContext) {
      return true;
   }
}
