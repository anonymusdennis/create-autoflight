package dev.eriksonn.aeronautics.content.blocks.mounted_potato_cannon;

import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.base.ShaftVisual;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.DynamicVisual.Context;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.OrientedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import dev.eriksonn.aeronautics.index.AeroPartialModels;
import java.util.function.Consumer;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Quaternionf;

public class MountedPotatoCannonVisual extends ShaftVisual<MountedPotatoCannonBlockEntity> implements SimpleDynamicVisual {
   private final OrientedInstance cogInstance;
   final Axis rotationAxis;
   final Quaternionf blockOrientation;

   public MountedPotatoCannonVisual(VisualizationContext context, MountedPotatoCannonBlockEntity blockEntity, float partialTick) {
      super(context, blockEntity, partialTick);
      Direction facing = (Direction)this.blockState.getValue(BlockStateProperties.FACING);
      this.rotationAxis = Axis.of(Direction.get(AxisDirection.POSITIVE, this.rotationAxis()).step());
      this.blockOrientation = getBlockStateOrientation(facing);
      this.cogInstance = (OrientedInstance)this.instancerProvider()
         .instancer(InstanceTypes.ORIENTED, Models.partial(AeroPartialModels.CANNON_COG))
         .createInstance();
      this.cogInstance.position(this.getVisualPosition()).rotation(this.blockOrientation).setChanged();
   }

   public void update(float pt) {
      super.update(pt);
   }

   public void updateLight(float partialTick) {
      super.updateLight(partialTick);
      this.relight(new FlatLit[]{this.cogInstance});
   }

   protected void _delete() {
      super._delete();
      this.cogInstance.delete();
   }

   public void collectCrumblingInstances(Consumer<Instance> consumer) {
      super.collectCrumblingInstances(consumer);
      consumer.accept(this.cogInstance);
   }

   static Quaternionf getBlockStateOrientation(Direction facing) {
      Quaternionf orientation;
      if (facing.getAxis().isHorizontal()) {
         orientation = Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(facing));
      } else {
         orientation = new Quaternionf();
      }

      orientation.mul(Axis.XP.rotationDegrees(AngleHelper.verticalAngle(facing)));
      return orientation;
   }

   public void beginFrame(Context context) {
      float angle = ((MountedPotatoCannonBlockEntity)this.blockEntity).getCogwheelAngle(context.partialTick());
      Quaternionf rot = Axis.ZP.rotationDegrees(angle);
      rot.premul(this.blockOrientation);
      this.cogInstance.rotation(rot).setChanged();
   }
}
