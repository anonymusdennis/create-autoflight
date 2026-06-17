package dev.eriksonn.aeronautics.content.blocks.propeller.small;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.OrientedRotatingVisual;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.DynamicVisual.Context;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.OrientedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import dev.simulated_team.simulated.util.SimMathUtils;
import java.util.function.Consumer;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public abstract class SimplePropellerVisual<T extends BasePropellerBlockEntity> extends OrientedRotatingVisual<T> implements SimpleDynamicVisual {
   protected final OrientedInstance propeller;
   protected final Vector3f rotationAxis;
   protected final Quaternionf blockOrientation;
   private float lastRotation;

   public SimplePropellerVisual(VisualizationContext context, T blockEntity, float partialTick) {
      super(
         context,
         blockEntity,
         partialTick,
         Direction.SOUTH,
         ((Direction)blockEntity.getBlockState().getValue(BlockStateProperties.FACING)).getOpposite(),
         Models.partial(AllPartialModels.SHAFT_HALF)
      );
      Direction facing = (Direction)this.blockState.getValue(BlockStateProperties.FACING);
      Vec3i normal = facing.getNormal();
      Vec3 normalPos = new Vec3((double)normal.getX(), (double)normal.getY(), (double)normal.getZ());
      Vector3f pos = Vec3.atLowerCornerOf(this.getVisualPosition()).add(normalPos.scale(0.1875)).toVector3f();
      this.rotationAxis = Direction.get(AxisDirection.POSITIVE, this.rotationAxis()).step();
      this.blockOrientation = SimMathUtils.getBlockStateOrientation(facing);
      this.propeller = (OrientedInstance)this.instancerProvider()
         .instancer(InstanceTypes.ORIENTED, Models.partial(this.getModel(blockEntity.getBlockState())))
         .createInstance();
      this.propeller.position(pos).rotation(this.blockOrientation).setChanged();
   }

   public abstract PartialModel getModel(BlockState var1);

   public void beginFrame(Context context) {
      float angle = this.getAngle(context.partialTick());
      if (this.lastRotation != angle) {
         this.lastRotation = angle;
         ((OrientedInstance)this.propeller
               .identityRotation()
               .rotate((float) (Math.PI / 180.0) * angle, this.rotationAxis.x, this.rotationAxis.y, this.rotationAxis.z))
            .rotate(this.blockOrientation)
            .setChanged();
      }
   }

   public float getAngle(float partialTicks) {
      return 2.0F
         * (
            ((BasePropellerBlockEntity)this.blockEntity).getPreviousAngle() * (1.0F - partialTicks)
               + ((BasePropellerBlockEntity)this.blockEntity).getAngle() * partialTicks
         );
   }

   public void updateLight(float partialTick) {
      super.updateLight(partialTick);
      this.relight(this.pos, new FlatLit[]{this.propeller});
   }

   protected void _delete() {
      super._delete();
      this.propeller.delete();
   }

   public void collectCrumblingInstances(Consumer<Instance> consumer) {
      super.collectCrumblingInstances(consumer);
      consumer.accept(this.propeller);
   }
}
