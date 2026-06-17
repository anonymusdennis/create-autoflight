package com.simibubi.create.foundation.utility;

import com.mojang.blaze3d.platform.InputConstants.Key;
import com.mojang.blaze3d.platform.InputConstants.Type;
import com.simibubi.create.AllKeys;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;

public class ControlsUtil {
   private static List<KeyMapping> standardControls;

   public static List<KeyMapping> getControls() {
      if (standardControls == null) {
         Options gameSettings = Minecraft.getInstance().options;
         standardControls = new ArrayList<>(6);
         standardControls.add(gameSettings.keyUp);
         standardControls.add(gameSettings.keyDown);
         standardControls.add(gameSettings.keyLeft);
         standardControls.add(gameSettings.keyRight);
         standardControls.add(gameSettings.keyJump);
         standardControls.add(gameSettings.keyShift);
      }

      return standardControls;
   }

   public static boolean isActuallyPressed(KeyMapping kb) {
      Key key = kb.getKey();
      return key.getType() == Type.MOUSE ? AllKeys.isMouseButtonDown(key.getValue()) : AllKeys.isKeyDown(key.getValue());
   }
}
