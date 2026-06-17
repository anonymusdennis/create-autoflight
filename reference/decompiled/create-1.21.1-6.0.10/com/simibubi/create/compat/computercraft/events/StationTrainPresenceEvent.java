package com.simibubi.create.compat.computercraft.events;

import com.simibubi.create.content.trains.entity.Train;
import org.jetbrains.annotations.NotNull;

public class StationTrainPresenceEvent implements ComputerEvent {
   public StationTrainPresenceEvent.Type type;
   @NotNull
   public Train train;

   public StationTrainPresenceEvent(StationTrainPresenceEvent.Type type, @NotNull Train train) {
      this.type = type;
      this.train = train;
   }

   public static enum Type {
      IMMINENT("train_imminent"),
      ARRIVAL("train_arrival"),
      DEPARTURE("train_departure");

      public final String name;

      private Type(String name) {
         this.name = name;
      }
   }
}
