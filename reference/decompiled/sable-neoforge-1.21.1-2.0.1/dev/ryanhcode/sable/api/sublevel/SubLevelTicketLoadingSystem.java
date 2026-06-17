package dev.ryanhcode.sable.api.sublevel;

import dev.ryanhcode.sable.api.sublevel.ticket.SubLevelTicketInfo;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public class SubLevelTicketLoadingSystem implements SubLevelObserver {
   private final ServerSubLevelContainer container;

   public SubLevelTicketLoadingSystem(ServerSubLevelContainer container) {
      this.container = container;
   }

   @Override
   public void onSubLevelAdded(SubLevel subLevel) {
      SubLevelTicketInfo info = (SubLevelTicketInfo)this.container.allTickets.get(subLevel.getUniqueId());
      if (info != null && !info.tickets().isEmpty()) {
         this.container.activeTickets.put((ServerSubLevel)subLevel, new ObjectArraySet(info.tickets()));
      }
   }

   @Override
   public void onSubLevelRemoved(SubLevel subLevel, SubLevelRemovalReason reason) {
      ServerSubLevel serverSubLevel = (ServerSubLevel)subLevel;
      if (reason == SubLevelRemovalReason.UNLOADED) {
         this.container.activeTickets.remove(serverSubLevel);
         SubLevelTicketInfo info = (SubLevelTicketInfo)this.container.allTickets.get(serverSubLevel);
         if (info != null) {
            info.setPointer(serverSubLevel.getLastSerializationPointer());
         }
      } else if (reason == SubLevelRemovalReason.REMOVED) {
         this.container.allTickets.remove(subLevel.getUniqueId());
         this.container.activeTickets.remove(serverSubLevel);
      }
   }
}
