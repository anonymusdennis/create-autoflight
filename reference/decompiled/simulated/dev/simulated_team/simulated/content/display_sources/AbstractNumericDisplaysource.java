package dev.simulated_team.simulated.content.display_sources;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.source.NumericSingleLineDisplaySource;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import java.util.List;
import net.minecraft.network.chat.Component;

public abstract class AbstractNumericDisplaysource extends NumericSingleLineDisplaySource {
   public void initConfigurationWidgets(DisplayLinkContext context, ModularGuiLineBuilder builder, boolean isFirstLine) {
      super.initConfigurationWidgets(context, builder, isFirstLine);
      if (!isFirstLine) {
         builder.addSelectionScrollInput(
            0, this.getWidth(), (selectionScrollInput, label) -> selectionScrollInput.forOptions(this.getOptions()), this.getSelectionKey()
         );
      }
   }

   public int getWidth() {
      return 100;
   }

   abstract List<Component> getOptions();

   abstract String getKey();

   abstract String getSelectionKey();

   protected String getTranslationKey() {
      return this.getKey();
   }

   protected boolean allowsLabeling(DisplayLinkContext displayLinkContext) {
      return true;
   }
}
