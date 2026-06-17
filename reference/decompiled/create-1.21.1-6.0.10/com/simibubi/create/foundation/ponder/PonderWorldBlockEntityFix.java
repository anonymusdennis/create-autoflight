package com.simibubi.create.foundation.ponder;

import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;
import java.util.Iterator;
import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

public class PonderWorldBlockEntityFix {
   public static void fixControllerBlockEntities(PonderLevel world) {
      Iterator var1 = world.getBlockEntities().iterator();

      while (true) {
         BlockEntity blockEntity;
         label40:
         while (true) {
            if (!var1.hasNext()) {
               return;
            }

            blockEntity = (BlockEntity)var1.next();
            if (!(blockEntity instanceof BeltBlockEntity beltBlockEntity)) {
               break;
            }

            if (beltBlockEntity.isController()) {
               BlockPos controllerPos = blockEntity.getBlockPos();
               Iterator current = BeltBlock.getBeltChain(world, controllerPos).iterator();

               while (true) {
                  if (!current.hasNext()) {
                     break label40;
                  }

                  BlockPos blockPos = (BlockPos)current.next();
                  if (world.getBlockEntity(blockPos) instanceof BeltBlockEntity belt2) {
                     belt2.setController(controllerPos);
                  }
               }
            }
         }

         if (blockEntity instanceof IMultiBlockEntityContainer multiBlockEntity) {
            BlockPos lastKnown = multiBlockEntity.getLastKnownPos();
            BlockPos current = blockEntity.getBlockPos();
            if (lastKnown != null && current != null && !multiBlockEntity.isController() && !lastKnown.equals(current)) {
               BlockPos newControllerPos = multiBlockEntity.getController().offset(current.subtract(lastKnown));
               multiBlockEntity.setController(newControllerPos);
            }
         }
      }
   }
}
