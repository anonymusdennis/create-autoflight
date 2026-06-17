package dev.engine_room.flywheel.lib.transform;

import org.joml.Vector3fc;

public interface Scale<Self extends Scale<Self>> {
   Self scale(float var1, float var2, float var3);

   default Self scale(float factor) {
      return this.scale(factor, factor, factor);
   }

   default Self scaleX(float factor) {
      return this.scale(factor, 1.0F, 1.0F);
   }

   default Self scaleY(float factor) {
      return this.scale(1.0F, factor, 1.0F);
   }

   default Self scaleZ(float factor) {
      return this.scale(1.0F, 1.0F, factor);
   }

   default Self scale(Vector3fc factors) {
      return this.scale(factors.x(), factors.y(), factors.z());
   }
}
