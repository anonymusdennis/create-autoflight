package dev.ryanhcode.sable.mixin.portal;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.BlockUtil.FoundRectangle;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin({Entity.class})
public abstract class EntityMixin {
   @Shadow
   public abstract Vec3 position();

   @Shadow
   public abstract EntityDimensions getDimensions(Pose var1);

   @Shadow
   public abstract Pose getPose();

   @Shadow
   public abstract Level level();

   @Overwrite
   public Vec3 getRelativePortalPosition(Axis axis, FoundRectangle foundRectangle) {
      SubLevel subLevel = Sable.HELPER.getContaining(this.level(), foundRectangle.minCorner);
      Vec3 position = this.position();
      if (subLevel != null) {
         position = subLevel.logicalPose().transformPositionInverse(position);
      }

      return PortalShape.getRelativePosition(foundRectangle, axis, position, this.getDimensions(this.getPose()));
   }
}
