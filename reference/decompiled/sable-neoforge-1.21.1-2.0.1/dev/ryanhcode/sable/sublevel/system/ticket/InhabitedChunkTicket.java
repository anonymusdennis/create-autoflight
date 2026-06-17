package dev.ryanhcode.sable.sublevel.system.ticket;

import java.util.UUID;
import net.minecraft.server.level.Ticket;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public final class InhabitedChunkTicket {
   private final UUID uuid;
   private final Ticket<UUID> ticket;
   private long lastInhabitedTick;

   public InhabitedChunkTicket(UUID uuid, long lastInhabitedTick, Ticket<UUID> ticket) {
      this.uuid = uuid;
      this.lastInhabitedTick = lastInhabitedTick;
      this.ticket = ticket;
   }

   public long lastInhabitedTick() {
      return this.lastInhabitedTick;
   }

   public void setLastInhabitedTick(long lastInhabitedTick) {
      this.lastInhabitedTick = lastInhabitedTick;
   }

   public Ticket<UUID> getTicket() {
      return this.ticket;
   }

   @Override
   public int hashCode() {
      return this.uuid.hashCode();
   }

   @Override
   public boolean equals(Object obj) {
      return obj instanceof InhabitedChunkTicket other ? this.uuid.equals(other.uuid) : false;
   }
}
