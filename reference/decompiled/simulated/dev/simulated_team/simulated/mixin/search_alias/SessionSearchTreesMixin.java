package dev.simulated_team.simulated.mixin.search_alias;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.simulated_team.simulated.client.SearchAlias;
import java.util.List;
import net.minecraft.client.multiplayer.SessionSearchTrees;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({SessionSearchTrees.class})
public class SessionSearchTreesMixin {
   @WrapOperation(
      method = {"*"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/item/ItemStack;getTooltipLines(Lnet/minecraft/world/item/Item$TooltipContext;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/TooltipFlag;)Ljava/util/List;"
      )}
   )
   private static List<Component> simulated$getTooltipLines(
      ItemStack instance, TooltipContext i, Player list, TooltipFlag mutablecomponent, Operation<List<Component>> original
   ) {
      List<Component> tooltipLines = (List<Component>)original.call(new Object[]{instance, i, list, mutablecomponent});
      tooltipLines.addAll(SearchAlias.getAliases(instance).stream().map(Component::literal).toList());
      return tooltipLines;
   }
}
