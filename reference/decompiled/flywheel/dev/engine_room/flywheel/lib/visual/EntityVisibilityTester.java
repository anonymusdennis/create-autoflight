package dev.engine_room.flywheel.lib.visual;

import dev.engine_room.flywheel.lib.math.MoreMath;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.joml.FrustumIntersection;

public class EntityVisibilityTester {
   private final Entity entity;
   private final Vec3i renderOrigin;
   private final float scale;
   @Nullable
   private AABB lastVisibleAABB;

   public EntityVisibilityTester(Entity entity, Vec3i renderOrigin, float scale) {
      this.entity = entity;
      this.renderOrigin = renderOrigin;
      this.scale = scale;
   }

   public boolean check(FrustumIntersection frustum) {
      AABB aabb = this.entity.getBoundingBoxForCulling();
      boolean visible = this.lastVisibleAABB == null;
      if (!visible) {
         visible = this.adjustAndTestAABB(frustum, aabb);
      }

      if (!visible && this.lastVisibleAABB != aabb) {
         visible = this.adjustAndTestAABB(frustum, this.lastVisibleAABB);
      }

      if (visible) {
         this.lastVisibleAABB = aabb;
      }

      return visible;
   }

   private boolean adjustAndTestAABB(FrustumIntersection frustum, AABB aabb) {
      float x = (float)Mth.lerp(0.5, aabb.minX, aabb.maxX) - (float)this.renderOrigin.getX();
      float y = (float)Mth.lerp(0.5, aabb.minY, aabb.maxY) - (float)this.renderOrigin.getY();
      float z = (float)Mth.lerp(0.5, aabb.minZ, aabb.maxZ) - (float)this.renderOrigin.getZ();
      float maxSize = (float)Math.max(aabb.getXsize(), Math.max(aabb.getYsize(), aabb.getZsize()));
      return frustum.testSphere(x, y, z, maxSize * MoreMath.SQRT_3_OVER_2 * this.scale);
   }
}
