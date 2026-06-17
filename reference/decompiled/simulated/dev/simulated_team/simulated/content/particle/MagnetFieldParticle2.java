package dev.simulated_team.simulated.content.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class MagnetFieldParticle2 extends SimpleAnimatedParticle {
   protected int timeUntilEnd;

   protected MagnetFieldParticle2(
      ClientLevel world,
      double x,
      double y,
      double z,
      double prevX,
      double prevY,
      double prevZ,
      double nextX,
      double nextY,
      double nextZ,
      SpriteSet sprite,
      boolean negative,
      int timeUntilEnd
   ) {
      super(world, x, y, z, sprite, world.random.nextFloat() * 0.5F);
      this.hasPhysics = false;
      this.lifetime = 5;
      this.xo = prevX;
      this.yo = prevY;
      this.zo = prevZ;
      this.x = x;
      this.y = y;
      this.z = z;
      this.xd = nextX;
      this.yd = nextY;
      this.zd = nextZ;
      this.timeUntilEnd = timeUntilEnd;
      this.selectSprite(0);
      this.setAlpha(0.4F);
      if (negative) {
         this.setColor(0.7F, 0.7F, 1.0F);
      } else {
         this.setColor(1.0F, 0.7F, 0.7F);
      }
   }

   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
   }

   private void dissipate() {
      this.remove();
   }

   public void render(VertexConsumer buffer, Camera renderInfo, float partialTicks) {
      Quaternionf quaternionf = new Quaternionf();
      this.getFacingCameraMode().setRotation(quaternionf, renderInfo, partialTicks);
      if (this.roll != 0.0F) {
         quaternionf.rotateZ(Mth.lerp(partialTicks, this.oRoll, this.roll));
      }

      new Vector3f(1.0F, 1.0F, 1.0F);
      float t = (float)(Minecraft.getInstance().level.getGameTime() % 1000L) + partialTicks;
      t = (float)((double)t * 0.2);
      new Vector3f();
      Vec3 vec3 = renderInfo.getPosition();
      float x = (float)(this.x - vec3.x());
      float y = (float)(this.y - vec3.y());
      float z = (float)(this.z - vec3.z());
      float dirX = (float)Mth.lerp((double)partialTicks, this.xo, this.xd);
      float dirY = (float)Mth.lerp((double)partialTicks, this.yo, this.yd);
      float dirZ = (float)Mth.lerp((double)partialTicks, this.zo, this.zd);
      float offsetX = (float)Mth.lerp((double)partialTicks, -this.xo, this.xd) * 0.5F;
      float offsetY = (float)Mth.lerp((double)partialTicks, -this.yo, this.yd) * 0.5F;
      float offsetZ = (float)Mth.lerp((double)partialTicks, -this.zo, this.zd) * 0.5F;
      quaternionf.identity();
      quaternionf.lookAlong(new Vector3f(dirX, dirY, dirZ), new Vector3f(x, y, z)).conjugate();
      quaternionf.rotateX((float) (Math.PI / 2));
      this.renderRotatedQuad(buffer, quaternionf, x + offsetX, y + offsetY, z + offsetZ, partialTicks);
   }

   public float getQuadSize(float scaleFactor) {
      float x = (float)Mth.lerp((double)scaleFactor, this.xo, this.xd);
      float y = (float)Mth.lerp((double)scaleFactor, this.yo, this.yd);
      float z = (float)Mth.lerp((double)scaleFactor, this.zo, this.zd);
      return (float)Mth.length((double)x, (double)y, (double)z) * 0.5F;
   }

   public void tick() {
      if (this.age++ >= this.lifetime) {
         this.remove();
      } else {
         this.selectSprite(this.age + 1);
      }
   }

   public int getLightColor(float partialTick) {
      BlockPos blockpos = new BlockPos((int)this.x, (int)this.y, (int)this.z);
      return this.level.isLoaded(blockpos) ? LevelRenderer.getLightColor(this.level, blockpos) : 0;
   }

   private void selectSprite(int index) {
      int n = 6;
      int clampedIndex = 2 * index < 6 ? Math.min(index, this.timeUntilEnd) : Math.max(index, 6 - this.timeUntilEnd + 1);
      this.setSprite(this.sprites.get(clampedIndex, 6));
   }

   public static class Factory implements ParticleProvider<MagnetFieldParticleData2> {
      private final SpriteSet spriteSet;

      public Factory(SpriteSet animatedSprite) {
         this.spriteSet = animatedSprite;
      }

      public Particle createParticle(
         MagnetFieldParticleData2 data, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed
      ) {
         return new MagnetFieldParticle2(
            level,
            x,
            y,
            z,
            data.previousOffset.x,
            data.previousOffset.y,
            data.previousOffset.z,
            data.nextOffset.x,
            data.nextOffset.y,
            data.nextOffset.z,
            this.spriteSet,
            data.isNegative(),
            data.getTimeUntilEnd()
         );
      }
   }
}
