package dev.simulated_team.simulated.multiloader.energy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

public class SingleBattery {
   public final int maxEnergy;
   protected int throughput;
   protected int energy = 0;

   public SingleBattery(int maxEnergy, int throughput) {
      this.maxEnergy = maxEnergy;
      this.throughput = throughput;
   }

   public int getEnergy() {
      return this.energy;
   }

   public void setEnergy(int v) {
      this.energy = v;
   }

   public int getThroughput() {
      return this.throughput;
   }

   public void setThroughput(int v) {
      this.throughput = v;
   }

   public int receiveEnergy(int toReceive, boolean simulate) {
      int diff = Math.min(Math.min(toReceive, this.maxEnergy - this.energy), this.throughput);
      if (!simulate) {
         this.energy += diff;
      }

      return diff;
   }

   public int extractEnergy(int toExtract, boolean simulate) {
      int diff = Math.min(Math.min(toExtract, this.energy), this.throughput);
      if (!simulate) {
         this.energy -= diff;
      }

      return diff;
   }

   public boolean canExtract() {
      return true;
   }

   public boolean canReceive() {
      return true;
   }

   public void read(CompoundTag tag) {
      this.energy = Mth.clamp(tag.getInt("Energy"), 0, this.maxEnergy);
   }

   public CompoundTag write() {
      CompoundTag tag = new CompoundTag();
      tag.putInt("Energy", this.energy);
      return tag;
   }
}
