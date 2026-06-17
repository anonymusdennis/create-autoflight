package dev.ryanhcode.sable.api.sublevel.ticket;

import java.util.Objects;
import java.util.UUID;

public final class SubLevelLoadingTicket<T> {
   private final SubLevelLoadingTicketType<T> type;
   private final UUID subLevelId;
   private final T key;

   public SubLevelLoadingTicket(SubLevelLoadingTicketType<T> type, UUID subLevelId, T key) {
      this.subLevelId = subLevelId;
      this.type = type;
      this.key = key;
   }

   @Override
   public boolean equals(Object o) {
      if (o != null && this.getClass() == o.getClass()) {
         SubLevelLoadingTicket<?> that = (SubLevelLoadingTicket<?>)o;
         return Objects.equals(this.type, that.type) && Objects.equals(this.subLevelId, that.subLevelId) && Objects.equals(this.key, that.key);
      } else {
         return false;
      }
   }

   @Override
   public String toString() {
      String type = String.valueOf(this.type);
      return "SubLevelLoadingTicket[" + type + " " + this.subLevelId + " (" + this.key + ")]";
   }

   public SubLevelLoadingTicketType<T> getType() {
      return this.type;
   }

   public T getKey() {
      return this.key;
   }

   public UUID getSubLevelId() {
      return this.subLevelId;
   }
}
