package dev.simulated_team.simulated.content.blocks.auger_shaft.auger_groups;

import dev.ryanhcode.sable.util.LevelAccelerator;
import dev.simulated_team.simulated.content.blocks.auger_shaft.BlockHarvester;
import dev.simulated_team.simulated.content.blocks.auger_shaft.ItemReciever;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.mutable.MutableInt;

public record AugerDistributor(List<ItemReciever> receivers, List<BlockHarvester> harvesters, MutableInt index) {
   public AugerDistributor() {
      this(new ArrayList<>(), new ArrayList<>(), new MutableInt());
   }

   public ItemStack distributeItem(ItemStack stack, BlockPos fromPos) {
      if (this.receivers.isEmpty()) {
         return stack;
      } else if (this.checkAndCleanReceivers()) {
         return stack;
      } else {
         ItemStack modifiedStack = stack;
         int startIndex = this.index.getValue();

         do {
            ItemReciever nextReceiver = this.receivers.get(this.index.getValue());
            if (nextReceiver.isActive()) {
               modifiedStack = nextReceiver.onRecieveItem(stack, fromPos);
            }

            this.index.increment();
            this.index.setValue(this.index.getValue() % this.receivers.size());
         } while (startIndex != this.index.getValue() && modifiedStack.equals(stack));

         return modifiedStack;
      }
   }

   private boolean checkAndCleanReceivers() {
      if (this.receivers.removeIf(ItemReciever::removed)) {
         if (this.receivers.isEmpty()) {
            return true;
         }

         this.index.setValue(this.index.getValue() % this.receivers.size());
      }

      return false;
   }

   public void gatherAndAssociateHarvesters(Direction[] surrounding, BlockPos startingPos, Level level, LevelAccelerator accelerator) {
      Set<BlockPos> visited = new HashSet<>();
      Queue<BlockPos> frontier = new ArrayDeque<>(16);

      for (BlockHarvester harvester : this.harvesters) {
         harvester.simulated$setDistributor(null);
      }

      this.harvesters.clear();
      frontier.add(startingPos);

      while (!frontier.isEmpty()) {
         BlockPos pos = frontier.poll();
         if (!visited.contains(pos)) {
            visited.add(pos);
            if (level.getBlockEntity(pos) instanceof BlockHarvester harvester) {
               this.harvesters.add(harvester);
               harvester.simulated$setDistributor(this);
            }

            for (Direction d : surrounding) {
               BlockPos newp = pos.relative(d);
               if (accelerator.getBlockState(newp).hasBlockEntity()) {
                  frontier.add(newp);
               }
            }
         }
      }

      accelerator.clearCache();
      visited.clear();
      frontier.clear();
   }

   public void addReceiver(ItemReciever receiver) {
      this.receivers.add(receiver);
   }

   public void removeReceiver(ItemReciever receiver) {
      this.receivers.remove(receiver);
   }
}
