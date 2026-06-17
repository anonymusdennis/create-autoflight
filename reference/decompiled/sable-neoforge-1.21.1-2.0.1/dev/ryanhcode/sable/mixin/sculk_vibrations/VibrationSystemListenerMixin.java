package dev.ryanhcode.sable.mixin.sculk_vibrations;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEvent.Context;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem.Data;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem.Listener;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({Listener.class})
public class VibrationSystemListenerMixin {
   @WrapMethod(
      method = {"scheduleVibration"}
   )
   private void sable$useGlobalPos(
      ServerLevel level, Data data, Holder<GameEvent> gameEvent, Context context, Vec3 pos, Vec3 sensorPos, Operation<Void> original
   ) {
      original.call(
         new Object[]{level, data, gameEvent, context, Sable.HELPER.projectOutOfSubLevel(level, pos), Sable.HELPER.projectOutOfSubLevel(level, sensorPos)}
      );
   }

   @WrapMethod(
      method = {"isOccluded"}
   )
   private static boolean sable$occlusionChecks(Level level, Vec3 pos1, Vec3 pos2, Operation<Boolean> original) {
      ActiveSableCompanion helper = Sable.HELPER;
      Vec3 global1 = helper.projectOutOfSubLevel(level, pos1);
      Vec3 global2 = helper.projectOutOfSubLevel(level, pos2);
      if ((Boolean)original.call(new Object[]{level, global1, global2})) {
         return true;
      } else {
         SubLevel l1 = helper.getContaining(level, pos1);
         SubLevel l2 = helper.getContaining(level, pos2);
         if (l1 == l2) {
            return l1 == null ? false : (Boolean)original.call(new Object[]{level, pos1, pos2});
         } else {
            return l2 != null && original.call(new Object[]{level, l2.logicalPose().transformPositionInverse(global1), pos2})
               ? true
               : l1 != null && (Boolean)original.call(new Object[]{level, l1.logicalPose().transformPositionInverse(global2), pos2});
         }
      }
   }
}
