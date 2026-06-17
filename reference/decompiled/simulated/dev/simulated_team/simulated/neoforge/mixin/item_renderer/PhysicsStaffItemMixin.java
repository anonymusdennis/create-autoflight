package dev.simulated_team.simulated.neoforge.mixin.item_renderer;

import com.simibubi.create.foundation.item.render.SimpleCustomRenderer;
import dev.simulated_team.simulated.content.physics_staff.PhysicsStaffItem;
import dev.simulated_team.simulated.content.physics_staff.PhysicsStaffItemRenderer;
import java.util.function.Consumer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({PhysicsStaffItem.class})
public abstract class PhysicsStaffItemMixin extends Item {
   public PhysicsStaffItemMixin(Properties properties) {
      super(properties);
   }

   @OnlyIn(Dist.CLIENT)
   public void initializeClient(Consumer<IClientItemExtensions> consumer) {
      consumer.accept(SimpleCustomRenderer.create(this, new PhysicsStaffItemRenderer()));
   }
}
