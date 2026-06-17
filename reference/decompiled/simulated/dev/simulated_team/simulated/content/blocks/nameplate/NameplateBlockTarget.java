package dev.simulated_team.simulated.content.blocks.nameplate;

import com.simibubi.create.api.behaviour.display.DisplayTarget;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import com.simibubi.create.foundation.utility.CreateLang;
import com.tterrag.registrate.util.entry.RegistryEntry;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.api.ConditionalDisplayTarget;
import dev.simulated_team.simulated.data.SimLang;
import java.util.List;
import net.createmod.catnip.theme.Color;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class NameplateBlockTarget extends ConditionalDisplayTarget {
   public static final RegistryEntry<DisplayTarget, NameplateBlockTarget> NAMEPLATE = Simulated.getRegistrate()
      .displayTarget("nameplate", NameplateBlockTarget::new)
      .register();

   @Override
   public boolean allowsWriting(DisplayLinkContext context) {
      if (context.getTargetBlockEntity() instanceof NameplateBlockEntity nbe && nbe.waxed) {
         return true;
      }

      return false;
   }

   @Override
   public Component getErrorMessage(DisplayLinkContext context) {
      return SimLang.translate("nameplate.target.unwaxed").color(Color.RED).component();
   }

   public void acceptText(int line, List<MutableComponent> text, DisplayLinkContext context) {
      if (context.getTargetBlockEntity() instanceof NameplateBlockEntity nbe && nbe.waxed) {
         nbe.setName(text.get(0).getString(), true, null);
      }
   }

   public DisplayTargetStats provideStats(DisplayLinkContext context) {
      return new DisplayTargetStats(1, 0, this);
   }

   public Component getLineOptionText(int line) {
      return CreateLang.translateDirect("display_target.single_line", new Object[0]);
   }
}
