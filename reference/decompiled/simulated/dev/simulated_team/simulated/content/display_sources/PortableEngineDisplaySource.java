package dev.simulated_team.simulated.content.display_sources;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import dev.simulated_team.simulated.content.blocks.portable_engine.PortableEngineBlockEntity;
import dev.simulated_team.simulated.data.SimLang;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class PortableEngineDisplaySource extends AbstractNumericDisplaysource {
   @Override
   List<Component> getOptions() {
      return SimLang.translatedOptions("display_source.portable_engine", "current_burn", "total_burn");
   }

   @Override
   String getKey() {
      return "portable_engine.data";
   }

   @Override
   String getSelectionKey() {
      return "PortableEngineSelection";
   }

   protected MutableComponent provideLine(DisplayLinkContext displayLinkContext, DisplayTargetStats displayTargetStats) {
      if (displayLinkContext.getSourceBlockEntity() instanceof PortableEngineBlockEntity be) {
         switch (displayLinkContext.sourceConfig().getInt(this.getSelectionKey())) {
            case 0:
               if (be.isCurrentFuelInfinite()) {
                  return SimLang.translate("portable_engine.infinite").component();
               }

               return SimLang.number((double)be.getCurrentBurnTime() / 20.0).component();
            case 1:
               if (be.isCurrentFuelInfinite()) {
                  return SimLang.translate("portable_engine.infinite").component();
               }

               return SimLang.number((double)be.getTotalBurnTime() / 20.0).component();
            default:
               return ZERO.copy();
         }
      } else {
         return ZERO.copy();
      }
   }
}
