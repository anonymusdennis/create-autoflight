package dev.simulated_team.simulated.multiloader.energy;

import net.neoforged.neoforge.energy.IEnergyStorage;

public class SingleBatteryWrapper implements IEnergyStorage {
   private final SingleBattery battery;

   public SingleBatteryWrapper(SingleBattery battery) {
      this.battery = battery;
   }

   public int receiveEnergy(int toReceive, boolean simulate) {
      return this.battery.receiveEnergy(toReceive, simulate);
   }

   public int extractEnergy(int toExtract, boolean simulate) {
      return this.battery.extractEnergy(toExtract, simulate);
   }

   public int getEnergyStored() {
      return this.battery.getEnergy();
   }

   public int getMaxEnergyStored() {
      return this.battery.maxEnergy;
   }

   public boolean canExtract() {
      return this.battery.canExtract();
   }

   public boolean canReceive() {
      return this.battery.canReceive();
   }
}
