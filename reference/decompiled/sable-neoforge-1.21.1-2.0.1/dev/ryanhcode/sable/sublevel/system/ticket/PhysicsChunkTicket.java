package dev.ryanhcode.sable.sublevel.system.ticket;

import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.Collection;
import java.util.Objects;
import net.minecraft.core.SectionPos;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public final class PhysicsChunkTicket {
   private final SectionPos pos;
   private final Collection<SubLevel> residentSubLevels;
   private long lastInhabitedTick;

   public PhysicsChunkTicket(SectionPos pos, long lastInhabitedTick, Collection<SubLevel> residentSubLevels) {
      this.pos = pos;
      this.lastInhabitedTick = lastInhabitedTick;
      this.residentSubLevels = residentSubLevels;
   }

   public SectionPos pos() {
      return this.pos;
   }

   public long lastInhabitedTick() {
      return this.lastInhabitedTick;
   }

   public void setLastInhabitedTick(long lastInhabitedTick) {
      this.lastInhabitedTick = lastInhabitedTick;
   }

   public Collection<SubLevel> residentSubLevels() {
      return this.residentSubLevels;
   }

   @Override
   public boolean equals(Object obj) {
      if (obj == this) {
         return true;
      } else if (obj != null && obj.getClass() == this.getClass()) {
         PhysicsChunkTicket that = (PhysicsChunkTicket)obj;
         return Objects.equals(this.pos, that.pos);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.pos, this.lastInhabitedTick);
   }

   @Override
   public String toString() {
      return "PhysicsChunkTicket[pos=" + this.pos + ", lastInhabitedTick=" + this.lastInhabitedTick + ", residentSubLevels=" + this.residentSubLevels + "]";
   }
}
