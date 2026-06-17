package dev.simulated_team.simulated.content.display_sources;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import dev.simulated_team.simulated.content.blocks.gimbal_sensor.GimbalSensorBlockEntity;
import dev.simulated_team.simulated.data.SimLang;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class GimbalSensorDisplaySource extends AbstractNumericDisplaysource {
   @Override
   List<Component> getOptions() {
      return SimLang.translatedOptions("display_source.gimbal_sensor", "x_angle", "z_angle");
   }

   @Override
   String getKey() {
      return "gimbal_sensor.data";
   }

   @Override
   String getSelectionKey() {
      return "GimbalSensorSelection";
   }

   @Override
   public int getWidth() {
      return 50;
   }

   public MutableComponent provideLine(DisplayLinkContext displayLinkContext, DisplayTargetStats displayTargetStats) {
      if (displayLinkContext.getSourceBlockEntity() instanceof GimbalSensorBlockEntity be) {
         switch (displayLinkContext.sourceConfig().getInt(this.getSelectionKey())) {
            case 0:
               return SimLang.number(Math.toDegrees(be.getXAngle())).component();
            case 1:
               return SimLang.number(Math.toDegrees(be.getZAngle())).component();
            default:
               return ZERO.copy();
         }
      } else {
         return ZERO.copy();
      }
   }
}
