package com.simibubi.create.api.event;

import com.simibubi.create.content.trains.graph.TrackGraph;
import net.neoforged.bus.api.Event;

public class TrackGraphMergeEvent extends Event {
   private final TrackGraph mergedInto;
   private final TrackGraph mergedFrom;

   public TrackGraphMergeEvent(TrackGraph from, TrackGraph into) {
      this.mergedInto = into;
      this.mergedFrom = from;
   }

   public TrackGraph getGraphMergedInto() {
      return this.mergedInto;
   }

   public TrackGraph getGraphMergedFrom() {
      return this.mergedFrom;
   }
}
