package com.simibubi.create.content.equipment.armor;

import java.util.Locale;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ArmorItem.Type;
import net.minecraft.world.item.ArmorMaterial.Layer;
import net.minecraft.world.item.Item.Properties;
import org.jetbrains.annotations.Nullable;

public class BaseArmorItem extends ArmorItem {
   protected final ResourceLocation textureLoc;

   public BaseArmorItem(Holder<ArmorMaterial> armorMaterial, Type type, Properties properties, ResourceLocation textureLoc) {
      super(armorMaterial, type, properties.stacksTo(1));
      this.textureLoc = textureLoc;
   }

   @Nullable
   public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, Layer layer, boolean innerModel) {
      return ResourceLocation.parse(
         String.format(
            Locale.ROOT,
            "%s:textures/models/armor/%s_layer_%d.png",
            this.textureLoc.getNamespace(),
            this.textureLoc.getPath(),
            slot == EquipmentSlot.LEGS ? 2 : 1
         )
      );
   }
}
