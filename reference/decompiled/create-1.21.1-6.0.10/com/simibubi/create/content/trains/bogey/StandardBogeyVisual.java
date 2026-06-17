package com.simibubi.create.content.trains.bogey;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.AllSpriteShifts;
import com.simibubi.create.content.processing.burner.ScrollTransformedInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import com.simibubi.create.foundation.render.SpecialModels;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.instance.Instancer;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import java.util.function.Consumer;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

public class StandardBogeyVisual implements BogeyVisual {
   private final TransformedInstance shaft1;
   private final TransformedInstance shaft2;

   public StandardBogeyVisual(VisualizationContext ctx, float partialTick, boolean inContraption) {
      Instancer<TransformedInstance> shaftInstancer = ctx.instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(AllPartialModels.SHAFT));
      this.shaft1 = (TransformedInstance)shaftInstancer.createInstance();
      this.shaft2 = (TransformedInstance)shaftInstancer.createInstance();
   }

   @Override
   public void update(CompoundTag bogeyData, float wheelAngle, PoseStack poseStack) {
      ((TransformedInstance)((TransformedInstance)((TransformedInstance)((TransformedInstance)this.shaft1
                     .setTransform(poseStack)
                     .translate(-0.5F, 0.25F, 0.0F)
                     .center())
                  .rotateTo(Direction.UP, Direction.SOUTH))
               .rotateYDegrees(wheelAngle))
            .uncenter())
         .setChanged();
      ((TransformedInstance)((TransformedInstance)((TransformedInstance)((TransformedInstance)this.shaft2
                     .setTransform(poseStack)
                     .translate(-0.5F, 0.25F, -1.0F)
                     .center())
                  .rotateTo(Direction.UP, Direction.SOUTH))
               .rotateYDegrees(wheelAngle))
            .uncenter())
         .setChanged();
   }

   @Override
   public void hide() {
      this.shaft1.setZeroTransform().setChanged();
      this.shaft2.setZeroTransform().setChanged();
   }

   @Override
   public void updateLight(int packedLight) {
      this.shaft1.light(packedLight).setChanged();
      this.shaft2.light(packedLight).setChanged();
   }

   @Override
   public void collectCrumblingInstances(Consumer<Instance> consumer) {
      consumer.accept(this.shaft1);
      consumer.accept(this.shaft2);
   }

   @Override
   public void delete() {
      this.shaft1.delete();
      this.shaft2.delete();
   }

   public static class Large extends StandardBogeyVisual {
      private final TransformedInstance secondaryShaft1;
      private final TransformedInstance secondaryShaft2;
      private final TransformedInstance drive;
      private final ScrollTransformedInstance belt;
      private final TransformedInstance piston;
      private final TransformedInstance wheels;
      private final TransformedInstance pin;

      public Large(VisualizationContext ctx, float partialTick, boolean inContraption) {
         super(ctx, partialTick, inContraption);
         Instancer<TransformedInstance> secondaryShaftInstancer = ctx.instancerProvider()
            .instancer(InstanceTypes.TRANSFORMED, SpecialModels.smoothLit(AllPartialModels.SHAFT));
         this.secondaryShaft1 = (TransformedInstance)secondaryShaftInstancer.createInstance();
         this.secondaryShaft2 = (TransformedInstance)secondaryShaftInstancer.createInstance();
         this.drive = (TransformedInstance)ctx.instancerProvider()
            .instancer(InstanceTypes.TRANSFORMED, SpecialModels.smoothLit(AllPartialModels.BOGEY_DRIVE))
            .createInstance();
         this.belt = (ScrollTransformedInstance)ctx.instancerProvider()
            .instancer(AllInstanceTypes.SCROLLING_TRANSFORMED, SpecialModels.smoothLit(AllPartialModels.BOGEY_DRIVE_BELT))
            .createInstance();
         this.piston = (TransformedInstance)ctx.instancerProvider()
            .instancer(InstanceTypes.TRANSFORMED, SpecialModels.smoothLit(AllPartialModels.BOGEY_PISTON))
            .createInstance();
         this.wheels = (TransformedInstance)ctx.instancerProvider()
            .instancer(InstanceTypes.TRANSFORMED, SpecialModels.smoothLit(AllPartialModels.LARGE_BOGEY_WHEELS))
            .createInstance();
         this.pin = (TransformedInstance)ctx.instancerProvider()
            .instancer(InstanceTypes.TRANSFORMED, SpecialModels.smoothLit(AllPartialModels.BOGEY_PIN))
            .createInstance();
         this.belt.setSpriteShift(AllSpriteShifts.BOGEY_BELT);
      }

      @Override
      public void update(CompoundTag bogeyData, float wheelAngle, PoseStack poseStack) {
         super.update(bogeyData, wheelAngle, poseStack);
         ((TransformedInstance)((TransformedInstance)((TransformedInstance)((TransformedInstance)this.secondaryShaft1
                        .setTransform(poseStack)
                        .translate(-0.5F, 0.25F, 0.5F)
                        .center())
                     .rotateTo(Direction.UP, Direction.EAST))
                  .rotateYDegrees(wheelAngle))
               .uncenter())
            .setChanged();
         ((TransformedInstance)((TransformedInstance)((TransformedInstance)((TransformedInstance)this.secondaryShaft2
                        .setTransform(poseStack)
                        .translate(-0.5F, 0.25F, -1.5F)
                        .center())
                     .rotateTo(Direction.UP, Direction.EAST))
                  .rotateYDegrees(wheelAngle))
               .uncenter())
            .setChanged();
         ((TransformedInstance)this.drive.setTransform(poseStack).scale(0.9980469F)).setChanged();
         ((TransformedInstance)this.belt.offset(0.0F, 0.0054541538F * wheelAngle).setTransform(poseStack).scale(0.9980469F)).setChanged();
         ((TransformedInstance)this.piston.setTransform(poseStack).translate(0.0, 0.0, 0.25 * Math.sin((double)AngleHelper.rad((double)wheelAngle))))
            .setChanged();
         ((TransformedInstance)this.wheels.setTransform(poseStack).translate(0.0F, 1.0F, 0.0F).rotateXDegrees(wheelAngle)).setChanged();
         ((TransformedInstance)((TransformedInstance)this.pin.setTransform(poseStack).translate(0.0F, 1.0F, 0.0F).rotateXDegrees(wheelAngle))
               .translate(0.0F, 0.25F, 0.0F)
               .rotateXDegrees(-wheelAngle))
            .setChanged();
      }

      @Override
      public void hide() {
         super.hide();
         this.secondaryShaft1.setZeroTransform().setChanged();
         this.secondaryShaft2.setZeroTransform().setChanged();
         this.wheels.setZeroTransform().setChanged();
         this.drive.setZeroTransform().setChanged();
         this.belt.setZeroTransform().setChanged();
         this.piston.setZeroTransform().setChanged();
         this.pin.setZeroTransform().setChanged();
      }

      @Override
      public void updateLight(int packedLight) {
         super.updateLight(packedLight);
         this.secondaryShaft1.light(packedLight).setChanged();
         this.secondaryShaft2.light(packedLight).setChanged();
         this.wheels.light(packedLight).setChanged();
         this.drive.light(packedLight).setChanged();
         this.belt.light(packedLight).setChanged();
         this.piston.light(packedLight).setChanged();
         this.pin.light(packedLight).setChanged();
      }

      @Override
      public void collectCrumblingInstances(Consumer<Instance> consumer) {
         super.collectCrumblingInstances(consumer);
         consumer.accept(this.secondaryShaft1);
         consumer.accept(this.secondaryShaft2);
         consumer.accept(this.wheels);
         consumer.accept(this.drive);
         consumer.accept(this.belt);
         consumer.accept(this.piston);
         consumer.accept(this.pin);
      }

      @Override
      public void delete() {
         super.delete();
         this.secondaryShaft1.delete();
         this.secondaryShaft2.delete();
         this.wheels.delete();
         this.drive.delete();
         this.belt.delete();
         this.piston.delete();
         this.pin.delete();
      }
   }

   public static class Small extends StandardBogeyVisual {
      private final TransformedInstance frame;
      private final TransformedInstance wheel1;
      private final TransformedInstance wheel2;

      public Small(VisualizationContext ctx, float partialTick, boolean inContraption) {
         super(ctx, partialTick, inContraption);
         Instancer<TransformedInstance> wheelInstancer = ctx.instancerProvider()
            .instancer(InstanceTypes.TRANSFORMED, SpecialModels.smoothLit(AllPartialModels.SMALL_BOGEY_WHEELS));
         this.frame = (TransformedInstance)ctx.instancerProvider()
            .instancer(InstanceTypes.TRANSFORMED, SpecialModels.smoothLit(AllPartialModels.BOGEY_FRAME))
            .createInstance();
         this.wheel1 = (TransformedInstance)wheelInstancer.createInstance();
         this.wheel2 = (TransformedInstance)wheelInstancer.createInstance();
      }

      @Override
      public void update(CompoundTag bogeyData, float wheelAngle, PoseStack poseStack) {
         super.update(bogeyData, wheelAngle, poseStack);
         ((TransformedInstance)this.wheel1.setTransform(poseStack).translate(0.0F, 0.75F, -1.0F).rotateXDegrees(wheelAngle)).setChanged();
         ((TransformedInstance)this.wheel2.setTransform(poseStack).translate(0.0F, 0.75F, 1.0F).rotateXDegrees(wheelAngle)).setChanged();
         ((TransformedInstance)this.frame.setTransform(poseStack).scale(0.9980469F)).setChanged();
      }

      @Override
      public void hide() {
         super.hide();
         this.frame.setZeroTransform().setChanged();
         this.wheel1.setZeroTransform().setChanged();
         this.wheel2.setZeroTransform().setChanged();
      }

      @Override
      public void updateLight(int packedLight) {
         super.updateLight(packedLight);
         this.frame.light(packedLight).setChanged();
         this.wheel1.light(packedLight).setChanged();
         this.wheel2.light(packedLight).setChanged();
      }

      @Override
      public void collectCrumblingInstances(Consumer<Instance> consumer) {
         super.collectCrumblingInstances(consumer);
         consumer.accept(this.frame);
         consumer.accept(this.wheel1);
         consumer.accept(this.wheel2);
      }

      @Override
      public void delete() {
         super.delete();
         this.frame.delete();
         this.wheel1.delete();
         this.wheel2.delete();
      }
   }
}
