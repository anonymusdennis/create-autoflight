package dev.simulated_team.simulated.content.blocks.throttle_lever;

import com.simibubi.create.content.redstone.analogLever.AnalogLeverBlock;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.DynamicVisual.Context;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import dev.simulated_team.simulated.index.SimPartialModels;
import dev.simulated_team.simulated.util.SimColors;
import java.util.function.Consumer;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.AttachFace;

public class ThrottleLeverVisual extends AbstractBlockEntityVisual<ThrottleLeverBlockEntity> implements SimpleDynamicVisual {
   private final TransformedInstance diode = (TransformedInstance)this.instancerProvider()
      .instancer(InstanceTypes.TRANSFORMED, Models.partial(SimPartialModels.THROTTLE_LEVER_DIODE))
      .createInstance();
   private final TransformedInstance handle = (TransformedInstance)this.instancerProvider()
      .instancer(InstanceTypes.TRANSFORMED, Models.partial(SimPartialModels.THROTTLE_LEVER_HANDLE))
      .createInstance();
   private final TransformedInstance button = (TransformedInstance)this.instancerProvider()
      .instancer(InstanceTypes.TRANSFORMED, Models.partial(SimPartialModels.THROTTLE_LEVER_BUTTON))
      .createInstance();
   private final AttachFace attached;
   private final Direction facing;

   public ThrottleLeverVisual(VisualizationContext ctx, ThrottleLeverBlockEntity blockEntity, float partialTick) {
      super(ctx, blockEntity, partialTick);
      this.attached = (AttachFace)blockEntity.getBlockState().getValue(AnalogLeverBlock.FACE);
      this.facing = (Direction)blockEntity.getBlockState().getValue(AnalogLeverBlock.FACING);
      this.diode.colorArgb(SimColors.redstone(Math.max(0.0F, (float)blockEntity.state / 15.0F)));
      this.transformAll(partialTick);
   }

   public void beginFrame(Context context) {
      this.diode.colorArgb(SimColors.redstone(Math.max(0.0F, (float)((ThrottleLeverBlockEntity)this.blockEntity).state / 15.0F)));
      this.transformAll(context.partialTick());
   }

   private void transformAll(float partialTicks) {
      this.diode.setIdentityTransform();
      this.handle.setIdentityTransform();
      this.button.setIdentityTransform();
      this.initialTransform(this.handle);
      this.initialTransform(this.button);
      this.initialTransform(this.diode);
      double buttonAngle = (double)(((ThrottleLeverBlockEntity)this.blockEntity).clientPressedLerp.getValue(partialTicks) * -7.0F);
      float angle = (float)(((double)(((ThrottleLeverBlockEntity)this.blockEntity).clientAngle.getValue(partialTicks) / 15.0F) * 80.0 - 40.0) / 180.0 * Math.PI);
      if (this.attached == AttachFace.WALL) {
         angle = -angle;
      }

      this.transformHandle(this.handle, angle, this.attached);
      this.transformHandle(this.button, angle, this.attached);
      ((TransformedInstance)this.button.translate(0.0F, 0.875F, 0.5F).rotateXDegrees((float)buttonAngle)).translateBack(0.0F, 0.875F, 0.5F);
      this.diode.setChanged();
      this.handle.setChanged();
      this.button.setChanged();
   }

   private void initialTransform(TransformedInstance instance) {
      instance.translate(this.getVisualPosition());

      float rX = switch (this.attached) {
         case FLOOR -> 0.0F;
         case WALL -> 90.0F;
         default -> 180.0F;
      };
      float rY = AngleHelper.horizontalAngle(this.facing);
      instance.rotateCentered((float)((double)(rY / 180.0F) * Math.PI), Direction.UP);
      instance.rotateCentered((float)((double)(rX / 180.0F) * Math.PI), Direction.EAST);
      instance.rotateCentered(this.attached == AttachFace.CEILING ? (float) Math.PI : 0.0F, Direction.UP);
   }

   private void transformHandle(TransformedInstance instance, float angle, AttachFace face) {
      ((TransformedInstance)instance.translate(0.5F, 0.1875F, 0.5F).rotateX(angle).translateBack(0.5F, 0.1875F, 0.5F))
         .rotateCentered(face == AttachFace.WALL ? (float) Math.PI : 0.0F, Direction.UP);
   }

   public void collectCrumblingInstances(Consumer<Instance> consumer) {
      consumer.accept(this.handle);
      consumer.accept(this.button);
      consumer.accept(this.diode);
   }

   public void updateLight(float v) {
      this.relight(new FlatLit[]{this.handle});
      this.relight(new FlatLit[]{this.button});
      this.relight(new FlatLit[]{this.diode});
   }

   protected void _delete() {
      this.handle.delete();
      this.button.delete();
      this.diode.delete();
   }
}
