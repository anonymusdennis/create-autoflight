package com.simibubi.create.content.legacy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item.Properties;

public class ShadowSteelItem extends NoGravMagicalDohickyItem {
   public ShadowSteelItem(Properties properties) {
      super(properties);
   }

   @Override
   protected void onCreated(ItemEntity entity, CompoundTag persistentData) {
      super.onCreated(entity, persistentData);
      float yMotion = (entity.fallDistance + 3.0F) / 50.0F;
      entity.setDeltaMovement(0.0, (double)yMotion, 0.0);
   }

   @Override
   protected float getIdleParticleChance(ItemEntity entity) {
      return (float)(Mth.clamp((double)(entity.getItem().getCount() - 10), Mth.clamp(entity.getDeltaMovement().y * 20.0, 5.0, 20.0), 100.0) / 64.0);
   }
}
