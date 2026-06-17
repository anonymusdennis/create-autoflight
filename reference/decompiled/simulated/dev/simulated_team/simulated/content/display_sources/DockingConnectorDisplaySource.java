package dev.simulated_team.simulated.content.display_sources;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.source.SingleLineDisplaySource;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.content.blocks.docking_connector.DockingConnectorBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class DockingConnectorDisplaySource extends SingleLineDisplaySource {
   protected MutableComponent provideLine(DisplayLinkContext context, DisplayTargetStats stats) {
      if (context.getSourceBlockEntity() instanceof DockingConnectorBlockEntity be) {
         DockingConnectorBlockEntity otherConnector = be.getOtherConnector();
         if (otherConnector != null) {
            SubLevel otherSubLevel = Sable.HELPER.getContaining(otherConnector);
            if (otherSubLevel != null) {
               String name = otherSubLevel.getName();
               return name != null ? Component.literal(name) : EMPTY_LINE.copy();
            }
         }

         return EMPTY_LINE.copy();
      } else {
         return EMPTY_LINE.copy();
      }
   }

   public void initConfigurationWidgets(DisplayLinkContext context, ModularGuiLineBuilder builder, boolean isFirstLine) {
      super.initConfigurationWidgets(context, builder, isFirstLine);
   }

   protected String getTranslationKey() {
      return "sublevel_name";
   }

   protected boolean allowsLabeling(DisplayLinkContext context) {
      return true;
   }
}
