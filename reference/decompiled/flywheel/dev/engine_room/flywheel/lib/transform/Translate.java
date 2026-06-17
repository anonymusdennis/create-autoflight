package dev.engine_room.flywheel.lib.transform;

import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;
import org.joml.Vector3ic;

public interface Translate<Self extends Translate<Self>> {
   float CENTER = 0.5F;

   Self translate(float var1, float var2, float var3);

   default Self translate(double x, double y, double z) {
      return this.translate((float)x, (float)y, (float)z);
   }

   default Self translate(float v) {
      return this.translate(v, v, v);
   }

   default Self translateX(float x) {
      return this.translate(x, 0.0F, 0.0F);
   }

   default Self translateY(float y) {
      return this.translate(0.0F, y, 0.0F);
   }

   default Self translateZ(float z) {
      return this.translate(0.0F, 0.0F, z);
   }

   default Self translate(Vec3i vec) {
      return this.translate((float)vec.getX(), (float)vec.getY(), (float)vec.getZ());
   }

   default Self translate(Vector3ic vec) {
      return this.translate((float)vec.x(), (float)vec.y(), (float)vec.z());
   }

   default Self translate(Vector3fc vec) {
      return this.translate(vec.x(), vec.y(), vec.z());
   }

   default Self translate(Vec3 vec) {
      return this.translate(vec.x, vec.y, vec.z);
   }

   default Self translateBack(float x, float y, float z) {
      return this.translate(-x, -y, -z);
   }

   default Self translateBack(double x, double y, double z) {
      return this.translate(-x, -y, -z);
   }

   default Self translateBack(float v) {
      return this.translate(-v);
   }

   default Self translateBack(Vec3i vec) {
      return this.translateBack((float)vec.getX(), (float)vec.getY(), (float)vec.getZ());
   }

   default Self translateBack(Vector3ic vec) {
      return this.translateBack((float)vec.x(), (float)vec.y(), (float)vec.z());
   }

   default Self translateBack(Vector3fc vec) {
      return this.translateBack(vec.x(), vec.y(), vec.z());
   }

   default Self translateBack(Vec3 vec) {
      return this.translateBack(vec.x, vec.y, vec.z);
   }

   default Self center() {
      return this.translate(0.5F);
   }

   default Self uncenter() {
      return this.translate(-0.5F);
   }

   default Self nudge(int seed) {
      long randomBits = (long)seed * 31L * 493286711L;
      randomBits = randomBits * randomBits * 4392167121L + randomBits * 98761L;
      float xNudge = (((float)(randomBits >> 16 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
      float yNudge = (((float)(randomBits >> 20 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
      float zNudge = (((float)(randomBits >> 24 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
      return this.translate(xNudge, yNudge, zNudge);
   }
}
