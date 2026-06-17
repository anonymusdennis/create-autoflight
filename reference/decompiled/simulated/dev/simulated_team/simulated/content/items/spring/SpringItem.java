package dev.simulated_team.simulated.content.items.spring;

import dev.simulated_team.simulated.index.SimClickInteractions;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;

public class SpringItem extends Item {
   public SpringItem(Properties pProperties) {
      super(pProperties);
   }

   public InteractionResult useOn(UseOnContext context) {
      return context.getPlayer().isLocalPlayer() && SimClickInteractions.SPRING_INTERACTION.tryStartPlacement(context)
         ? InteractionResult.SUCCESS
         : super.useOn(context);
   }
}
