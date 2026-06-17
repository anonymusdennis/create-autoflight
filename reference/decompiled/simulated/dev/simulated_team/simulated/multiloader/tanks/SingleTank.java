package dev.simulated_team.simulated.multiloader.tanks;

import dev.simulated_team.simulated.service.SimFluidService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Tuple;
import org.jetbrains.annotations.Nullable;

public class SingleTank {
   public long amount;
   public final long capacity;
   public CFluidType type = CFluidType.BLANK;

   public SingleTank(int capacity) {
      this.capacity = SimFluidService.INSTANCE.mbToLoaderUnits((long)capacity);
   }

   public static long calculateInsert(SingleTank tank, CFluidType insertedType, long maxAmount) {
      return !insertedType.equals(tank.type) && !tank.type.isBlank() ? 0L : Math.min(maxAmount, tank.capacity - tank.amount);
   }

   public static void applyInsert(SingleTank tank, CFluidType insertedType, long insertedAmount) {
      tank.type = insertedType;
      tank.amount += insertedAmount;
   }

   public static long calculateExtract(SingleTank tank, CFluidType extractedType, long maxAmount) {
      return extractedType.equals(tank.type) ? Math.min(maxAmount, tank.amount) : 0L;
   }

   public static void applyExtract(SingleTank tank, long extractedAmount) {
      tank.amount -= extractedAmount;
      if (tank.amount == 0L) {
         tank.type = CFluidType.BLANK;
      }
   }

   public long insert(CFluidType insertedType, long maxAmount, boolean simulate, @Nullable Runnable beforeApply) {
      long v = calculateInsert(this, insertedType, maxAmount);
      if (!simulate) {
         if (beforeApply != null) {
            beforeApply.run();
         }

         applyInsert(this, insertedType, v);
      }

      return v;
   }

   public final long insert(CFluidType insertedType, long maxAmount, boolean simulate) {
      return this.insert(insertedType, maxAmount, simulate, null);
   }

   public long extract(CFluidType extractedType, long maxAmount, boolean simulate, @Nullable Runnable beforeApply) {
      long v = calculateExtract(this, extractedType, maxAmount);
      if (!simulate) {
         if (beforeApply != null) {
            beforeApply.run();
         }

         applyExtract(this, v);
      }

      return v;
   }

   public final long extract(CFluidType insertedType, long maxAmount, boolean simulate) {
      return this.extract(insertedType, maxAmount, simulate, null);
   }

   public void read(CompoundTag tag) {
      this.amount = (long)tag.getInt("Amount");
      this.type = CFluidType.read(tag.getCompound("Variant"));
   }

   public CompoundTag write() {
      CompoundTag tag = new CompoundTag();
      tag.putLong("Amount", this.amount);
      tag.put("Variant", this.type.write());
      return tag;
   }

   public Tuple<CFluidType, Long> createSnapshot() {
      return new Tuple(this.type, this.amount);
   }

   public void readSnapshot(CFluidType type, long amount) {
      this.type = type;
      this.amount = amount;
   }
}
