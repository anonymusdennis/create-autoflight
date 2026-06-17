package net.createmod.ponder.foundation.registration;

import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;
import net.createmod.ponder.api.registration.IndexExclusionHelper;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

public class PonderIndexExclusionHelper implements IndexExclusionHelper {
   private final Builder<Predicate<ItemLike>> exclusions = Stream.builder();

   public static Stream<Predicate<ItemLike>> pluginToExclusions(PonderPlugin plugin) {
      PonderIndexExclusionHelper helper = new PonderIndexExclusionHelper();
      plugin.indexExclusions(helper);
      return helper.getExclusions();
   }

   public Stream<Predicate<ItemLike>> getExclusions() {
      return this.exclusions.build();
   }

   @Override
   public IndexExclusionHelper exclude(ItemLike item) {
      this.exclusions.add(itemLike -> itemLike.asItem() == item.asItem());
      return this;
   }

   @Override
   public IndexExclusionHelper excludeItemVariants(Class<? extends Item> itemClazz, Item originalVariant) {
      this.exclusions.add(itemLike -> !itemClazz.isInstance(itemLike) ? false : itemLike.asItem() != originalVariant.asItem());
      return this;
   }

   @Override
   public IndexExclusionHelper excludeBlockVariants(Class<? extends Block> blockClazz, Block originalVariant) {
      this.exclusions.add(itemLike -> {
         if (itemLike instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            return !blockClazz.isInstance(block) ? false : block.asItem() != originalVariant.asItem();
         } else {
            return false;
         }
      });
      return this;
   }

   @Override
   public IndexExclusionHelper exclude(Predicate<ItemLike> predicate) {
      this.exclusions.add(predicate);
      return this;
   }
}
