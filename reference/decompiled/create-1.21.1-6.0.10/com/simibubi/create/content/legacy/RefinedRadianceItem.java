package com.simibubi.create.content.legacy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;

public class RefinedRadianceItem extends NoGravMagicalDohickyItem {
   public RefinedRadianceItem(Properties properties) {
      super(properties);
   }

   public boolean isFoil(ItemStack stack) {
      return true;
   }

   @Override
   protected void onCreated(ItemEntity entity, CompoundTag persistentData) {
      super.onCreated(entity, persistentData);
      entity.setDeltaMovement(entity.getDeltaMovement().add(0.0, 0.25, 0.0));
   }
}
