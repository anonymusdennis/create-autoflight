package com.simibubi.create.content.redstone.displayLink.source;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.smartObserver.SmartObserverBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.InvManipulationBehaviour;
import com.simibubi.create.foundation.item.CountedItemStackList;
import java.util.stream.Stream;
import net.createmod.catnip.data.IntAttached;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.neoforge.items.IItemHandler;

public class ItemListDisplaySource extends ValueListDisplaySource {
   @Override
   protected Stream<IntAttached<MutableComponent>> provideEntries(DisplayLinkContext context, int maxRows) {
      if (context.getSourceBlockEntity() instanceof SmartObserverBlockEntity cobe) {
         InvManipulationBehaviour invManipulationBehaviour = cobe.getBehaviour(InvManipulationBehaviour.TYPE);
         FilteringBehaviour filteringBehaviour = cobe.getBehaviour(FilteringBehaviour.TYPE);
         IItemHandler handler = invManipulationBehaviour.getInventory();
         return handler == null ? Stream.empty() : new CountedItemStackList(handler, filteringBehaviour).getTopNames(maxRows);
      } else {
         return Stream.empty();
      }
   }

   @Override
   protected String getTranslationKey() {
      return "list_items";
   }

   @Override
   protected boolean valueFirst() {
      return true;
   }
}
