package com.simibubi.create.api.equipment.goggles;

import com.simibubi.create.foundation.utility.CreateLang;
import java.util.List;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public non-sealed interface IHaveGoggleInformation extends IHaveCustomOverlayIcon {
   default boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
      return false;
   }

   default boolean containedFluidTooltip(List<Component> tooltip, boolean isPlayerSneaking, IFluidHandler handler) {
      if (handler == null) {
         return false;
      } else if (handler.getTanks() == 0) {
         return false;
      } else {
         LangBuilder mb = CreateLang.translate("generic.unit.millibuckets");
         CreateLang.translate("gui.goggles.fluid_container").forGoggles(tooltip);
         boolean isEmpty = true;

         for (int i = 0; i < handler.getTanks(); i++) {
            FluidStack fluidStack = handler.getFluidInTank(i);
            if (!fluidStack.isEmpty()) {
               CreateLang.fluidName(fluidStack).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
               CreateLang.builder()
                  .add(CreateLang.number((double)fluidStack.getAmount()).add(mb).style(ChatFormatting.GOLD))
                  .text(ChatFormatting.GRAY, " / ")
                  .add(CreateLang.number((double)handler.getTankCapacity(i)).add(mb).style(ChatFormatting.DARK_GRAY))
                  .forGoggles(tooltip, 1);
               isEmpty = false;
            }
         }

         if (handler.getTanks() > 1) {
            if (isEmpty) {
               tooltip.remove(tooltip.size() - 1);
            }

            return true;
         } else if (!isEmpty) {
            return true;
         } else {
            CreateLang.translate("gui.goggles.fluid_container.capacity")
               .add(CreateLang.number((double)handler.getTankCapacity(0)).add(mb).style(ChatFormatting.GOLD))
               .style(ChatFormatting.GRAY)
               .forGoggles(tooltip, 1);
            return true;
         }
      }
   }
}
