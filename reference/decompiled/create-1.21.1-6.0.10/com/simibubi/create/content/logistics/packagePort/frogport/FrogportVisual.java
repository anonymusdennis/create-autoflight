package com.simibubi.create.content.logistics.packagePort.frogport;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.logistics.packagePort.PackagePortBlockEntity;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.DynamicVisual.Context;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class FrogportVisual extends AbstractBlockEntityVisual<FrogportBlockEntity> implements SimpleDynamicVisual {
   private final TransformedInstance body;
   private TransformedInstance head;
   private final TransformedInstance tongue;
   private final TransformedInstance rig;
   private final TransformedInstance box;
   private final Matrix4f basePose = new Matrix4f();
   private float lastYaw = Float.NaN;
   private float lastHeadPitch = Float.NaN;
   private float lastTonguePitch = Float.NaN;
   private float lastTongueLength = Float.NaN;
   private boolean lastGoggles = false;

   public FrogportVisual(VisualizationContext ctx, FrogportBlockEntity blockEntity, float partialTick) {
      super(ctx, blockEntity, partialTick);
      this.body = (TransformedInstance)ctx.instancerProvider()
         .instancer(InstanceTypes.TRANSFORMED, Models.partial(AllPartialModels.FROGPORT_BODY))
         .createInstance();
      this.head = (TransformedInstance)ctx.instancerProvider()
         .instancer(InstanceTypes.TRANSFORMED, Models.partial(AllPartialModels.FROGPORT_HEAD))
         .createInstance();
      this.tongue = (TransformedInstance)ctx.instancerProvider()
         .instancer(InstanceTypes.TRANSFORMED, Models.partial(AllPartialModels.FROGPORT_TONGUE))
         .createInstance();
      this.rig = (TransformedInstance)ctx.instancerProvider()
         .instancer(InstanceTypes.TRANSFORMED, Models.block(Blocks.AIR.defaultBlockState()))
         .createInstance();
      this.box = (TransformedInstance)ctx.instancerProvider()
         .instancer(InstanceTypes.TRANSFORMED, Models.block(Blocks.AIR.defaultBlockState()))
         .createInstance();
      this.rig.handle().setVisible(false);
      this.box.handle().setVisible(false);
      this.animate(partialTick);
   }

   public void beginFrame(Context ctx) {
      this.animate(ctx.partialTick());
   }

   private void animate(float partialTicks) {
      this.updateGoggles();
      float yaw = ((FrogportBlockEntity)this.blockEntity).getYaw();
      float headPitch = 80.0F;
      float tonguePitch = 0.0F;
      float tongueLength = 0.0F;
      float headPitchModifier = 1.0F;
      boolean hasTarget = ((FrogportBlockEntity)this.blockEntity).target != null;
      boolean animating = ((FrogportBlockEntity)this.blockEntity).isAnimationInProgress();
      boolean depositing = ((FrogportBlockEntity)this.blockEntity).currentlyDepositing;
      Vec3 diff = Vec3.ZERO;
      if (hasTarget) {
         diff = ((FrogportBlockEntity)this.blockEntity)
            .target
            .getExactTargetLocation(
               (PackagePortBlockEntity)this.blockEntity,
               ((FrogportBlockEntity)this.blockEntity).getLevel(),
               ((FrogportBlockEntity)this.blockEntity).getBlockPos()
            )
            .subtract(0.0, animating && depositing ? 0.0 : 0.75, 0.0)
            .subtract(Vec3.atCenterOf(((FrogportBlockEntity)this.blockEntity).getBlockPos()));
         tonguePitch = (float)Mth.atan2(diff.y, diff.multiply(1.0, 0.0, 1.0).length() + 0.1875) * (180.0F / (float)Math.PI);
         tongueLength = Math.max((float)diff.length(), 1.0F);
         headPitch = Mth.clamp(tonguePitch * 2.0F, 60.0F, 100.0F);
      }

      if (animating) {
         float progress = ((FrogportBlockEntity)this.blockEntity).animationProgress.getValue(partialTicks);
         float scale = 1.0F;
         float itemDistance = 0.0F;
         if (depositing) {
            double modifier = Math.max(0.0, 1.0 - Math.pow(((double)progress - 0.25) * 4.0 - 1.0, 4.0));
            itemDistance = (float)Math.max((double)tongueLength * Math.min(1.0, ((double)progress - 0.25) * 3.0), (double)tongueLength * modifier);
            tongueLength = (float)((double)tongueLength * Math.max(0.0, 1.0 - Math.pow(((double)progress * 1.25 - 0.25) * 4.0 - 1.0, 4.0)));
            headPitchModifier = (float)Math.max(0.0, 1.0 - Math.pow((double)progress * 1.25 * 2.0 - 1.0, 4.0));
            scale = 0.25F + progress * 3.0F / 4.0F;
         } else {
            tongueLength = (float)((double)tongueLength * Math.pow(Math.max(0.0, 1.0 - (double)progress * 1.25), 5.0));
            headPitchModifier = 1.0F - (float)Math.min(1.0, Math.max(0.0, (Math.pow((double)progress * 1.5, 2.0) - 0.5) * 2.0));
            scale = (float)Math.max(0.5, 1.0 - (double)progress * 1.25);
            itemDistance = tongueLength;
         }

         this.renderPackage(diff, scale, itemDistance);
      } else {
         tongueLength = 0.0F;
         float anticipation = ((FrogportBlockEntity)this.blockEntity).anticipationProgress.getValue(partialTicks);
         headPitchModifier = anticipation > 0.0F ? (float)Math.max(0.0, 1.0 - Math.pow((double)anticipation * 1.25 * 2.0 - 1.0, 4.0)) : 0.0F;
         this.rig.handle().setVisible(false);
         this.box.handle().setVisible(false);
      }

      headPitch *= headPitchModifier;
      headPitch = Math.max(headPitch, ((FrogportBlockEntity)this.blockEntity).manualOpenAnimationProgress.getValue(partialTicks) * 60.0F);
      tongueLength = Math.max(tongueLength, ((FrogportBlockEntity)this.blockEntity).manualOpenAnimationProgress.getValue(partialTicks) * 0.25F);
      if (yaw != this.lastYaw) {
         ((TransformedInstance)((TransformedInstance)((TransformedInstance)((TransformedInstance)this.body
                        .setIdentityTransform()
                        .translate(this.getVisualPosition()))
                     .center())
                  .rotateYDegrees(yaw))
               .uncenter())
            .setChanged();
         this.basePose.set(this.body.pose).translate(0.5F, 0.625F, 0.6875F);
         this.lastYaw = yaw;
         this.lastTonguePitch = Float.NaN;
         this.lastHeadPitch = Float.NaN;
      }

      if (headPitch != this.lastHeadPitch) {
         ((TransformedInstance)((TransformedInstance)this.head.setTransform(this.basePose).rotateXDegrees(headPitch)).translateBack(0.5F, 0.625F, 0.6875F))
            .setChanged();
         this.lastHeadPitch = headPitch;
      }

      if (tonguePitch != this.lastTonguePitch || tongueLength != this.lastTongueLength) {
         ((TransformedInstance)((TransformedInstance)this.tongue.setTransform(this.basePose).rotateXDegrees(tonguePitch))
               .scale(1.0F, 1.0F, tongueLength / 0.4375F)
               .translateBack(0.5F, 0.625F, 0.6875F))
            .setChanged();
         this.lastTonguePitch = tonguePitch;
         this.lastTongueLength = tongueLength;
      }
   }

   public void updateGoggles() {
      if (((FrogportBlockEntity)this.blockEntity).goggles && !this.lastGoggles) {
         this.head.delete();
         this.head = (TransformedInstance)this.instancerProvider()
            .instancer(InstanceTypes.TRANSFORMED, Models.partial(AllPartialModels.FROGPORT_HEAD_GOGGLES))
            .createInstance();
         this.lastHeadPitch = -1.0F;
         this.updateLight(0.0F);
         this.lastGoggles = true;
      }

      if (!((FrogportBlockEntity)this.blockEntity).goggles && this.lastGoggles) {
         this.head.delete();
         this.head = (TransformedInstance)this.instancerProvider()
            .instancer(InstanceTypes.TRANSFORMED, Models.partial(AllPartialModels.FROGPORT_HEAD))
            .createInstance();
         this.lastHeadPitch = -1.0F;
         this.updateLight(0.0F);
         this.lastGoggles = false;
      }
   }

   private void renderPackage(Vec3 diff, float scale, float itemDistance) {
      if (((FrogportBlockEntity)this.blockEntity).animatedPackage != null && !((double)scale < 0.45)) {
         ResourceLocation key = BuiltInRegistries.ITEM.getKey(((FrogportBlockEntity)this.blockEntity).animatedPackage.getItem());
         if (key == BuiltInRegistries.ITEM.getDefaultKey()) {
            this.rig.handle().setVisible(false);
            this.box.handle().setVisible(false);
         } else {
            boolean animating = ((FrogportBlockEntity)this.blockEntity).isAnimationInProgress();
            boolean depositing = ((FrogportBlockEntity)this.blockEntity).currentlyDepositing;
            this.instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(AllPartialModels.PACKAGES.get(key))).stealInstance(this.box);
            this.box.handle().setVisible(true);
            ((TransformedInstance)((TransformedInstance)((TransformedInstance)((TransformedInstance)((TransformedInstance)this.box
                              .setIdentityTransform()
                              .translate(this.getVisualPosition()))
                           .translate(0.0F, 0.1875F, 0.0F)
                           .translate(diff.normalize().scale((double)itemDistance).subtract(0.0, animating && depositing ? 0.75 : 0.0, 0.0)))
                        .center())
                     .scale(scale))
                  .uncenter())
               .setChanged();
            if (!depositing) {
               this.rig.handle().setVisible(false);
            } else {
               this.instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(AllPartialModels.PACKAGE_RIGGING.get(key))).stealInstance(this.rig);
               this.rig.handle().setVisible(true);
               this.rig.pose.set(this.box.pose);
               this.rig.setChanged();
            }
         }
      } else {
         this.rig.handle().setVisible(false);
         this.box.handle().setVisible(false);
      }
   }

   public void collectCrumblingInstances(Consumer<Instance> consumer) {
      consumer.accept(this.body);
      consumer.accept(this.head);
   }

   public void updateLight(float partialTick) {
      this.relight(new FlatLit[]{this.body, this.head, this.tongue, this.rig, this.box});
   }

   protected void _delete() {
      this.body.delete();
      this.head.delete();
      this.tongue.delete();
      this.rig.delete();
      this.box.delete();
   }
}
