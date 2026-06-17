package com.simibubi.create;

import net.minecraft.advancements.critereon.ItemPredicate.Builder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.item.enchantment.effects.SetValue;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;

public class AllEnchantments {
   public static final ResourceKey<Enchantment> POTATO_RECOVERY = key("potato_recovery");
   public static final ResourceKey<Enchantment> CAPACITY = key("capacity");

   private static ResourceKey<Enchantment> key(String name) {
      return ResourceKey.create(Registries.ENCHANTMENT, Create.asResource(name));
   }

   public static void bootstrap(BootstrapContext<Enchantment> context) {
      HolderGetter<Item> itemHolderGetter = context.lookup(Registries.ITEM);
      register(
         context,
         POTATO_RECOVERY,
         Enchantment.enchantment(
               Enchantment.definition(
                  HolderSet.direct(new Holder[]{AllItems.POTATO_CANNON}),
                  10,
                  3,
                  Enchantment.dynamicCost(15, 15),
                  Enchantment.dynamicCost(45, 15),
                  1,
                  new EquipmentSlotGroup[]{EquipmentSlotGroup.MAINHAND}
               )
            )
            .withEffect(
               EnchantmentEffectComponents.AMMO_USE,
               new SetValue(LevelBasedValue.perLevel(0.0F, 33.333332F)),
               MatchTool.toolMatches(Builder.item().of(new ItemLike[0]))
            )
      );
      register(
         context,
         CAPACITY,
         Enchantment.enchantment(
            Enchantment.definition(
               itemHolderGetter.getOrThrow(AllTags.AllItemTags.PRESSURIZED_AIR_SOURCES.tag),
               10,
               3,
               Enchantment.dynamicCost(15, 15),
               Enchantment.dynamicCost(45, 15),
               1,
               new EquipmentSlotGroup[]{EquipmentSlotGroup.MAINHAND}
            )
         )
      );
   }

   private static void register(
      BootstrapContext<Enchantment> context, ResourceKey<Enchantment> key, net.minecraft.world.item.enchantment.Enchantment.Builder builder
   ) {
      context.register(key, builder.build(key.location()));
   }
}
