package com.simibubi.create.content.equipment.bell;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.lib.util.ShadersModHelper;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class CustomRotationParticle extends SimpleAnimatedParticle {
   protected boolean mirror;
   protected int loopLength;

   public CustomRotationParticle(ClientLevel worldIn, double x, double y, double z, SpriteSet spriteSet, float yAccel) {
      super(worldIn, x, y, z, spriteSet, yAccel);
   }

   public void selectSpriteLoopingWithAge(SpriteSet sprite) {
      int loopFrame = this.age % this.loopLength;
      this.setSprite(sprite.get(loopFrame, this.loopLength));
   }

   public Quaternionf getCustomRotation(Camera camera, float partialTicks) {
      Quaternionf quaternion = new Quaternionf(camera.rotation());
      if (this.roll != 0.0F) {
         float angle = Mth.lerp(partialTicks, this.oRoll, this.roll);
         quaternion.mul(Axis.ZP.rotation(angle));
      }

      return quaternion;
   }

   public void render(VertexConsumer builder, Camera camera, float partialTicks) {
      Vec3 cameraPos = camera.getPosition();
      float originX = (float)(Mth.lerp((double)partialTicks, this.xo, this.x) - cameraPos.x());
      float originY = (float)(Mth.lerp((double)partialTicks, this.yo, this.y) - cameraPos.y());
      float originZ = (float)(Mth.lerp((double)partialTicks, this.zo, this.z) - cameraPos.z());
      Vector3f[] vertices = new Vector3f[]{
         new Vector3f(-1.0F, -1.0F, 0.0F), new Vector3f(-1.0F, 1.0F, 0.0F), new Vector3f(1.0F, 1.0F, 0.0F), new Vector3f(1.0F, -1.0F, 0.0F)
      };
      float scale = this.getQuadSize(partialTicks);
      Quaternionf rotation = this.getCustomRotation(camera, partialTicks);

      for (int i = 0; i < 4; i++) {
         Vector3f vertex = vertices[i];
         vertex.rotate(rotation);
         vertex.mul(scale);
         vertex.add(originX, originY, originZ);
      }

      float minU = this.mirror ? this.getU1() : this.getU0();
      float maxU = this.mirror ? this.getU0() : this.getU1();
      float minV = this.getV0();
      float maxV = this.getV1();
      int brightness = ShadersModHelper.isShaderPackInUse() ? LightTexture.pack(12, 15) : this.getLightColor(partialTicks);
      builder.addVertex(vertices[0].x(), vertices[0].y(), vertices[0].z())
         .setUv(maxU, maxV)
         .setColor(this.rCol, this.gCol, this.bCol, this.alpha)
         .setLight(brightness);
      builder.addVertex(vertices[1].x(), vertices[1].y(), vertices[1].z())
         .setUv(maxU, minV)
         .setColor(this.rCol, this.gCol, this.bCol, this.alpha)
         .setLight(brightness);
      builder.addVertex(vertices[2].x(), vertices[2].y(), vertices[2].z())
         .setUv(minU, minV)
         .setColor(this.rCol, this.gCol, this.bCol, this.alpha)
         .setLight(brightness);
      builder.addVertex(vertices[3].x(), vertices[3].y(), vertices[3].z())
         .setUv(minU, maxV)
         .setColor(this.rCol, this.gCol, this.bCol, this.alpha)
         .setLight(brightness);
   }
}
