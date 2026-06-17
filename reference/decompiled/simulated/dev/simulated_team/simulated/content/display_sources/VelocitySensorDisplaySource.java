package dev.simulated_team.simulated.content.display_sources;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import dev.simulated_team.simulated.content.blocks.velocity_sensor.VelocitySensorBlockEntity;
import dev.simulated_team.simulated.data.SimLang;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class VelocitySensorDisplaySource extends AbstractNumericDisplaysource {
   @Override
   List<Component> getOptions() {
      return SimLang.translatedOptions("display_source.velocity_sensor", "speed");
   }

   @Override
   String getKey() {
      return "velocity_sensor.data";
   }

   @Override
   String getSelectionKey() {
      return "VeclotySensorSelection";
   }

   @Override
   public int getWidth() {
      return 90;
   }

   protected MutableComponent provideLine(DisplayLinkContext displayLinkContext, DisplayTargetStats displayTargetStats) {
      if (displayLinkContext.getSourceBlockEntity() instanceof VelocitySensorBlockEntity vbe) {
         return displayLinkContext.sourceConfig().getInt(this.getSelectionKey()) == 0
            ? SimLang.number((double)Math.abs(vbe.getAdjustedVelocity())).text(" m/s").component()
            : ZERO.copy();
      } else {
         return ZERO.copy();
      }
   }
}
