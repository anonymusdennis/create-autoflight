package com.simibubi.create.content.redstone.displayLink.source;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class ComputerDisplaySource extends DisplaySource {
   @Override
   public List<MutableComponent> provideText(DisplayLinkContext context, DisplayTargetStats stats) {
      List<MutableComponent> components = new ArrayList<>();
      ListTag tag = context.sourceConfig().getList("ComputerSourceList", 8);

      for (int i = 0; i < tag.size(); i++) {
         components.add(Component.literal(tag.getString(i)));
      }

      return components;
   }

   @Override
   public boolean shouldPassiveReset() {
      return false;
   }
}
