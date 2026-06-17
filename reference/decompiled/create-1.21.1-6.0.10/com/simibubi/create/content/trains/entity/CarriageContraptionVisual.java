package com.simibubi.create.content.trains.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionVisual;
import com.simibubi.create.content.trains.bogey.BogeyVisual;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import dev.engine_room.flywheel.api.visual.DynamicVisual.Context;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.engine_room.flywheel.lib.util.RecyclingPoseStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import org.apache.commons.lang3.tuple.MutablePair;
import org.joml.Vector3f;

public class CarriageContraptionVisual extends ContraptionVisual<CarriageContraptionEntity> {
   public static final int MAX_NUM_BOGEYS = 2;
   private final PoseStack poseStack = new RecyclingPoseStack();
   private final CarriageContraption contraption;
   private int numBogeys;
   private final CarriageBogey[] bogeys = new CarriageBogey[2];
   private final BogeyVisual[] visuals = new BogeyVisual[2];
   private final int[] bogeyPos = new int[2];

   public CarriageContraptionVisual(VisualizationContext context, CarriageContraptionEntity entity, float partialTick) {
      super(context, entity, partialTick);
      this.lightPaddingBlocks = 2;
      this.contraption = (CarriageContraption)entity.getContraption();
      this.animate(partialTick);
   }

   @Override
   public void beginFrame(Context ctx) {
      super.beginFrame(ctx);
      this.animate(ctx.partialTick());
   }

   @Override
   protected <T extends BlockEntity> void setupVisualizer(T be, float partialTicks) {
      if (((CarriageContraptionEntity)this.entity).getContraption() instanceof CarriageContraption cc && cc.isHiddenInPortal(be.getBlockPos())) {
         return;
      }

      super.setupVisualizer(be, partialTicks);
   }

   @Override
   protected void setupActor(MutablePair<StructureBlockInfo, MovementContext> actor, VirtualRenderWorld renderLevel) {
      if (((CarriageContraptionEntity)this.entity).getContraption() instanceof CarriageContraption cc
         && cc.isHiddenInPortal(((StructureBlockInfo)actor.left).pos())) {
         return;
      }

      super.setupActor(actor, renderLevel);
   }

   private boolean checkCarriage(float pt) {
      if (this.numBogeys > 0) {
         return true;
      } else {
         Carriage carriage = ((CarriageContraptionEntity)this.entity).getCarriage();
         if (((CarriageContraptionEntity)this.entity).validForRender && carriage != null) {
            this.numBogeys = 0;

            for (CarriageBogey bogey : carriage.bogeys) {
               if (bogey != null) {
                  this.visuals[this.numBogeys] = bogey.getStyle().createVisual(bogey.getSize(), this.visualizationContext, pt, true);
                  this.bogeys[this.numBogeys] = bogey;
                  this.bogeyPos[this.numBogeys] = bogey.isLeading
                     ? 0
                     : carriage.bogeySpacing * this.contraption.getAssemblyDirection().getCounterClockWise().getAxisDirection().getStep();
                  this.numBogeys++;
               }
            }

            return true;
         } else {
            return false;
         }
      }
   }

   private void animate(float partialTick) {
      if (this.checkCarriage(partialTick)) {
         float viewYRot = ((CarriageContraptionEntity)this.entity).getViewYRot(partialTick);
         float viewXRot = ((CarriageContraptionEntity)this.entity).getViewXRot(partialTick);
         Carriage carriage = ((CarriageContraptionEntity)this.entity).getCarriage();
         int bogeySpacing = carriage.bogeySpacing;
         this.poseStack.pushPose();
         Vector3f visualPosition = this.getVisualPosition(partialTick);
         TransformStack.of(this.poseStack).translate(visualPosition);

         for (int bogeyIdx = 0; bogeyIdx < this.numBogeys; bogeyIdx++) {
            if (this.contraption.isHiddenInPortal(this.bogeyPos[bogeyIdx])) {
               this.visuals[bogeyIdx].hide();
            } else {
               this.poseStack.pushPose();
               CarriageBogey bogey = this.bogeys[bogeyIdx];
               CarriageContraptionEntityRenderer.translateBogey(this.poseStack, bogey, bogeySpacing, viewYRot, viewXRot, partialTick);
               this.poseStack.translate(0.0, -1.5078125, 0.0);
               CompoundTag bogeyData = bogey.bogeyData;
               if (bogeyData == null) {
                  bogeyData = new CompoundTag();
               }

               this.visuals[bogeyIdx].update(bogeyData, bogey.wheelAngle.getValue(partialTick), this.poseStack);
               this.poseStack.popPose();
            }
         }

         this.poseStack.popPose();
      }
   }

   @Override
   public void _delete() {
      super._delete();

      for (BogeyVisual visual : this.visuals) {
         if (visual != null) {
            visual.delete();
         }
      }
   }
}
