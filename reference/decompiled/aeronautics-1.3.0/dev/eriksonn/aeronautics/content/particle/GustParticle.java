package dev.eriksonn.aeronautics.content.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ryanhcode.sable.api.particle.ParticleSubLevelKickable;
import dev.ryanhcode.sable.mixinterface.particle.ParticleExtension;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import org.joml.Quaterniondc;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class GustParticle extends TextureSheetParticle implements ParticleSubLevelKickable {
   private static final float FPS = 16.0F;
   private static final float FRAMES = 8.0F;
   private final Quaternionf orientation;
   private final Quaternionf renderOrientation = new Quaternionf();
   private final Quaternionf subLevelOrientation = new Quaternionf();

   protected GustParticle(ClientLevel level, double x, double y, double z, Quaternionf orientation) {
      super(level, x, y, z);
      this.orientation = orientation.normalize();
      this.quadSize = 2.0F;
      this.lifetime = 9;
      this.alpha = 0.25F;
   }

   public void render(VertexConsumer buffer, Camera renderInfo, float partialTicks) {
      this.renderOrientation.set(this.orientation);
      ParticleExtension extension = (ParticleExtension)this;
      if (extension.sable$getTrackingSubLevel() instanceof ClientSubLevel subLevel) {
         Quaterniondc orientation1 = subLevel.renderPose().orientation();
         this.renderOrientation.premul(this.subLevelOrientation.set(orientation1));
      }

      this.renderRotatedQuad(buffer, renderInfo, this.renderOrientation, partialTicks);
   }

   protected void renderRotatedQuad(VertexConsumer buffer, Quaternionf quaternion, float x, float y, float z, float partialTicks) {
      float f = this.getQuadSize(partialTicks);
      float f1 = this.getU0();
      float f2 = this.getU1();
      float f3 = this.getV0();
      float f4 = this.getV1();
      int i = this.getLightColor(partialTicks);
      this.renderVertex(buffer, quaternion, x, y, z, 1.0F, -1.0F, f, f2, f4, i);
      this.renderVertex(buffer, quaternion, x, y, z, 1.0F, 1.0F, f, f2, f3, i);
      this.renderVertex(buffer, quaternion, x, y, z, -1.0F, 1.0F, f, f1, f3, i);
      this.renderVertex(buffer, quaternion, x, y, z, -1.0F, -1.0F, f, f1, f4, i);
      this.renderVertex(buffer, quaternion, x, y, z, -1.0F, -1.0F, f, f1, f4, i);
      this.renderVertex(buffer, quaternion, x, y, z, -1.0F, 1.0F, f, f1, f3, i);
      this.renderVertex(buffer, quaternion, x, y, z, 1.0F, 1.0F, f, f2, f3, i);
      this.renderVertex(buffer, quaternion, x, y, z, 1.0F, -1.0F, f, f2, f4, i);
   }

   private void renderVertex(
      VertexConsumer buffer, Quaternionf quaternion, float x, float y, float z, float xOffset, float yOffset, float quadSize, float u, float v, int packedLight
   ) {
      Vector3f vector3f = new Vector3f(xOffset, yOffset, 0.0F).rotate(quaternion).mul(quadSize).add(x, y, z);
      buffer.addVertex(vector3f.x(), vector3f.y(), vector3f.z()).setUv(u, v).setColor(this.rCol, this.gCol, this.bCol, this.alpha).setLight(packedLight);
   }

   protected float getU0() {
      float offset = this.getFrameOffset(this.getFrame());
      return super.getU0() + offset;
   }

   protected float getU1() {
      float offset = this.getFrameOffset(this.getFrame() + 1);
      return super.getU0() + offset;
   }

   private float getFrameOffset(int frame) {
      float width = this.sprite.getU1() - this.sprite.getU0();
      float frameWidth = width / 8.0F;
      return frameWidth * (float)frame;
   }

   private int getFrame() {
      float age = (float)this.age / 20.0F;
      return (int)(age * 16.0F);
   }

   public void pickSprite(SpriteSet sprite) {
      super.pickSprite(sprite);
   }

   public void tick() {
      super.tick();
   }

   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
   }

   public boolean sable$shouldKickFromTracking() {
      return false;
   }

   public boolean sable$shouldCollideWithTrackingSubLevel() {
      return false;
   }

   public static class Factory implements ParticleProvider<GustParticleData> {
      private final SpriteSet spriteSet;

      public Factory(SpriteSet animatedSprite) {
         this.spriteSet = animatedSprite;
      }

      public Particle createParticle(GustParticleData data, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
         GustParticle particle = new GustParticle(worldIn, x, y, z, data.orientation());
         particle.setSprite(this.spriteSet.get(worldIn.random));
         return particle;
      }
   }
}
