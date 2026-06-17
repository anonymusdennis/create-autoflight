package dev.ryanhcode.sable.mixin.debug_render;

import dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.render.dispatcher.SubLevelRenderDispatcher;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.At.Shift;

@Mixin({DebugScreenOverlay.class})
public abstract class DebugScreenOverlayMixin {
   @Shadow
   protected abstract Level getLevel();

   @ModifyVariable(
      method = {"getSystemInformation"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/Minecraft;showOnlyReducedInfo()Z",
         shift = Shift.BEFORE
      ),
      ordinal = 0
   )
   public List<String> sable$addDebugInfo(List<String> value) {
      SubLevelContainer container = SubLevelContainer.getContainer(Minecraft.getInstance().level);
      value.add("");
      value.add(ChatFormatting.UNDERLINE + "Sable");
      if (container instanceof ClientSubLevelContainer clientContainer) {
         clientContainer.addDebugInfo(value::add);
      }

      SubLevelRenderDispatcher.get().addDebugInfo(value::add);
      return value;
   }
}
