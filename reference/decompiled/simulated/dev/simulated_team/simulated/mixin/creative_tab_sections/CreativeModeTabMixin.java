package dev.simulated_team.simulated.mixin.creative_tab_sections;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.simulated_team.simulated.registrate.simulated_tab.SimulatedCreativeTab;
import dev.simulated_team.simulated.service.SimTabService;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin({CreativeModeTab.class})
public class CreativeModeTabMixin {
   @Shadow
   private Collection<ItemStack> displayItems;
   @Shadow
   private Set<ItemStack> displayItemsSearchTab;

   @WrapMethod(
      method = {"buildContents"}
   )
   private void simulated$buildContents(ItemDisplayParameters parameters, Operation<Void> original) {
      CreativeModeTab self = (CreativeModeTab)this;
      if (self == SimTabService.INSTANCE.getCreativeTab()) {
         List<ItemStack> displayItems = new LinkedList<>();
         Set<ItemStack> searchItems = new LinkedHashSet<>();
         SimulatedCreativeTab.processItems(displayItems::add, searchItems::add);
         this.displayItems = displayItems;
         this.displayItemsSearchTab = searchItems;
      } else {
         original.call(new Object[]{parameters});
      }
   }
}
