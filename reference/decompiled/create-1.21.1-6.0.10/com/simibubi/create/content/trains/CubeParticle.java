package com.simibubi.create.content.trains;

import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import net.createmod.ponder.enums.PonderSpecialTextures;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class CubeParticle extends Particle {
   public static final Vec3[] CUBE = new Vec3[]{
      new Vec3(1.0, 1.0, -1.0),
      new Vec3(1.0, 1.0, 1.0),
      new Vec3(-1.0, 1.0, 1.0),
      new Vec3(-1.0, 1.0, -1.0),
      new Vec3(-1.0, -1.0, -1.0),
      new Vec3(-1.0, -1.0, 1.0),
      new Vec3(1.0, -1.0, 1.0),
      new Vec3(1.0, -1.0, -1.0),
      new Vec3(-1.0, -1.0, 1.0),
      new Vec3(-1.0, 1.0, 1.0),
      new Vec3(1.0, 1.0, 1.0),
      new Vec3(1.0, -1.0, 1.0),
      new Vec3(1.0, -1.0, -1.0),
      new Vec3(1.0, 1.0, -1.0),
      new Vec3(-1.0, 1.0, -1.0),
      new Vec3(-1.0, -1.0, -1.0),
      new Vec3(-1.0, -1.0, -1.0),
      new Vec3(-1.0, 1.0, -1.0),
      new Vec3(-1.0, 1.0, 1.0),
      new Vec3(-1.0, -1.0, 1.0),
      new Vec3(1.0, -1.0, 1.0),
      new Vec3(1.0, 1.0, 1.0),
      new Vec3(1.0, 1.0, -1.0),
      new Vec3(1.0, -1.0, -1.0)
   };
   private static final ParticleRenderType RENDER_TYPE = new ParticleRenderType() {
      @NotNull
      public BufferBuilder begin(Tesselator tesselator, TextureManager textureManager) {
         PonderSpecialTextures.BLANK.bind();
         RenderSystem.depthMask(false);
         RenderSystem.enableBlend();
         RenderSystem.blendFunc(SourceFactor.ONE, DestFactor.ONE);
         BufferBuilder builder = tesselator.begin(Mode.QUADS, DefaultVertexFormat.PARTICLE);
         RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
         return builder;
      }
   };
   protected float scale;
   protected boolean hot;
   private boolean billowing = false;

   public CubeParticle(ClientLevel world, double x, double y, double z, double motionX, double motionY, double motionZ) {
      super(world, x, y, z);
      this.xd = motionX;
      this.yd = motionY;
      this.zd = motionZ;
      this.setScale(0.2F);
   }

   public void setScale(float scale) {
      this.scale = scale;
      this.setSize(scale * 0.5F, scale * 0.5F);
   }

   public void averageAge(int age) {
      this.lifetime = (int)((double)age + (this.random.nextDouble() * 2.0 - 1.0) * 8.0);
   }

   public void setHot(boolean hot) {
      this.hot = hot;
   }

   public void tick() {
      if (this.hot && this.age > 0) {
         if (this.yo == this.y) {
            this.billowing = true;
            this.stoppedByCollision = false;
            if (this.xd == 0.0 && this.zd == 0.0) {
               Vec3 diff = Vec3.atLowerCornerOf(BlockPos.containing(this.x, this.y, this.z)).add(0.5, 0.5, 0.5).subtract(this.x, this.y, this.z);
               this.xd = -diff.x * 0.1;
               this.zd = -diff.z * 0.1;
            }

            this.xd *= 1.1;
            this.yd *= 0.9;
            this.zd *= 1.1;
         } else if (this.billowing) {
            this.yd *= 1.2;
         }
      }

      super.tick();
   }

   public void render(VertexConsumer builder, Camera renderInfo, float p_225606_3_) {
      Vec3 projectedView = renderInfo.getPosition();
      float lerpedX = (float)(Mth.lerp((double)p_225606_3_, this.xo, this.x) - projectedView.x());
      float lerpedY = (float)(Mth.lerp((double)p_225606_3_, this.yo, this.y) - projectedView.y());
      float lerpedZ = (float)(Mth.lerp((double)p_225606_3_, this.zo, this.z) - projectedView.z());
      int light = 15728880;
      double ageMultiplier = 1.0
         - Math.pow((double)Mth.clamp((float)this.age + p_225606_3_, 0.0F, (float)this.lifetime), 3.0) / Math.pow((double)this.lifetime, 3.0);

      for (int i = 0; i < 6; i++) {
         for (int j = 0; j < 4; j++) {
            Vec3 vec = CUBE[i * 4 + j].scale(-1.0);
            vec = vec.scale((double)this.scale * ageMultiplier).add((double)lerpedX, (double)lerpedY, (double)lerpedZ);
            builder.addVertex((float)vec.x, (float)vec.y, (float)vec.z)
               .setUv((float)j / 2.0F, (float)(j % 2))
               .setColor(this.rCol, this.gCol, this.bCol, this.alpha)
               .setLight(light);
         }
      }
   }

   public ParticleRenderType getRenderType() {
      return RENDER_TYPE;
   }

   public static class Factory implements ParticleProvider<CubeParticleData> {
      public Particle createParticle(CubeParticleData data, ClientLevel world, double x, double y, double z, double motionX, double motionY, double motionZ) {
         CubeParticle particle = new CubeParticle(world, x, y, z, motionX, motionY, motionZ);
         particle.setColor(data.r, data.g, data.b);
         particle.setScale(data.scale);
         particle.averageAge(data.avgAge);
         particle.setHot(data.hot);
         return particle;
      }
   }
}
