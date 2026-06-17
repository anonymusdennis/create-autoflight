package net.createmod.ponder.api.registration;

import java.util.function.Predicate;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

public interface IndexExclusionHelper {
   IndexExclusionHelper exclude(ItemLike var1);

   IndexExclusionHelper excludeItemVariants(Class<? extends Item> var1, Item var2);

   IndexExclusionHelper excludeBlockVariants(Class<? extends Block> var1, Block var2);

   IndexExclusionHelper exclude(Predicate<ItemLike> var1);
}
