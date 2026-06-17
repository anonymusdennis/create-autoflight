package dev.simulated_team.simulated.content.display_sources;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.source.SingleLineDisplaySource;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import dev.simulated_team.simulated.content.blocks.nav_table.NavTableBlockEntity;
import dev.simulated_team.simulated.content.blocks.nav_table.navigation_target.NavigationTarget;
import dev.simulated_team.simulated.data.SimLang;
import java.time.Duration;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class NavigationTableDisplaySource extends SingleLineDisplaySource {
   protected MutableComponent provideLine(DisplayLinkContext context, DisplayTargetStats stats) {
      if (context.getSourceBlockEntity() instanceof NavTableBlockEntity be) {
         switch (context.sourceConfig().getInt("NavTableSelection")) {
            case 0:
               NavigationTarget navigationTarget = be.getNavTableItem();
               if (navigationTarget == null) {
                  return EMPTY_LINE.copy();
               }

               int distancex = (int)navigationTarget.distanceToTarget(be);
               return Component.literal(String.valueOf(distancex));
            case 1:
               double distance = be.distanceToTarget();
               double lastDistance = be.lastDistanceToTarget();
               double change = lastDistance - distance;
               double speed = change / 0.5;
               int totalSeconds = (int)(distance / speed);
               Duration duration = Duration.ofSeconds((long)totalSeconds);
               String eta = "%2s:%2s".formatted(duration.toMinutesPart(), duration.toSecondsPart());
               if (duration.toHoursPart() > 0) {
                  eta = "%2s:".formatted(duration.toHoursPart()) + eta;
               }

               if (totalSeconds >= 0 && !(change < 0.001)) {
                  return Component.literal(eta.replace(' ', '0'));
               }

               return Component.literal("N/A");
            default:
               return EMPTY_LINE.copy();
         }
      } else {
         return EMPTY_LINE.copy();
      }
   }

   public void initConfigurationWidgets(DisplayLinkContext context, ModularGuiLineBuilder builder, boolean isFirstLine) {
      super.initConfigurationWidgets(context, builder, isFirstLine);
      if (!isFirstLine) {
         builder.addSelectionScrollInput(
            0,
            95,
            (selectionScrollInput, label) -> selectionScrollInput.forOptions(
                  SimLang.translatedOptions("display_source.navigation_table", "distance", "eta_real")
               ),
            "NavTableSelection"
         );
      }
   }

   protected String getTranslationKey() {
      return "navigation_table.data";
   }

   protected boolean allowsLabeling(DisplayLinkContext context) {
      return true;
   }
}
