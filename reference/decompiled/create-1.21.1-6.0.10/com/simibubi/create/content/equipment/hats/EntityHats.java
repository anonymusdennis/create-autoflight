package com.simibubi.create.content.equipment.hats;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlock;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

public class EntityHats {
   @Nullable
   public static PartialModel getHatFor(LivingEntity entity) {
      if (entity == null) {
         return null;
      } else {
         ItemStack headItem = entity.getItemBySlot(EquipmentSlot.HEAD);
         if (!headItem.isEmpty()) {
            return null;
         } else {
            return shouldRenderTrainHat(entity) ? AllPartialModels.TRAIN_HAT : getLogisticsHatFor(entity);
         }
      }
   }

   public static PartialModel getLogisticsHatFor(LivingEntity entity) {
      if (!entity.isPassenger()) {
         return null;
      } else if (!(entity.getVehicle() instanceof SeatEntity cce)) {
         return null;
      } else {
         int var16 = 0;
         Level level = entity.level();
         BlockPos pos = entity.blockPosition();
         PartialModel hat = null;

         for (Direction d : Iterate.horizontalDirections) {
            for (int y : Iterate.zeroAndOne) {
               Block hatOfStation = level.getBlockState(pos.relative(d).above(y)).getBlock();
               if (hatOfStation instanceof StockTickerBlock) {
                  StockTickerBlock lw = (StockTickerBlock)hatOfStation;
                  PartialModel hatOfStationx = lw.getHat(level, pos, entity);
                  if (hatOfStationx != null) {
                     hat = hatOfStationx;
                     var16++;
                  }
               }
            }
         }

         return var16 == 1 ? hat : null;
      }
   }

   public static boolean shouldRenderTrainHat(LivingEntity entity) {
      if (entity.getPersistentData().contains("TrainHat")) {
         return true;
      } else if (!entity.isPassenger()) {
         return false;
      } else if (entity.getVehicle() instanceof CarriageContraptionEntity cce) {
         if (!cce.hasSchedule() && !(entity instanceof Player)) {
            return false;
         } else if (cce.getContraption() instanceof CarriageContraption cc) {
            BlockPos seatOf = cc.getSeatOf(entity.getUUID());
            if (seatOf == null) {
               return false;
            } else {
               Couple<Boolean> validSides = cc.conductorSeats.get(seatOf);
               return validSides != null;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }
}
