package dev.simulated_team.simulated.neoforge.mixin.self_mixins;

import com.simibubi.create.foundation.item.render.SimpleCustomRenderer;
import dev.simulated_team.simulated.content.items.plunger_launcher.PlungerLauncherItem;
import dev.simulated_team.simulated.content.items.plunger_launcher.PlungerLauncherItemRenderer;
import java.util.function.Consumer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({PlungerLauncherItem.class})
public abstract class PlungerLauncherItemMixin extends Item {
   public PlungerLauncherItemMixin(Properties properties) {
      super(properties);
   }

   public boolean onEntitySwing(ItemStack stack, LivingEntity entity, InteractionHand hand) {
      return true;
   }

   @OnlyIn(Dist.CLIENT)
   public void initializeClient(Consumer<IClientItemExtensions> consumer) {
      consumer.accept(SimpleCustomRenderer.create(this, new PlungerLauncherItemRenderer()));
   }
}
