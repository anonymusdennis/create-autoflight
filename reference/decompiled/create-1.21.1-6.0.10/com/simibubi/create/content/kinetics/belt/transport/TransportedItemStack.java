package com.simibubi.create.content.kinetics.belt.transport;

import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import com.simibubi.create.content.kinetics.belt.BeltHelper;
import com.simibubi.create.content.kinetics.fan.processing.AllFanProcessingTypes;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import com.simibubi.create.content.logistics.box.PackageItem;
import java.util.Random;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class TransportedItemStack implements Comparable<TransportedItemStack> {
   private static Random R = new Random();
   public ItemStack stack;
   public float beltPosition;
   public float sideOffset;
   public int angle;
   public int insertedAt;
   public Direction insertedFrom;
   public boolean locked;
   public boolean lockedExternally;
   public float prevBeltPosition;
   public float prevSideOffset;
   public FanProcessingType processedBy;
   public int processingTime;

   public TransportedItemStack(ItemStack stack) {
      this.stack = stack;
      boolean centered = BeltHelper.isItemUpright(stack);
      this.angle = centered ? 180 : R.nextInt(360);
      if (PackageItem.isPackage(stack)) {
         this.angle = R.nextInt(4) * 90 + R.nextInt(20) - 10;
      }

      this.sideOffset = this.prevSideOffset = this.getTargetSideOffset();
      this.insertedFrom = Direction.UP;
   }

   public float getTargetSideOffset() {
      return (float)(this.angle - 180) / 1080.0F;
   }

   public int compareTo(TransportedItemStack o) {
      return this.beltPosition < o.beltPosition ? 1 : (this.beltPosition > o.beltPosition ? -1 : 0);
   }

   public TransportedItemStack getSimilar() {
      TransportedItemStack copy = new TransportedItemStack(this.stack.copy());
      copy.beltPosition = this.beltPosition;
      copy.insertedAt = this.insertedAt;
      copy.insertedFrom = this.insertedFrom;
      copy.prevBeltPosition = this.prevBeltPosition;
      copy.prevSideOffset = this.prevSideOffset;
      copy.processedBy = this.processedBy;
      copy.processingTime = this.processingTime;
      return copy;
   }

   public TransportedItemStack copy() {
      TransportedItemStack copy = this.getSimilar();
      copy.angle = this.angle;
      copy.sideOffset = this.sideOffset;
      return copy;
   }

   public CompoundTag serializeNBT(Provider registries) {
      CompoundTag nbt = new CompoundTag();
      nbt.put("Item", this.stack.saveOptional(registries));
      nbt.putFloat("Pos", this.beltPosition);
      nbt.putFloat("PrevPos", this.prevBeltPosition);
      nbt.putFloat("Offset", this.sideOffset);
      nbt.putFloat("PrevOffset", this.prevSideOffset);
      nbt.putInt("InSegment", this.insertedAt);
      nbt.putInt("Angle", this.angle);
      nbt.putInt("InDirection", this.insertedFrom.get3DDataValue());
      if (this.processedBy != null) {
         ResourceLocation key = CreateBuiltInRegistries.FAN_PROCESSING_TYPE.getKey(this.processedBy);
         if (key == null) {
            throw new IllegalArgumentException("Could not get id for FanProcessingType " + this.processedBy + "!");
         }

         nbt.putString("FanProcessingType", key.toString());
         nbt.putInt("FanProcessingTime", this.processingTime);
      }

      if (this.locked) {
         nbt.putBoolean("Locked", this.locked);
      }

      if (this.lockedExternally) {
         nbt.putBoolean("LockedExternally", this.lockedExternally);
      }

      return nbt;
   }

   public static TransportedItemStack read(CompoundTag nbt, Provider registries) {
      TransportedItemStack stack = new TransportedItemStack(ItemStack.parseOptional(registries, nbt.getCompound("Item")));
      stack.beltPosition = nbt.getFloat("Pos");
      stack.prevBeltPosition = nbt.getFloat("PrevPos");
      stack.sideOffset = nbt.getFloat("Offset");
      stack.prevSideOffset = nbt.getFloat("PrevOffset");
      stack.insertedAt = nbt.getInt("InSegment");
      stack.angle = nbt.getInt("Angle");
      stack.insertedFrom = Direction.from3DDataValue(nbt.getInt("InDirection"));
      stack.locked = nbt.getBoolean("Locked");
      stack.lockedExternally = nbt.getBoolean("LockedExternally");
      if (nbt.contains("FanProcessingType")) {
         stack.processedBy = AllFanProcessingTypes.parseLegacy(nbt.getString("FanProcessingType"));
         stack.processingTime = nbt.getInt("FanProcessingTime");
      }

      return stack;
   }

   public void clearFanProcessingData() {
      this.processedBy = null;
      this.processingTime = 0;
   }
}
