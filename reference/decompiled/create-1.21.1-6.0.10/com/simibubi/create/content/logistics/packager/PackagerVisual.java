package com.simibubi.create.content.logistics.packager;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.DynamicVisual.Context;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import java.util.function.Consumer;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public class PackagerVisual<T extends PackagerBlockEntity> extends AbstractBlockEntityVisual<T> implements SimpleDynamicVisual {
   public final TransformedInstance hatch;
   public final TransformedInstance tray;
   public float lastTrayOffset = Float.NaN;
   public PartialModel lastHatchPartial;

   public PackagerVisual(VisualizationContext ctx, T blockEntity, float partialTick) {
      super(ctx, blockEntity, partialTick);
      this.lastHatchPartial = PackagerRenderer.getHatchModel(blockEntity);
      this.hatch = (TransformedInstance)this.instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(this.lastHatchPartial)).createInstance();
      this.tray = (TransformedInstance)this.instancerProvider()
         .instancer(InstanceTypes.TRANSFORMED, Models.partial(PackagerRenderer.getTrayModel(this.blockState)))
         .createInstance();
      Direction facing = ((Direction)this.blockState.getValue(PackagerBlock.FACING)).getOpposite();
      Vec3 lowerCorner = Vec3.atLowerCornerOf(facing.getNormal());
      ((TransformedInstance)((TransformedInstance)((TransformedInstance)((TransformedInstance)this.hatch
                     .setIdentityTransform()
                     .translate(this.getVisualPosition()))
                  .translate(lowerCorner.scale(0.49999F)))
               .rotateYCenteredDegrees(AngleHelper.horizontalAngle(facing)))
            .rotateXCenteredDegrees(AngleHelper.verticalAngle(facing)))
         .setChanged();
      this.animate(partialTick);
   }

   public void beginFrame(Context ctx) {
      this.animate(ctx.partialTick());
   }

   public void animate(float partialTick) {
      PartialModel hatchPartial = PackagerRenderer.getHatchModel((PackagerBlockEntity)this.blockEntity);
      if (hatchPartial != this.lastHatchPartial) {
         this.instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(hatchPartial)).stealInstance(this.hatch);
         this.lastHatchPartial = hatchPartial;
      }

      float trayOffset = ((PackagerBlockEntity)this.blockEntity).getTrayOffset(partialTick);
      if (trayOffset != this.lastTrayOffset) {
         Direction facing = ((Direction)this.blockState.getValue(PackagerBlock.FACING)).getOpposite();
         Vec3 lowerCorner = Vec3.atLowerCornerOf(facing.getNormal());
         ((TransformedInstance)((TransformedInstance)((TransformedInstance)this.tray.setIdentityTransform().translate(this.getVisualPosition()))
                  .translate(lowerCorner.scale((double)trayOffset)))
               .rotateYCenteredDegrees(facing.toYRot()))
            .setChanged();
         this.lastTrayOffset = trayOffset;
      }
   }

   public void updateLight(float partialTick) {
      this.relight(new FlatLit[]{this.hatch, this.tray});
   }

   protected void _delete() {
      this.hatch.delete();
      this.tray.delete();
   }

   public void collectCrumblingInstances(Consumer<Instance> consumer) {
   }
}
