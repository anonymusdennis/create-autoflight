package com.simibubi.create.api.equipment.goggles;

import java.util.List;
import net.minecraft.network.chat.Component;

public non-sealed interface IHaveHoveringInformation extends IHaveCustomOverlayIcon {
   default boolean addToTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
      return false;
   }
}
