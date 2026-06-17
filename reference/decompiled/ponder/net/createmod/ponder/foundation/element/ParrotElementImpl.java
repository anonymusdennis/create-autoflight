package net.createmod.ponder.foundation.element;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.ponder.api.element.ParrotElement;
import net.createmod.ponder.api.element.ParrotPose;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.foundation.PonderScene;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.phys.Vec3;

public class ParrotElementImpl extends AnimatedSceneElementBase implements ParrotElement {
   protected Vec3 location;
   @Nullable
   protected Parrot entity;
   protected ParrotPose pose;
   protected Supplier<? extends ParrotPose> initialPose;

   public static ParrotElement create(Vec3 location, Supplier<? extends ParrotPose> pose) {
      return new ParrotElementImpl(location, pose);
   }

   protected ParrotElementImpl(Vec3 location, Supplier<? extends ParrotPose> pose) {
      this.location = location;
      this.initialPose = pose;
      this.pose = this.initialPose.get();
   }

   @Override
   public void reset(PonderScene scene) {
      super.reset(scene);
      this.setPose(this.initialPose.get());
      this.entity.setPosRaw(0.0, 0.0, 0.0);
      this.entity.xo = 0.0;
      this.entity.yo = 0.0;
      this.entity.zo = 0.0;
      this.entity.xOld = 0.0;
      this.entity.yOld = 0.0;
      this.entity.zOld = 0.0;
      this.entity.setXRot(this.entity.xRotO = 0.0F);
      this.entity.setYRot(this.entity.yRotO = 180.0F);
   }

   @Override
   public void tick(PonderScene scene) {
      super.tick(scene);
      if (this.entity == null) {
         this.entity = this.pose.create(scene.getWorld());
         this.entity.setYRot(this.entity.yRotO = 180.0F);
      }

      this.entity.tickCount++;
      this.entity.yHeadRotO = this.entity.yHeadRot;
      this.entity.oFlapSpeed = this.entity.flapSpeed;
      this.entity.oFlap = this.entity.flap;
      this.entity.setOnGround(true);
      this.entity.xo = this.entity.getX();
      this.entity.yo = this.entity.getY();
      this.entity.zo = this.entity.getZ();
      this.entity.yRotO = this.entity.getYRot();
      this.entity.xRotO = this.entity.getXRot();
      this.pose.tick(scene, this.entity, this.location);
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
   public void setRotation(Vec3 eulers, boolean immediate) {
      if (this.entity != null) {
         this.entity.setXRot((float)eulers.x);
         this.entity.setYRot((float)eulers.y);
         if (immediate) {
            this.entity.xRotO = this.entity.getXRot();
            this.entity.yRotO = this.entity.getYRot();
         }
      }
   }

   @Override
   public Vec3 getPositionOffset() {
      return this.entity != null ? this.entity.position() : Vec3.ZERO;
   }

   @Override
   public Vec3 getRotation() {
      return this.entity != null ? new Vec3((double)this.entity.getXRot(), (double)this.entity.getYRot(), 0.0) : Vec3.ZERO;
   }

   @Override
   protected void renderLast(PonderLevel world, MultiBufferSource buffer, GuiGraphics graphics, float fade, float pt) {
      PoseStack poseStack = graphics.pose();
      EntityRenderDispatcher entityrenderermanager = Minecraft.getInstance().getEntityRenderDispatcher();
      if (this.entity == null) {
         this.entity = this.pose.create(world);
         this.entity.setYRot(this.entity.yRotO = 180.0F);
      }

      poseStack.pushPose();
      poseStack.translate(this.location.x, this.location.y, this.location.z);
      poseStack.translate(
         Mth.lerp((double)pt, this.entity.xo, this.entity.getX()),
         Mth.lerp((double)pt, this.entity.yo, this.entity.getY()),
         Mth.lerp((double)pt, this.entity.zo, this.entity.getZ())
      );
      float angle = AngleHelper.angleLerp((double)pt, (double)this.entity.yRotO, (double)this.entity.getYRot());
      poseStack.mulPose(Axis.YP.rotationDegrees(angle));
      entityrenderermanager.render(this.entity, 0.0, 0.0, 0.0, 0.0F, pt, poseStack, buffer, this.lightCoordsFromFade(fade));
      poseStack.popPose();
   }

   @Override
   public void setPose(ParrotPose pose) {
      this.pose = pose;
   }
}
