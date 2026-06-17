package com.simibubi.create.content.redstone.displayLink.target;

import com.simibubi.create.api.behaviour.display.DisplayTarget;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import java.util.List;
import net.createmod.catnip.data.Couple;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.entity.SignBlockEntity;

public class SignDisplayTarget extends DisplayTarget {
   @Override
   public void acceptText(int line, List<MutableComponent> text, DisplayLinkContext context) {
      if (context.getTargetBlockEntity() instanceof SignBlockEntity sign) {
         boolean changed = false;
         Couple signText = Couple.createWithContext(sign::getText);

         for (int i = 0; i < text.size() && i + line < 4; i++) {
            if (i == 0) {
               reserve(i + line, sign, context);
            }

            if (i > 0 && this.isReserved(i + line, sign, context)) {
               break;
            }

            int iFinal = i;
            String content = text.get(iFinal).getString(sign.getMaxTextLineWidth());
            signText = signText.map(st -> st.setMessage(iFinal + line, Component.literal(content)));
            changed = true;
         }

         if (changed) {
            signText.forEachWithContext(sign::setText);
            context.level().sendBlockUpdated(context.getTargetPos(), sign.getBlockState(), sign.getBlockState(), 2);
         }
      }
   }

   @Override
   public DisplayTargetStats provideStats(DisplayLinkContext context) {
      return new DisplayTargetStats(4, 15, this);
   }

   @Override
   public boolean requiresComponentSanitization() {
      return true;
   }
}
