package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.block_breakers;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.kinetics.drill.DrillBlock;
import com.simibubi.create.content.kinetics.saw.SawBlock;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.math.LevelReusedVectors;
import dev.ryanhcode.sable.api.math.OrientedBoundingBox3d;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.LevelExtension;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.entity_collision.SubLevelEntityCollision;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({SawBlock.class, DrillBlock.class})
public class BlockBreakingKineticBlockEntityDamageMixin {
   @WrapOperation(
      method = {"entityInside"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/phys/AABB;intersects(Lnet/minecraft/world/phys/AABB;)Z"
      )}
   )
   public boolean sable$fixBlockBreakerDamage(AABB blockAABB, AABB entityAABB, Operation<Boolean> original, @Local(argsOnly = true) Level level) {
      Vector3d jomlCenterPos = JOMLConversion.toJOML(blockAABB.getCenter());
      SubLevel parentSublevel = Sable.HELPER.getContaining(level, jomlCenterPos);
      if (parentSublevel != null) {
         LevelReusedVectors jomlSink = ((LevelExtension)level).sable$getJOMLSink();
         Vector3d entityCenter = JOMLConversion.toJOML(entityAABB.getCenter());
         Vector3d sideLengths = new Vector3d(entityAABB.getXsize(), entityAABB.getYsize(), entityAABB.getZsize());
         OrientedBoundingBox3d burnerBounds = new OrientedBoundingBox3d(
            parentSublevel.logicalPose().transformPosition(jomlCenterPos),
            new Vector3d(blockAABB.getXsize()),
            parentSublevel.logicalPose().orientation(),
            jomlSink
         );
         OrientedBoundingBox3d entityBounds = new OrientedBoundingBox3d(entityCenter, sideLengths, JOMLConversion.QUAT_IDENTITY, jomlSink);
         jomlSink.entityBoxOrientation.identity();
         double yaw = SubLevelEntityCollision.getHitBoxYaw(parentSublevel.logicalPose());
         jomlSink.entityBoxOrientation.rotateY(yaw);
         entityBounds.setOrientation(jomlSink.entityBoxOrientation);
         if (OrientedBoundingBox3d.sat(burnerBounds, entityBounds).lengthSquared() > 0.0) {
            return true;
         }
      }

      return (Boolean)original.call(new Object[]{blockAABB, entityAABB});
   }
}
