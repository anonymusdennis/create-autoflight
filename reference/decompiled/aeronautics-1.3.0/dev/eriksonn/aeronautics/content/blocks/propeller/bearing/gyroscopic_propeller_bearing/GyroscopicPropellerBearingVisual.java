package dev.eriksonn.aeronautics.content.blocks.propeller.bearing.gyroscopic_propeller_bearing;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.OrientedRotatingVisual;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.instance.Instancer;
import dev.engine_room.flywheel.api.visual.DynamicVisual.Context;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import dev.eriksonn.aeronautics.index.AeroPartialModels;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.simulated_team.simulated.util.SimMathUtils;
import java.util.function.Consumer;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3d;

public class GyroscopicPropellerBearingVisual extends OrientedRotatingVisual<GyroscopicPropellerBearingBlockEntity> implements SimpleDynamicVisual {
   private final TransformedInstance topInstance;
   private final Axis rotationAxis;
   private final Quaternionf blockOrientation;
   protected TransformedInstance[] pistonHeads = new TransformedInstance[4];
   protected TransformedInstance[] pistonPoles = new TransformedInstance[4];

   public GyroscopicPropellerBearingVisual(VisualizationContext context, GyroscopicPropellerBearingBlockEntity blockEntity, float partialTick) {
      super(
         context,
         blockEntity,
         partialTick,
         Direction.SOUTH,
         ((Direction)blockEntity.getBlockState().getValue(BlockStateProperties.FACING)).getOpposite(),
         Models.partial(AllPartialModels.SHAFT_HALF)
      );
      Direction facing = (Direction)this.blockState.getValue(BlockStateProperties.FACING);
      this.rotationAxis = Axis.of(Direction.get(AxisDirection.POSITIVE, this.rotationAxis()).step());
      this.blockOrientation = SimMathUtils.getBlockStateOrientation(facing);
      PartialModel top = AeroPartialModels.BEARING_PLATE_METAL;
      this.topInstance = (TransformedInstance)this.instancerProvider()
         .instancer(InstanceTypes.TRANSFORMED, Models.partial(AeroPartialModels.BEARING_PLATE_METAL))
         .createInstance();
      Instancer<TransformedInstance> headProvider = this.instancerProvider()
         .instancer(InstanceTypes.TRANSFORMED, Models.partial(AeroPartialModels.GYRO_BEARING_PISTON_HEAD));
      Instancer<TransformedInstance> poleProvider = this.instancerProvider()
         .instancer(InstanceTypes.TRANSFORMED, Models.partial(AeroPartialModels.GYRO_BEARING_PISTON_POLE));

      for (int i = 0; i < 4; i++) {
         this.pistonPoles[i] = (TransformedInstance)poleProvider.createInstance();
         this.pistonHeads[i] = (TransformedInstance)headProvider.createInstance();
      }
   }

   public void beginFrame(Context ctx) {
      float interpolatedAngle = ((GyroscopicPropellerBearingBlockEntity)this.blockEntity).getInterpolatedAngle(ctx.partialTick() - 1.0F);
      this.topInstance.setIdentityTransform();
      this.topInstance.translate(this.getVisualPosition());
      Quaternionf tilt = new Quaternionf(((GyroscopicPropellerBearingBlockEntity)this.blockEntity).previousTiltQuat)
         .slerp(new Quaternionf(((GyroscopicPropellerBearingBlockEntity)this.blockEntity).tiltQuat), ctx.partialTick());
      ((TransformedInstance)((TransformedInstance)((TransformedInstance)this.topInstance
                  .translate(JOMLConversion.toMojang(((GyroscopicPropellerBearingBlockEntity)this.blockEntity).blockNormal).scale(0.25)))
               .rotateCentered(tilt))
            .rotateCentered(this.rotationAxis.rotationDegrees(interpolatedAngle)))
         .translate(JOMLConversion.toMojang(((GyroscopicPropellerBearingBlockEntity)this.blockEntity).blockNormal).scale(-0.25));
      this.topInstance.rotateCentered(this.blockOrientation);
      this.topInstance.setChanged();
      PoseStack ms = new PoseStack();
      TransformStack<PoseTransformStack> msr = TransformStack.of(ms);
      msr.translate(this.getVisualPosition());
      msr.center();
      msr.rotate(this.blockOrientation);

      for (int i = 0; i < 4; i++) {
         Vector3d originalPos = JOMLConversion.toJOML(VecHelper.rotate(new Vec3(0.375, 0.0, 0.0), (double)(-90 * i), net.minecraft.core.Direction.Axis.Y));
         Vector3d translatedPos = new Vector3d(originalPos);
         this.blockOrientation.transform(translatedPos);
         double translateDistance = -translatedPos.dot(((GyroscopicPropellerBearingBlockEntity)this.blockEntity).tiltVector)
            / ((GyroscopicPropellerBearingBlockEntity)this.blockEntity).blockNormal.dot(((GyroscopicPropellerBearingBlockEntity)this.blockEntity).tiltVector);
         translatedPos = originalPos.add(0.0, translateDistance + 0.1875, 0.0);
         msr.pushPose();
         msr.translate(translatedPos.x, translatedPos.y, translatedPos.z);
         msr.pushPose();
         msr.rotate((float)Math.toRadians((double)(-90 * i)), net.minecraft.core.Direction.Axis.Y);
         msr.translate(-0.0125, 0.03125, 0.0);
         this.pistonPoles[i].setTransform(ms);
         msr.popPose();
         Quaternionf Q = new Quaternionf(this.blockOrientation);
         Q.conjugate();
         Q.mul(tilt);
         Q.mul(this.blockOrientation);
         msr.rotate(Q);
         msr.rotate((float)Math.toRadians((double)(-90 * i)), net.minecraft.core.Direction.Axis.Y);
         this.pistonHeads[i].setTransform(ms);
         msr.popPose();
         this.pistonHeads[i].setChanged();
         this.pistonPoles[i].setChanged();
      }
   }

   public void updateLight(float partialTick) {
      super.updateLight(partialTick);
      this.relight(new FlatLit[]{this.topInstance});
      this.relight(this.pistonHeads);
      this.relight(this.pistonPoles);
   }

   protected void _delete() {
      super._delete();
      this.topInstance.delete();

      for (int i = 0; i < 4; i++) {
         this.pistonHeads[i].delete();
         this.pistonPoles[i].delete();
      }
   }

   public void collectCrumblingInstances(Consumer<Instance> consumer) {
      super.collectCrumblingInstances(consumer);
      consumer.accept(this.topInstance);
   }
}
