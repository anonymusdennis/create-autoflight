package com.simibubi.create.content.trains.bogey;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.DynamicVisual.Context;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

public class BogeyBlockEntityVisual extends AbstractBlockEntityVisual<AbstractBogeyBlockEntity> implements SimpleDynamicVisual {
   private final PoseStack poseStack = new PoseStack();
   @Nullable
   private final BogeySizes.BogeySize bogeySize;
   private BogeyStyle lastStyle;
   @Nullable
   private BogeyVisual bogey;

   public BogeyBlockEntityVisual(VisualizationContext ctx, AbstractBogeyBlockEntity blockEntity, float partialTick) {
      super(ctx, blockEntity, partialTick);
      this.lastStyle = blockEntity.getStyle();
      if (this.blockState.getBlock() instanceof AbstractBogeyBlock<?> block) {
         this.bogeySize = block.getSize();
         BlockPos var6 = this.getVisualPosition();
         this.poseStack.translate((float)var6.getX(), (float)var6.getY(), (float)var6.getZ());
         this.poseStack.translate(0.5F, 0.5F, 0.5F);
         if (this.blockState.getValue(AbstractBogeyBlock.AXIS) == Axis.X) {
            this.poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
         }

         this.poseStack.translate(0.0, -1.5078125, 0.0);
         this.bogey = this.lastStyle.createVisual(this.bogeySize, this.visualizationContext, partialTick, false);
         this.updateBogey(partialTick);
      } else {
         this.bogeySize = null;
      }
   }

   public void beginFrame(Context context) {
      if (this.bogeySize != null) {
         BogeyStyle style = ((AbstractBogeyBlockEntity)this.blockEntity).getStyle();
         if (style != this.lastStyle) {
            if (this.bogey != null) {
               this.bogey.delete();
               this.bogey = null;
            }

            this.lastStyle = style;
            this.bogey = this.lastStyle.createVisual(this.bogeySize, this.visualizationContext, context.partialTick(), false);
            this.updateLight(context.partialTick());
         }

         this.updateBogey(context.partialTick());
      }
   }

   private void updateBogey(float partialTick) {
      if (this.bogey != null) {
         CompoundTag bogeyData = ((AbstractBogeyBlockEntity)this.blockEntity).getBogeyData();
         float angle = ((AbstractBogeyBlockEntity)this.blockEntity).getVirtualAngle(partialTick);
         this.bogey.update(bogeyData, angle, this.poseStack);
      }
   }

   public void collectCrumblingInstances(Consumer<Instance> consumer) {
      if (this.bogey != null) {
         this.bogey.collectCrumblingInstances(consumer);
      }
   }

   public void updateLight(float partialTick) {
      if (this.bogey != null) {
         this.bogey.updateLight(this.computePackedLight());
      }
   }

   protected void _delete() {
      if (this.bogey != null) {
         this.bogey.delete();
      }
   }
}
