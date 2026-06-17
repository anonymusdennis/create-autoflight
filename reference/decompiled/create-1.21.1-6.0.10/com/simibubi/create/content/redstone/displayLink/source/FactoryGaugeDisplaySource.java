package com.simibubi.create.content.redstone.displayLink.source;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelPosition;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import net.createmod.catnip.data.IntAttached;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class FactoryGaugeDisplaySource extends ValueListDisplaySource {
   @Override
   protected Stream<IntAttached<MutableComponent>> provideEntries(DisplayLinkContext context, int maxRows) {
      List<FactoryPanelPosition> panels = context.blockEntity().factoryPanelSupport.getLinkedPanels();
      return panels.isEmpty()
         ? Stream.empty()
         : panels.stream().map(fpp -> this.createEntry(context.level(), fpp)).filter(Objects::nonNull).limit((long)maxRows);
   }

   @Nullable
   public IntAttached<MutableComponent> createEntry(Level level, FactoryPanelPosition pos) {
      FactoryPanelBehaviour panel = FactoryPanelBehaviour.at(level, pos);
      if (panel == null) {
         return null;
      } else {
         ItemStack filter = panel.getFilter();
         int demand = panel.getAmount() * (panel.upTo ? 1 : filter.getMaxStackSize());
         String s = " ";
         if (demand != 0) {
            int promised = panel.getPromised();
            if (panel.satisfied) {
               s = "✔";
            } else if (promised != 0) {
               s = "↑";
            } else {
               s = "▪";
            }
         }

         return IntAttached.with(
            panel.getLevelInStorage(),
            Component.literal(s + " ")
               .withStyle(style -> style.withColor(panel.getIngredientStatusColor()))
               .append(filter.getHoverName().plainCopy().withStyle(ChatFormatting.RESET))
         );
      }
   }

   @Override
   protected String getTranslationKey() {
      return "gauge_status";
   }

   @Override
   protected boolean valueFirst() {
      return true;
   }
}
