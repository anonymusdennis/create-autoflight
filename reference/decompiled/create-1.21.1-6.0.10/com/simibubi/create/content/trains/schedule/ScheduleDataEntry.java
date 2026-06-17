package com.simibubi.create.content.trains.schedule;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;

public abstract class ScheduleDataEntry implements IScheduleInput {
   protected CompoundTag data = new CompoundTag();

   @Override
   public CompoundTag getData() {
      return this.data;
   }

   @Override
   public void setData(Provider registries, CompoundTag data) {
      this.data = data;
      this.readAdditional(registries, data);
   }

   protected void writeAdditional(Provider registries, CompoundTag tag) {
   }

   protected void readAdditional(Provider registries, CompoundTag tag) {
   }

   protected <T> T enumData(String key, Class<T> enumClass) {
      T[] enumConstants = enumClass.getEnumConstants();
      return enumConstants[this.data.getInt(key) % enumConstants.length];
   }

   protected String textData(String key) {
      return this.data.getString(key);
   }

   protected int intData(String key) {
      return this.data.getInt(key);
   }
}
