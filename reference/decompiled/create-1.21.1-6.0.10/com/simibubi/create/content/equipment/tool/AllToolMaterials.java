package com.simibubi.create.content.equipment.tool;

import com.simibubi.create.AllItems;
import com.simibubi.create.Create;
import java.util.function.Supplier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

public enum AllToolMaterials implements Tier {
   CARDBOARD(Create.asResource("cardboard").toString(), 0, 1.0F, 2.0F, 1, () -> Ingredient.of(new ItemLike[]{AllItems.CARDBOARD.asItem()}));

   public final String name;
   private final int uses;
   private final float speed;
   private final float damageBonus;
   private final int enchantValue;
   private final Supplier<Ingredient> repairMaterial;

   private AllToolMaterials(String name, int uses, float speed, float damageBonus, int enchantValue, Supplier<Ingredient> repairMaterial) {
      this.name = name;
      this.uses = uses;
      this.speed = speed;
      this.damageBonus = damageBonus;
      this.enchantValue = enchantValue;
      this.repairMaterial = repairMaterial;
   }

   public int getUses() {
      return this.uses;
   }

   public float getSpeed() {
      return this.speed;
   }

   public float getAttackDamageBonus() {
      return this.damageBonus;
   }

   @NotNull
   public TagKey<Block> getIncorrectBlocksForDrops() {
      return BlockTags.INCORRECT_FOR_WOODEN_TOOL;
   }

   public int getEnchantmentValue() {
      return this.enchantValue;
   }

   @NotNull
   public Ingredient getRepairIngredient() {
      return this.repairMaterial.get();
   }
}
