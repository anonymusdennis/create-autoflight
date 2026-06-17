package com.simibubi.create.content.redstone.displayLink.source;

import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

public class EntityNameDisplaySource extends SingleLineDisplaySource {
   @Override
   protected MutableComponent provideLine(DisplayLinkContext context, DisplayTargetStats stats) {
      List<SeatEntity> seats = context.level().getEntitiesOfClass(SeatEntity.class, new AABB(context.getSourcePos()));
      if (seats.isEmpty()) {
         return EMPTY_LINE;
      } else {
         SeatEntity seatEntity = seats.get(0);
         List<Entity> passengers = seatEntity.getPassengers();
         return passengers.isEmpty() ? EMPTY_LINE : Component.literal(passengers.get(0).getDisplayName().getString());
      }
   }

   @Override
   protected String getTranslationKey() {
      return "entity_name";
   }

   @Override
   protected boolean allowsLabeling(DisplayLinkContext context) {
      return true;
   }
}
