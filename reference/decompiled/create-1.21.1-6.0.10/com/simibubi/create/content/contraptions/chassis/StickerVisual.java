package com.simibubi.create.content.contraptions.chassis;

import com.simibubi.create.AllPartialModels;
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
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

public class StickerVisual extends AbstractBlockEntityVisual<StickerBlockEntity> implements SimpleDynamicVisual {
   float lastOffset = Float.NaN;
   final Direction facing;
   final boolean fakeWorld;
   final int offset;
   private final TransformedInstance head = (TransformedInstance)this.instancerProvider()
      .instancer(InstanceTypes.TRANSFORMED, Models.partial(AllPartialModels.STICKER_HEAD))
      .createInstance();

   public StickerVisual(VisualizationContext context, StickerBlockEntity blockEntity, float partialTick) {
      super(context, blockEntity, partialTick);
      this.fakeWorld = blockEntity.getLevel() != Minecraft.getInstance().level;
      this.facing = (Direction)this.blockState.getValue(StickerBlock.FACING);
      this.offset = this.blockState.getValue(StickerBlock.EXTENDED) ? 1 : 0;
      this.animateHead((float)this.offset);
   }

   public void beginFrame(Context ctx) {
      float offset = ((StickerBlockEntity)this.blockEntity).piston.getValue(ctx.partialTick());
      if (this.fakeWorld) {
         offset = (float)this.offset;
      }

      if (!Mth.equal(offset, this.lastOffset)) {
         this.animateHead(offset);
         this.lastOffset = offset;
      }
   }

   private void animateHead(float offset) {
      ((TransformedInstance)((TransformedInstance)((TransformedInstance)((TransformedInstance)((TransformedInstance)((TransformedInstance)this.head
                           .setIdentityTransform()
                           .translate(this.getVisualPosition()))
                        .nudge(((StickerBlockEntity)this.blockEntity).hashCode()))
                     .center())
                  .rotateYDegrees(AngleHelper.horizontalAngle(this.facing)))
               .rotateXDegrees(AngleHelper.verticalAngle(this.facing) + 90.0F))
            .uncenter())
         .translate(0.0F, offset * offset * 4.0F / 16.0F, 0.0F)
         .setChanged();
   }

   public void updateLight(float partialTick) {
      this.relight(new FlatLit[]{this.head});
   }

   protected void _delete() {
      this.head.delete();
   }

   public void collectCrumblingInstances(Consumer<Instance> consumer) {
      consumer.accept(this.head);
   }
}
