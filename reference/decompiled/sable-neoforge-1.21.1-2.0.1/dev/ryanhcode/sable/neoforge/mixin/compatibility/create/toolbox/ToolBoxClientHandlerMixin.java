package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.toolbox;

import com.simibubi.create.content.equipment.toolbox.ToolboxHandlerClient;
import dev.ryanhcode.sable.Sable;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({ToolboxHandlerClient.class})
public class ToolBoxClientHandlerMixin {
   @Redirect(
      method = {"*"},
      at = @At(
         value = "INVOKE",
         target = "Lcom/simibubi/create/content/equipment/toolbox/ToolboxHandler;distance(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/core/BlockPos;)D"
      )
   )
   private static double sable$sublevelDistance(Vec3 location, BlockPos p) {
      return Sable.HELPER
         .distanceSquaredWithSubLevels(Minecraft.getInstance().level, location, (double)p.getX() + 0.5, (double)p.getY(), (double)p.getZ() + 0.5);
   }
}
