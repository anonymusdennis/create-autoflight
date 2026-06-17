package com.simibubi.create.foundation.mixin.accessor;

import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionBrewing.Mix;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({PotionBrewing.class})
public interface PotionBrewingAccessor {
   @Accessor("potionMixes")
   List<Mix<Potion>> create$getPotionMixes();

   @Accessor("containerMixes")
   List<Mix<Item>> create$getContainerMixes();

   @Invoker("isContainer")
   boolean create$isContainer(ItemStack var1);
}
