package com.simibubi.create.content.kinetics.mechanicalArm;

import com.google.common.collect.Lists;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.DynamicVisual.Context;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.AbstractInstance;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.engine_room.flywheel.lib.util.RecyclingPoseStack;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import java.util.ArrayList;
import java.util.function.Consumer;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

public class ArmVisual extends SingleAxisRotatingVisual<ArmBlockEntity> implements SimpleDynamicVisual {
   final TransformedInstance base;
   final TransformedInstance lowerBody;
   final TransformedInstance upperBody;
   final TransformedInstance claw;
   private final ArrayList<TransformedInstance> clawGrips;
   private final ArrayList<TransformedInstance> models;
   private final boolean ceiling;
   private final RecyclingPoseStack poseStack = new RecyclingPoseStack();
   private boolean wasDancing = false;
   private float baseAngle = Float.NaN;
   private float lowerArmAngle = Float.NaN;
   private float upperArmAngle = Float.NaN;
   private float headAngle = Float.NaN;

   public ArmVisual(VisualizationContext context, ArmBlockEntity blockEntity, float partialTick) {
      super(context, blockEntity, partialTick, Models.partial(AllPartialModels.ARM_COG));
      this.base = (TransformedInstance)this.instancerProvider()
         .instancer(InstanceTypes.TRANSFORMED, Models.partial(AllPartialModels.ARM_BASE))
         .createInstance();
      this.lowerBody = (TransformedInstance)this.instancerProvider()
         .instancer(InstanceTypes.TRANSFORMED, Models.partial(AllPartialModels.ARM_LOWER_BODY))
         .createInstance();
      this.upperBody = (TransformedInstance)this.instancerProvider()
         .instancer(InstanceTypes.TRANSFORMED, Models.partial(AllPartialModels.ARM_UPPER_BODY))
         .createInstance();
      this.claw = (TransformedInstance)this.instancerProvider()
         .instancer(InstanceTypes.TRANSFORMED, Models.partial(blockEntity.goggles ? AllPartialModels.ARM_CLAW_BASE_GOGGLES : AllPartialModels.ARM_CLAW_BASE))
         .createInstance();
      TransformedInstance clawGrip1 = (TransformedInstance)this.instancerProvider()
         .instancer(InstanceTypes.TRANSFORMED, Models.partial(AllPartialModels.ARM_CLAW_GRIP_UPPER))
         .createInstance();
      TransformedInstance clawGrip2 = (TransformedInstance)this.instancerProvider()
         .instancer(InstanceTypes.TRANSFORMED, Models.partial(AllPartialModels.ARM_CLAW_GRIP_LOWER))
         .createInstance();
      this.clawGrips = Lists.newArrayList(new TransformedInstance[]{clawGrip1, clawGrip2});
      this.models = Lists.newArrayList(new TransformedInstance[]{this.base, this.lowerBody, this.upperBody, this.claw, clawGrip1, clawGrip2});
      this.ceiling = (Boolean)this.blockState.getValue(ArmBlock.CEILING);
      PoseTransformStack msr = TransformStack.of(this.poseStack);
      msr.translate(this.getVisualPosition());
      msr.center();
      if (this.ceiling) {
         msr.rotateXDegrees(180.0F);
      }

      this.animate(partialTick);
   }

   public void beginFrame(Context ctx) {
      this.animate(ctx.partialTick());
   }

   private void animate(float pt) {
      if (((ArmBlockEntity)this.blockEntity).phase == ArmBlockEntity.Phase.DANCING && ((ArmBlockEntity)this.blockEntity).getSpeed() != 0.0F) {
         this.animateRave(pt);
         this.wasDancing = true;
      } else {
         float baseAngleNow = ((ArmBlockEntity)this.blockEntity).baseAngle.getValue(pt);
         float lowerArmAngleNow = ((ArmBlockEntity)this.blockEntity).lowerArmAngle.getValue(pt);
         float upperArmAngleNow = ((ArmBlockEntity)this.blockEntity).upperArmAngle.getValue(pt);
         float headAngleNow = ((ArmBlockEntity)this.blockEntity).headAngle.getValue(pt);
         boolean settled = Mth.equal(this.baseAngle, baseAngleNow)
            && Mth.equal(this.lowerArmAngle, lowerArmAngleNow)
            && Mth.equal(this.upperArmAngle, upperArmAngleNow)
            && Mth.equal(this.headAngle, headAngleNow);
         this.baseAngle = baseAngleNow;
         this.lowerArmAngle = lowerArmAngleNow;
         this.upperArmAngle = upperArmAngleNow;
         this.headAngle = headAngleNow;
         if (!settled || this.wasDancing) {
            this.animateArm();
         }

         this.wasDancing = false;
      }
   }

   private void animateRave(float partialTick) {
      int ticks = AnimationTickHolder.getTicks(((ArmBlockEntity)this.blockEntity).getLevel());
      float renderTick = (float)ticks + partialTick + (float)(((ArmBlockEntity)this.blockEntity).hashCode() % 64);
      float baseAngle = renderTick * 10.0F % 360.0F;
      float lowerArmAngle = Mth.lerp((Mth.sin(renderTick / 4.0F) + 1.0F) / 2.0F, -45.0F, 15.0F);
      float upperArmAngle = Mth.lerp((Mth.sin(renderTick / 8.0F) + 1.0F) / 4.0F, -45.0F, 95.0F);
      float headAngle = -lowerArmAngle;
      int color = Color.rainbowColor(ticks * 100).getRGB();
      this.updateAngles(baseAngle, lowerArmAngle, upperArmAngle, headAngle, color);
   }

   private void animateArm() {
      this.updateAngles(this.baseAngle, this.lowerArmAngle - 135.0F, this.upperArmAngle - 90.0F, this.headAngle, 16777215);
   }

   private void updateAngles(float baseAngle, float lowerArmAngle, float upperArmAngle, float headAngle, int color) {
      this.poseStack.pushPose();
      PoseTransformStack msr = TransformStack.of(this.poseStack);
      ArmRenderer.transformBase(msr, baseAngle);
      this.base.setTransform(this.poseStack).setChanged();
      ArmRenderer.transformLowerArm(msr, lowerArmAngle);
      this.lowerBody.setTransform(this.poseStack).colorRgb(color).setChanged();
      ArmRenderer.transformUpperArm(msr, upperArmAngle);
      this.upperBody.setTransform(this.poseStack).colorRgb(color).setChanged();
      ArmRenderer.transformHead(msr, headAngle);
      if (this.ceiling && ((ArmBlockEntity)this.blockEntity).goggles) {
         msr.rotateZDegrees(180.0F);
      }

      this.claw.setTransform(this.poseStack).setChanged();
      if (this.ceiling && ((ArmBlockEntity)this.blockEntity).goggles) {
         msr.rotateZDegrees(180.0F);
      }

      ItemStack item = ((ArmBlockEntity)this.blockEntity).heldItem;
      ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
      boolean hasItem = !item.isEmpty();
      boolean isBlockItem = hasItem && item.getItem() instanceof BlockItem && itemRenderer.getModel(item, Minecraft.getInstance().level, null, 0).isGui3d();

      for (int index : Iterate.zeroAndOne) {
         this.poseStack.pushPose();
         int flip = index * 2 - 1;
         ArmRenderer.transformClawHalf(msr, hasItem, isBlockItem, flip);
         this.clawGrips.get(index).setTransform(this.poseStack).setChanged();
         this.poseStack.popPose();
      }

      this.poseStack.popPose();
   }

   @Override
   public void update(float pt) {
      super.update(pt);
      this.instancerProvider()
         .instancer(
            InstanceTypes.TRANSFORMED,
            Models.partial(((ArmBlockEntity)this.blockEntity).goggles ? AllPartialModels.ARM_CLAW_BASE_GOGGLES : AllPartialModels.ARM_CLAW_BASE)
         )
         .stealInstance(this.claw);
   }

   @Override
   public void updateLight(float partialTick) {
      super.updateLight(partialTick);
      this.relight(this.models.toArray(FlatLit[]::new));
   }

   @Override
   protected void _delete() {
      super._delete();
      this.models.forEach(AbstractInstance::delete);
   }

   @Override
   public void collectCrumblingInstances(Consumer<Instance> consumer) {
      super.collectCrumblingInstances(consumer);
      this.models.forEach(consumer);
   }
}
