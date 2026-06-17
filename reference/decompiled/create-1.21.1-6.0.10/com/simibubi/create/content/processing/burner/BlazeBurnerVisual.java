package com.simibubi.create.content.processing.burner;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.AllSpriteShifts;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.TickableVisual.Context;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;
import java.util.function.Consumer;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.SpriteShiftEntry;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public class BlazeBurnerVisual extends AbstractBlockEntityVisual<BlazeBurnerBlockEntity> implements SimpleDynamicVisual, SimpleTickableVisual {
   private BlazeBurnerBlock.HeatLevel heatLevel = BlazeBurnerBlock.HeatLevel.SMOULDERING;
   private final TransformedInstance head;
   private final boolean isInert;
   @Nullable
   private TransformedInstance smallRods;
   @Nullable
   private TransformedInstance largeRods;
   @Nullable
   private ScrollInstance flame;
   @Nullable
   private TransformedInstance goggles;
   @Nullable
   private TransformedInstance hat;
   private boolean validBlockAbove;

   public BlazeBurnerVisual(VisualizationContext ctx, BlazeBurnerBlockEntity blockEntity, float partialTick) {
      super(ctx, blockEntity, partialTick);
      this.validBlockAbove = blockEntity.isValidBlockAbove();
      PartialModel blazeModel = BlazeBurnerRenderer.getBlazeModel(this.heatLevel, this.validBlockAbove);
      this.isInert = blazeModel == AllPartialModels.BLAZE_INERT;
      this.head = (TransformedInstance)this.instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(blazeModel)).createInstance();
      this.head.light(15728880);
      this.animate(partialTick);
   }

   public void tick(Context context) {
      ((BlazeBurnerBlockEntity)this.blockEntity).tickAnimation();
   }

   public void beginFrame(dev.engine_room.flywheel.api.visual.DynamicVisual.Context ctx) {
      if (this.isVisible(ctx.frustum()) && !this.doDistanceLimitThisFrame(ctx)) {
         this.animate(ctx.partialTick());
      }
   }

   private void animate(float partialTicks) {
      float animation = ((BlazeBurnerBlockEntity)this.blockEntity).headAnimation.getValue(partialTicks) * 0.175F;
      boolean validBlockAbove = animation > 0.125F;
      BlazeBurnerBlock.HeatLevel heatLevel = ((BlazeBurnerBlockEntity)this.blockEntity).getHeatLevelForRender();
      if (validBlockAbove != this.validBlockAbove || heatLevel != this.heatLevel) {
         this.validBlockAbove = validBlockAbove;
         PartialModel blazeModel = BlazeBurnerRenderer.getBlazeModel(heatLevel, validBlockAbove);
         this.instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(blazeModel)).stealInstance(this.head);
         boolean needsRods = heatLevel.isAtLeast(BlazeBurnerBlock.HeatLevel.FADING);
         boolean hasRods = this.heatLevel.isAtLeast(BlazeBurnerBlock.HeatLevel.FADING);
         if (needsRods && !hasRods) {
            PartialModel rodsModel = heatLevel == BlazeBurnerBlock.HeatLevel.SEETHING
               ? AllPartialModels.BLAZE_BURNER_SUPER_RODS
               : AllPartialModels.BLAZE_BURNER_RODS;
            PartialModel rodsModel2 = heatLevel == BlazeBurnerBlock.HeatLevel.SEETHING
               ? AllPartialModels.BLAZE_BURNER_SUPER_RODS_2
               : AllPartialModels.BLAZE_BURNER_RODS_2;
            this.smallRods = (TransformedInstance)this.instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(rodsModel)).createInstance();
            this.largeRods = (TransformedInstance)this.instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(rodsModel2)).createInstance();
            this.smallRods.light(15728880);
            this.largeRods.light(15728880);
         } else if (!needsRods && hasRods) {
            if (this.smallRods != null) {
               this.smallRods.delete();
            }

            if (this.largeRods != null) {
               this.largeRods.delete();
            }

            this.smallRods = null;
            this.largeRods = null;
         }

         this.heatLevel = heatLevel;
      }

      if (validBlockAbove && this.flame == null) {
         this.setupFlameInstance();
      } else if (!validBlockAbove && this.flame != null) {
         this.flame.delete();
         this.flame = null;
      }

      if (((BlazeBurnerBlockEntity)this.blockEntity).goggles && this.goggles == null) {
         this.goggles = (TransformedInstance)this.instancerProvider()
            .instancer(InstanceTypes.TRANSFORMED, Models.partial(this.isInert ? AllPartialModels.BLAZE_GOGGLES_SMALL : AllPartialModels.BLAZE_GOGGLES))
            .createInstance();
         this.goggles.light(15728880);
      } else if (!((BlazeBurnerBlockEntity)this.blockEntity).goggles && this.goggles != null) {
         this.goggles.delete();
         this.goggles = null;
      }

      boolean hatPresent = ((BlazeBurnerBlockEntity)this.blockEntity).hat || ((BlazeBurnerBlockEntity)this.blockEntity).stockKeeper;
      if (hatPresent && this.hat == null) {
         this.hat = (TransformedInstance)this.instancerProvider()
            .instancer(
               InstanceTypes.TRANSFORMED,
               Models.partial(((BlazeBurnerBlockEntity)this.blockEntity).stockKeeper ? AllPartialModels.LOGISTICS_HAT : AllPartialModels.TRAIN_HAT)
            )
            .createInstance();
         this.hat.light(15728880);
      } else if (!hatPresent && this.hat != null) {
         this.hat.delete();
         this.hat = null;
      }

      int hashCode = ((BlazeBurnerBlockEntity)this.blockEntity).hashCode();
      float time = AnimationTickHolder.getRenderTime(this.level);
      float renderTick = time + (float)(hashCode % 13) * 16.0F;
      float offsetMult = heatLevel.isAtLeast(BlazeBurnerBlock.HeatLevel.FADING) ? 64.0F : 16.0F;
      float offset = Mth.sin((float)((double)(renderTick / 16.0F) % (Math.PI * 2))) / offsetMult;
      float headY = offset - animation * 0.75F;
      float horizontalAngle = AngleHelper.rad((double)((BlazeBurnerBlockEntity)this.blockEntity).headAngle.getValue(partialTicks));
      ((TransformedInstance)((TransformedInstance)((TransformedInstance)((TransformedInstance)this.head
                     .setIdentityTransform()
                     .translate(this.getVisualPosition()))
                  .translateY(headY))
               .translate(0.5F))
            .rotateY(horizontalAngle)
            .translateBack(0.5F))
         .setChanged();
      if (this.goggles != null) {
         ((TransformedInstance)((TransformedInstance)((TransformedInstance)((TransformedInstance)this.goggles
                        .setIdentityTransform()
                        .translate(this.getVisualPosition()))
                     .translateY(headY + 0.5F))
                  .translate(0.5F))
               .rotateY(horizontalAngle)
               .translateBack(0.5F))
            .setChanged();
      }

      if (this.hat != null) {
         ((TransformedInstance)((TransformedInstance)this.hat.setIdentityTransform().translate(this.getVisualPosition())).translateY(headY)).translateY(0.75F);
         ((TransformedInstance)this.hat.rotateCentered(horizontalAngle + (float) Math.PI, Direction.UP)).translate(0.5F, 0.0F, 0.5F).light(15728880);
         this.hat.setChanged();
      }

      if (this.smallRods != null) {
         float offset1 = Mth.sin((float)(((double)(renderTick / 16.0F) + Math.PI) % (Math.PI * 2))) / offsetMult;
         ((TransformedInstance)((TransformedInstance)this.smallRods.setIdentityTransform().translate(this.getVisualPosition()))
               .translateY(offset1 + animation + 0.125F))
            .setChanged();
      }

      if (this.largeRods != null) {
         float offset2 = Mth.sin((float)(((double)(renderTick / 16.0F) + (Math.PI / 2)) % (Math.PI * 2))) / offsetMult;
         ((TransformedInstance)((TransformedInstance)this.largeRods.setIdentityTransform().translate(this.getVisualPosition()))
               .translateY(offset2 + animation - 0.1875F))
            .setChanged();
      }
   }

   private void setupFlameInstance() {
      this.flame = (ScrollInstance)this.instancerProvider()
         .instancer(AllInstanceTypes.SCROLLING, Models.partial(AllPartialModels.BLAZE_BURNER_FLAME))
         .createInstance();
      this.flame.position(this.getVisualPosition()).light(15728880);
      SpriteShiftEntry spriteShift = this.heatLevel == BlazeBurnerBlock.HeatLevel.SEETHING ? AllSpriteShifts.SUPER_BURNER_FLAME : AllSpriteShifts.BURNER_FLAME;
      float spriteWidth = spriteShift.getTarget().getU1() - spriteShift.getTarget().getU0();
      float spriteHeight = spriteShift.getTarget().getV1() - spriteShift.getTarget().getV0();
      float speed = 0.03125F + 0.015625F * (float)this.heatLevel.ordinal();
      this.flame.speedU = speed / 2.0F;
      this.flame.speedV = speed;
      this.flame.scaleU = spriteWidth / 2.0F;
      this.flame.scaleV = spriteHeight / 2.0F;
      this.flame.diffU = spriteShift.getTarget().getU0() - spriteShift.getOriginal().getU0();
      this.flame.diffV = spriteShift.getTarget().getV0() - spriteShift.getOriginal().getV0();
   }

   public void updateLight(float partialTick) {
   }

   public void collectCrumblingInstances(Consumer<Instance> consumer) {
   }

   protected void _delete() {
      this.head.delete();
      if (this.smallRods != null) {
         this.smallRods.delete();
      }

      if (this.largeRods != null) {
         this.largeRods.delete();
      }

      if (this.flame != null) {
         this.flame.delete();
      }

      if (this.goggles != null) {
         this.goggles.delete();
      }

      if (this.hat != null) {
         this.hat.delete();
      }
   }
}
