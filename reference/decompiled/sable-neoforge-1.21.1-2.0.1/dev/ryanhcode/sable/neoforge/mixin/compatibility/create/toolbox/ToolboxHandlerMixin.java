package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.toolbox;

import com.simibubi.create.content.equipment.toolbox.ToolboxBlockEntity;
import com.simibubi.create.content.equipment.toolbox.ToolboxHandler;
import dev.ryanhcode.sable.Sable;
import java.util.Comparator;
import java.util.List;
import java.util.WeakHashMap;
import java.util.stream.Collectors;
import net.createmod.catnip.data.WorldAttached;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ToolboxHandler.class})
public abstract class ToolboxHandlerMixin {
   @Shadow
   @Final
   public static WorldAttached<WeakHashMap<BlockPos, ToolboxBlockEntity>> toolboxes;

   @Inject(
      method = {"withinRange"},
      at = {@At("HEAD")},
      remap = false,
      cancellable = true
   )
   private static void sable$withinRangeToolBoxRedirect(Player player, ToolboxBlockEntity box, CallbackInfoReturnable<Boolean> cir) {
      if (player.level() != box.getLevel()) {
         cir.setReturnValue(false);
      }

      double maxRange = ToolboxHandler.getMaxRange(player);
      cir.setReturnValue(sable$getDistance(player.level(), player.position(), box.getBlockPos()) < maxRange * maxRange);
   }

   @Inject(
      method = {"getNearest"},
      at = {@At("HEAD")},
      remap = false,
      cancellable = true
   )
   private static void sable$getNearestToolBoxRedirect(LevelAccessor world, Player player, int maxAmount, CallbackInfoReturnable<List<ToolboxBlockEntity>> cir) {
      Vec3 location = player.position();
      double maxRange = ToolboxHandler.getMaxRange(player);
      cir.setReturnValue(
         (List)((WeakHashMap)toolboxes.get(world))
            .keySet()
            .stream()
            .filter(p -> sable$getDistance(world, location, p) < maxRange * maxRange)
            .sorted(Comparator.comparingDouble(p -> sable$getDistance(world, location, p)))
            .limit((long)maxAmount)
            .map(((WeakHashMap)toolboxes.get(world))::get)
            .filter(ToolboxBlockEntity::isFullyInitialized)
            .collect(Collectors.toList())
      );
   }

   @Unique
   private static double sable$getDistance(LevelAccessor level, Vec3 pos, BlockPos bPos) {
      return Sable.HELPER.distanceSquaredWithSubLevels((Level)level, pos, (double)bPos.getX() + 0.5, (double)bPos.getY(), (double)bPos.getZ() + 0.5);
   }
}
