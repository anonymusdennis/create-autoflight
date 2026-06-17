package com.simibubi.create.content.processing.burner;

import dev.engine_room.flywheel.api.instance.InstanceHandle;
import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.lib.instance.ColoredLitOverlayInstance;
import net.createmod.catnip.render.SpriteShiftEntry;
import net.minecraft.core.Vec3i;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;

public class ScrollInstance extends ColoredLitOverlayInstance {
   public float x;
   public float y;
   public float z;
   public final Quaternionf rotation = new Quaternionf();
   public float speedU;
   public float speedV;
   public float offsetU;
   public float offsetV;
   public float diffU;
   public float diffV;
   public float scaleU;
   public float scaleV;

   public ScrollInstance(InstanceType<? extends ColoredLitOverlayInstance> type, InstanceHandle handle) {
      super(type, handle);
   }

   public ScrollInstance position(Vec3i position) {
      this.x = (float)position.getX();
      this.y = (float)position.getY();
      this.z = (float)position.getZ();
      return this;
   }

   public ScrollInstance position(float x, float y, float z) {
      this.x = x;
      this.y = y;
      this.z = z;
      return this;
   }

   public ScrollInstance shift(float x, float y, float z) {
      this.x += x;
      this.y += y;
      this.z += z;
      return this;
   }

   public ScrollInstance rotation(Quaternionfc rotation) {
      this.rotation.set(rotation);
      return this;
   }

   public ScrollInstance setSpriteShift(SpriteShiftEntry spriteShift) {
      return this.setSpriteShift(spriteShift, 0.5F, 0.5F);
   }

   public ScrollInstance setSpriteShift(SpriteShiftEntry spriteShift, float factorU, float factorV) {
      float spriteWidth = spriteShift.getTarget().getU1() - spriteShift.getTarget().getU0();
      float spriteHeight = spriteShift.getTarget().getV1() - spriteShift.getTarget().getV0();
      this.scaleU = spriteWidth * factorU;
      this.scaleV = spriteHeight * factorV;
      this.diffU = spriteShift.getTarget().getU0() - spriteShift.getOriginal().getU0();
      this.diffV = spriteShift.getTarget().getV0() - spriteShift.getOriginal().getV0();
      return this;
   }

   public ScrollInstance speed(float speedU, float speedV) {
      this.speedU = speedU;
      this.speedV = speedV;
      return this;
   }

   public ScrollInstance offset(float offsetU, float offsetV) {
      this.offsetU = offsetU;
      this.offsetV = offsetV;
      return this;
   }
}
