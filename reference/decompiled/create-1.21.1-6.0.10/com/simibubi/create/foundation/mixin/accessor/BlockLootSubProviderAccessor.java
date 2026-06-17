package com.simibubi.create.foundation.mixin.accessor;

import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition.Builder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({BlockLootSubProvider.class})
public interface BlockLootSubProviderAccessor {
   @Invoker("hasSilkTouch")
   Builder create$hasSilkTouch();
}
