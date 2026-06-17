package net.createmod.ponder.api.registration;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;

public interface TagBuilder {
   TagBuilder title(String var1);

   TagBuilder description(String var1);

   TagBuilder addToIndex();

   TagBuilder icon(ResourceLocation var1);

   TagBuilder icon(String var1);

   TagBuilder idAsIcon();

   TagBuilder item(ItemLike var1, boolean var2, boolean var3);

   default TagBuilder item(ItemLike item) {
      return this.item(item, true, true);
   }

   void register();
}
