package dev.simulated_team.simulated.content.display_sources;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.source.SingleLineDisplaySource;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class LinkedTypewriterDisplaySource extends SingleLineDisplaySource {
   protected MutableComponent provideLine(DisplayLinkContext context, DisplayTargetStats stats) {
      return context.getSourceBlockEntity() instanceof LinkedTypewriterBlockEntity be ? Component.literal(be.getTypedEntry()) : EMPTY_LINE.copy();
   }

   protected String getTranslationKey() {
      return "typewriter.typed_text";
   }

   protected boolean allowsLabeling(DisplayLinkContext context) {
      return true;
   }
}
