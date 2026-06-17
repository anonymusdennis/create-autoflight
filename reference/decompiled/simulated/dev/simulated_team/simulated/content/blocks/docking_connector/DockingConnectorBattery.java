package dev.simulated_team.simulated.content.blocks.docking_connector;

import dev.simulated_team.simulated.multiloader.energy.SingleBattery;

public class DockingConnectorBattery extends SingleBattery {
   private DockingConnectorBattery other = null;

   public DockingConnectorBattery(int maxEnergy, int throughput) {
      super(maxEnergy, throughput);
   }

   public void connect(DockingConnectorBattery other) {
      this.other = other;
   }

   public void disconnect() {
      this.other = null;
   }

   protected int superReceiveEnergy(int toReceive, boolean simulate) {
      return super.receiveEnergy(toReceive, simulate);
   }

   @Override
   public int receiveEnergy(int toReceive, boolean simulate) {
      return this.other != null ? this.other.superReceiveEnergy(toReceive, simulate) : 0;
   }

   @Override
   public boolean canReceive() {
      return this.other != null;
   }
}
