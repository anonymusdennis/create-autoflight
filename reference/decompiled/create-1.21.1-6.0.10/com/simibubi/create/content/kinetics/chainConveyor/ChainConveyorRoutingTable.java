package com.simibubi.create.content.kinetics.chainConveyor;

import com.simibubi.create.content.logistics.box.PackageItem;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.mutable.MutableInt;

public class ChainConveyorRoutingTable {
   public static final int ENTRY_TIMEOUT = 100;
   public static final int PORT_ENTRY_TIMEOUT = 20;
   public List<ChainConveyorRoutingTable.RoutingTableEntry> entriesByDistance = new ArrayList<>();
   public int lastUpdate;
   public boolean changed;

   public void tick() {
      this.entriesByDistance.forEach(ChainConveyorRoutingTable.RoutingTableEntry::tick);
      this.changed = this.changed | this.entriesByDistance.removeIf(ChainConveyorRoutingTable.RoutingTableEntry::invalid);
      this.lastUpdate++;
   }

   public boolean shouldAdvertise() {
      return this.changed || this.lastUpdate > 80;
   }

   public void receivePortInfo(String filter, BlockPos connection) {
      this.insert(new ChainConveyorRoutingTable.RoutingTableEntry(filter, "*".equals(filter) ? 1000 : 0, connection, new MutableInt(20), true));
   }

   public BlockPos getExitFor(ItemStack box) {
      for (ChainConveyorRoutingTable.RoutingTableEntry entry : this.entriesByDistance) {
         if (PackageItem.matchAddress(box, entry.port())) {
            return entry.nextConnection();
         }
      }

      return BlockPos.ZERO;
   }

   public void advertiseTo(BlockPos connection, ChainConveyorRoutingTable otherTable) {
      BlockPos backConnection = connection.multiply(-1);

      for (ChainConveyorRoutingTable.RoutingTableEntry entry : this.entriesByDistance) {
         if (entry.endOfRoute() || !connection.equals(entry.nextConnection())) {
            otherTable.insert(entry.copyForNeighbour(connection));
         }
      }

      otherTable.entriesByDistance.removeIf(e -> e.timeout().intValue() < 100 && !e.endOfRoute() && backConnection.equals(e.nextConnection()));
   }

   private void insert(ChainConveyorRoutingTable.RoutingTableEntry entry) {
      int targetIndex = 0;

      for (int i = 0; i < this.entriesByDistance.size(); i++) {
         ChainConveyorRoutingTable.RoutingTableEntry otherEntry = this.entriesByDistance.get(i);
         if (otherEntry.distance() > entry.distance()) {
            break;
         }

         if (otherEntry.port().equals(entry.port())) {
            if (otherEntry.distance() == entry.distance() && otherEntry.nextConnection().equals(entry.nextConnection())) {
               otherEntry.timeout.setValue(100);
            }

            return;
         }

         targetIndex = i + 1;
      }

      this.entriesByDistance.add(targetIndex, entry);
      this.changed = true;
   }

   public Collection<? extends Component> createSummary() {
      ArrayList<Component> list = new ArrayList<>();

      for (ChainConveyorRoutingTable.RoutingTableEntry entry : this.entriesByDistance) {
         list.add(Component.literal("    [" + entry.distance() + "] " + entry.port()));
      }

      return list;
   }

   public static record RoutingTableEntry(String port, int distance, BlockPos nextConnection, MutableInt timeout, boolean endOfRoute) {
      public void tick() {
         this.timeout.decrement();
      }

      public boolean invalid() {
         return this.timeout.intValue() <= 0;
      }

      public ChainConveyorRoutingTable.RoutingTableEntry copyForNeighbour(BlockPos connection) {
         return new ChainConveyorRoutingTable.RoutingTableEntry(this.port, this.distance + 1, connection.multiply(-1), new MutableInt(100), false);
      }
   }
}
