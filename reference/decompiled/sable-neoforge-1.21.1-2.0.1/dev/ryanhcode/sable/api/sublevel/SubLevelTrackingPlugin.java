package dev.ryanhcode.sable.api.sublevel;

import java.util.UUID;

public interface SubLevelTrackingPlugin {
   Iterable<UUID> neededPlayers();

   void sendTrackingData(int var1);
}
