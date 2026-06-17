package com.simibubi.create.content.trains.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.EntityEvent.EnteringSection;

public class CarriageEntityHandler {
   public static void onEntityEnterSection(EnteringSection event) {
      if (event.didChunkChange()) {
         Entity entity = event.getEntity();
         if (entity instanceof CarriageContraptionEntity cce) {
            SectionPos newPos = event.getNewPos();
            Level level = entity.level();
            if (!level.isClientSide) {
               if (!isActiveChunk(level, newPos.center())) {
                  cce.leftTickingChunks = true;
               }
            }
         }
      }
   }

   public static void validateCarriageEntity(CarriageContraptionEntity entity) {
      if (entity.isAlive()) {
         Level level = entity.level();
         if (!level.isClientSide) {
            if (!isActiveChunk(level, entity.blockPosition())) {
               entity.leftTickingChunks = true;
            }
         }
      }
   }

   public static boolean isActiveChunk(Level level, BlockPos pos) {
      return level instanceof ServerLevel serverLevel ? serverLevel.isPositionEntityTicking(pos) : false;
   }
}
