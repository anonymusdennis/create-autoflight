package net.createmod.ponder.foundation.element;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import javax.annotation.Nullable;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.ponder.api.element.MinecartElement;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.foundation.PonderScene;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.phys.Vec3;

public class MinecartElementImpl extends AnimatedSceneElementBase implements MinecartElement {
   private final Vec3 location;
   private final LerpedFloat rotation;
   @Nullable
   private AbstractMinecart entity;
   private final MinecartElement.MinecartConstructor constructor;
   private final float initialRotation;

   public MinecartElementImpl(Vec3 location, float rotation, MinecartElement.MinecartConstructor constructor) {
      this.initialRotation = rotation;
      this.location = location.add(0.0, 0.0625, 0.0);
      this.constructor = constructor;
      this.rotation = LerpedFloat.angular().startWithValue((double)rotation);
   }

   @Override
   public void reset(PonderScene scene) {
      super.reset(scene);
      this.entity.setPosRaw(0.0, 0.0, 0.0);
      this.entity.xo = 0.0;
      this.entity.yo = 0.0;
      this.entity.zo = 0.0;
      this.entity.xOld = 0.0;
      this.entity.yOld = 0.0;
      this.entity.zOld = 0.0;
      this.rotation.startWithValue((double)this.initialRotation);
   }

   @Override
   public void tick(PonderScene scene) {
      super.tick(scene);
      if (this.entity == null) {
         this.entity = this.constructor.create(scene.getWorld(), 0.0, 0.0, 0.0);
      }

      this.entity.tickCount++;
      this.entity.setOnGround(true);
      this.entity.xo = this.entity.getX();
      this.entity.yo = this.entity.getY();
      this.entity.zo = this.entity.getZ();
      this.entity.xOld = this.entity.getX();
      this.entity.yOld = this.entity.getY();
      this.entity.zOld = this.entity.getZ();
   }

   @Override
   public void setPositionOffset(Vec3 position, boolean immediate) {
      if (this.entity != null) {
         this.entity.setPos(position.x, position.y, position.z);
         if (immediate) {
            this.entity.xo = position.x;
            this.entity.yo = position.y;
            this.entity.zo = position.z;
         }
      }
   }

   @Override
   public void setRotation(float angle, boolean immediate) {
      if (this.entity != null) {
         this.rotation.setValue((double)angle);
         if (immediate) {
            this.rotation.startWithValue((double)angle);
         }
      }
   }

   @Override
   public Vec3 getPositionOffset() {
      return this.entity != null ? this.entity.position() : Vec3.ZERO;
   }

   @Override
   public Vec3 getRotation() {
      return new Vec3(0.0, (double)this.rotation.getValue(), 0.0);
   }

   @Override
   public void renderLast(PonderLevel world, MultiBufferSource buffer, GuiGraphics graphics, float fade, float pt) {
      PoseStack poseStack = graphics.pose();
      EntityRenderDispatcher entityrenderermanager = Minecraft.getInstance().getEntityRenderDispatcher();
      if (this.entity == null) {
         this.entity = this.constructor.create(world, 0.0, 0.0, 0.0);
      }

      poseStack.pushPose();
      poseStack.translate(this.location.x, this.location.y, this.location.z);
      poseStack.translate(
         Mth.lerp((double)pt, this.entity.xo, this.entity.getX()),
         Mth.lerp((double)pt, this.entity.yo, this.entity.getY()),
         Mth.lerp((double)pt, this.entity.zo, this.entity.getZ())
      );
      poseStack.mulPose(Axis.YP.rotationDegrees(this.rotation.getValue(pt)));
      entityrenderermanager.render(this.entity, 0.0, 0.0, 0.0, 0.0F, pt, poseStack, buffer, this.lightCoordsFromFade(fade));
      poseStack.popPose();
   }
}
