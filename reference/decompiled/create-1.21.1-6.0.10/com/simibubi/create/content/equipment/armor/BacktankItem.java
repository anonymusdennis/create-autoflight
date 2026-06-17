package com.simibubi.create.content.equipment.armor;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.foundation.item.LayeredArmorItem;
import java.util.Locale;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ArmorItem.Type;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

public class BacktankItem extends BaseArmorItem {
   public static final EquipmentSlot SLOT = EquipmentSlot.CHEST;
   public static final Type TYPE = Type.CHESTPLATE;
   public static final int BAR_COLOR = 15724527;
   private final Supplier<BacktankItem.BacktankBlockItem> blockItem;

   public BacktankItem(Holder<ArmorMaterial> material, Properties properties, ResourceLocation textureLoc, Supplier<BacktankItem.BacktankBlockItem> placeable) {
      super(material, TYPE, properties, textureLoc);
      this.blockItem = placeable;
   }

   @Nullable
   public static BacktankItem getWornBy(Entity entity) {
      if (entity instanceof LivingEntity livingEntity) {
         Item var3 = livingEntity.getItemBySlot(SLOT).getItem();
         return var3 instanceof BacktankItem ? (BacktankItem)var3 : null;
      } else {
         return null;
      }
   }

   public InteractionResult useOn(UseOnContext ctx) {
      return this.blockItem.get().useOn(ctx);
   }

   public boolean isEnchantable(ItemStack p_77616_1_) {
      return true;
   }

   public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
      return !enchantment.is(Enchantments.MENDING) && !enchantment.is(Enchantments.UNBREAKING) ? super.supportsEnchantment(stack, enchantment) : false;
   }

   public boolean isBarVisible(ItemStack stack) {
      return true;
   }

   public int getBarWidth(ItemStack stack) {
      return Math.round(13.0F * Mth.clamp((float)getRemainingAir(stack) / (float)BacktankUtil.maxAir(stack), 0.0F, 1.0F));
   }

   public int getBarColor(ItemStack stack) {
      return 15724527;
   }

   public Block getBlock() {
      return this.blockItem.get().getBlock();
   }

   public static int getRemainingAir(ItemStack stack) {
      return (Integer)stack.getOrDefault(AllDataComponents.BACKTANK_AIR, 0);
   }

   public static class BacktankBlockItem extends BlockItem {
      private final Supplier<Item> actualItem;

      public BacktankBlockItem(Block block, Supplier<Item> actualItem, Properties properties) {
         super(block, properties);
         this.actualItem = actualItem;
      }

      public String getDescriptionId() {
         return this.getOrCreateDescriptionId();
      }

      public Item getActualItem() {
         return this.actualItem.get();
      }
   }

   public static class Layered extends BacktankItem implements LayeredArmorItem {
      public Layered(Holder<ArmorMaterial> material, Properties properties, ResourceLocation textureLoc, Supplier<BacktankItem.BacktankBlockItem> placeable) {
         super(material, properties, textureLoc, placeable);
      }

      @Override
      public String getArmorTextureLocation(LivingEntity entity, EquipmentSlot slot, ItemStack stack, int layer) {
         return String.format(Locale.ROOT, "%s:textures/models/armor/%s_layer_%d.png", this.textureLoc.getNamespace(), this.textureLoc.getPath(), layer);
      }
   }
}
