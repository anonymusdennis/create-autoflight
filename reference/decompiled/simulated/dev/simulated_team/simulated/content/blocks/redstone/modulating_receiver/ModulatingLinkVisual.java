package dev.simulated_team.simulated.content.blocks.redstone.modulating_receiver;

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
import java.util.function.Consumer;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Vector3d;

public class ModulatingLinkVisual extends AbstractBlockEntityVisual<ModulatingLinkedReceiverBlockEntity> implements SimpleDynamicVisual {
   public static final float MAX_DISTANCE = 256.0F;
   public static final float SMOOTHING = 20.0F;
   private final Vector3d tempNormal = new Vector3d();
   private final TransformedInstance topPlate = (TransformedInstance)this.instancerProvider()
      .instancer(InstanceTypes.TRANSFORMED, Models.partial(SimPartialModels.MODULATING_RECEIVER_PLATE))
      .createInstance();
   private final TransformedInstance bottomPlate = (TransformedInstance)this.instancerProvider()
      .instancer(InstanceTypes.TRANSFORMED, Models.partial(SimPartialModels.MODULATING_RECEIVER_PLATE))
      .createInstance();
   private final Direction facing;

   public ModulatingLinkVisual(VisualizationContext ctx, ModulatingLinkedReceiverBlockEntity blockEntity, float partialTick) {
      super(ctx, blockEntity, partialTick);
      this.facing = (Direction)blockEntity.getBlockState().getValue(BlockStateProperties.FACING);
      this.handleTransform();
   }

   public void beginFrame(Context context) {
      this.handleTransform();
   }

   private void handleTransform() {
      this.topPlate.setIdentityTransform();
      this.bottomPlate.setIdentityTransform();
      this.tempNormal.set(this.facing.step()).mul(0.0625);
      float max = this.getMax();
      ((TransformedInstance)this.topPlate.translate(this.getVisualPosition()))
         .translate(this.tempNormal.x() * (0.5 + (double)max), this.tempNormal.y() * (0.5 + (double)max), this.tempNormal.z() * (0.5 + (double)max));
      float min = this.getMin();
      ((TransformedInstance)this.bottomPlate.translate(this.getVisualPosition()))
         .translate(this.tempNormal.x() * (double)min, this.tempNormal.y() * (double)min, this.tempNormal.z() * (double)min);
      if (this.facing.getAxis().isHorizontal()) {
         this.rotateInstanceHorizontally(this.topPlate, this.facing);
         this.rotateInstanceHorizontally(this.bottomPlate, this.facing);
      }

      this.rotateInstanceVertically(this.topPlate, this.facing);
      this.rotateInstanceVertically(this.bottomPlate, this.facing);
      this.topPlate.setChanged();
      this.bottomPlate.setChanged();
   }

   private void rotateInstanceHorizontally(TransformedInstance inst, Direction facing) {
      inst.rotateCentered(AngleHelper.rad((double)AngleHelper.horizontalAngle(facing.getOpposite())), Direction.UP);
   }

   private void rotateInstanceVertically(TransformedInstance inst, Direction facing) {
      inst.rotateCentered(AngleHelper.rad((double)(-90.0F - AngleHelper.verticalAngle(facing))), Direction.EAST);
   }

   private float getMin() {
      return 5.5F
         * (float)(((ModulatingLinkedReceiverBlockEntity)this.blockEntity).minRange - 1)
         * 275.0F
         / (255.0F * (20.0F + (float)((ModulatingLinkedReceiverBlockEntity)this.blockEntity).minRange - 1.0F));
   }

   private float getMax() {
      return 5.5F
         * (float)(((ModulatingLinkedReceiverBlockEntity)this.blockEntity).maxRange - 1)
         * 275.0F
         / (255.0F * (20.0F + (float)((ModulatingLinkedReceiverBlockEntity)this.blockEntity).maxRange - 1.0F));
   }

   public void collectCrumblingInstances(Consumer<Instance> consumer) {
      consumer.accept(this.topPlate);
      consumer.accept(this.bottomPlate);
   }

   public void updateLight(float v) {
      this.relight(new FlatLit[]{this.topPlate});
      this.relight(new FlatLit[]{this.bottomPlate});
   }

   protected void _delete() {
      this.topPlate.delete();
      this.bottomPlate.delete();
   }
}
