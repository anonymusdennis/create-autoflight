package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.tracks;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.trains.graph.TrackGraphBounds;
import com.simibubi.create.content.trains.graph.TrackGraphVisualizer;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({TrackGraphVisualizer.class})
public class TrackGraphVisualizerMixin {
   @WrapOperation(
      method = {"debugViewGraph"},
      at = {@At(
         value = "FIELD",
         target = "Lcom/simibubi/create/content/trains/graph/TrackGraphBounds;box:Lnet/minecraft/world/phys/AABB;",
         opcode = 180
      )}
   )
   private static AABB debugViewGraph(TrackGraphBounds instance, Operation<AABB> original) {
      if (instance.box == null) {
         return (AABB)original.call(new Object[]{instance});
      } else {
         Level level = Minecraft.getInstance().level;
         if (level == null) {
            return (AABB)original.call(new Object[]{instance});
         } else {
            Vec3 center = instance.box.getCenter();
            SubLevel containing = Sable.HELPER.getContaining(level, center);
            return containing == null
               ? (AABB)original.call(new Object[]{instance})
               : new BoundingBox3d(instance.box).transform(containing.logicalPose()).toMojang();
         }
      }
   }

   @WrapOperation(
      method = {"debugViewGraph"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/phys/Vec3;distanceTo(Lnet/minecraft/world/phys/Vec3;)D"
      )}
   )
   private static double debugViewGraph(Vec3 location, Vec3 camera, Operation<Double> original) {
      Level level = Minecraft.getInstance().level;
      return level == null
         ? (Double)original.call(new Object[]{location, camera})
         : Math.sqrt(Sable.HELPER.distanceSquaredWithSubLevels(level, location, camera));
   }
}
