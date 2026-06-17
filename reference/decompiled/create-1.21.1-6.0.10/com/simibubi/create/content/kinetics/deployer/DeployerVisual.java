package com.simibubi.create.content.kinetics.deployer;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.ShaftVisual;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.TickableVisual.Context;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.OrientedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;
import java.util.function.Consumer;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

public class DeployerVisual extends ShaftVisual<DeployerBlockEntity> implements SimpleDynamicVisual, SimpleTickableVisual {
   final Direction facing = (Direction)this.blockState.getValue(DirectionalKineticBlock.FACING);
   final float yRot;
   final float xRot;
   final float zRot;
   protected final OrientedInstance pole;
   protected OrientedInstance hand;
   PartialModel currentHand;
   float progress;

   public DeployerVisual(VisualizationContext context, DeployerBlockEntity blockEntity, float partialTick) {
      super(context, blockEntity, partialTick);
      boolean rotatePole = (Boolean)this.blockState.getValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE) ^ this.facing.getAxis() == Axis.Z;
      this.yRot = AngleHelper.horizontalAngle(this.facing);
      this.xRot = this.facing == Direction.UP ? 270.0F : (this.facing == Direction.DOWN ? 90.0F : 0.0F);
      this.zRot = rotatePole ? 90.0F : 0.0F;
      this.pole = (OrientedInstance)this.instancerProvider().instancer(InstanceTypes.ORIENTED, Models.partial(AllPartialModels.DEPLOYER_POLE)).createInstance();
      this.currentHand = ((DeployerBlockEntity)this.blockEntity).getHandPose();
      this.hand = (OrientedInstance)this.instancerProvider().instancer(InstanceTypes.ORIENTED, Models.partial(this.currentHand)).createInstance();
      this.progress = this.getProgress(partialTick);
      updateRotation(this.pole, this.hand, this.yRot, this.xRot, this.zRot);
      this.updatePosition();
   }

   @Override
   public void tick(Context context) {
      PartialModel handPose = ((DeployerBlockEntity)this.blockEntity).getHandPose();
      if (this.currentHand != handPose) {
         this.currentHand = handPose;
         this.instancerProvider().instancer(InstanceTypes.ORIENTED, Models.partial(this.currentHand)).stealInstance(this.hand);
      }
   }

   public void beginFrame(dev.engine_room.flywheel.api.visual.DynamicVisual.Context ctx) {
      float newProgress = this.getProgress(ctx.partialTick());
      if (!Mth.equal(newProgress, this.progress)) {
         this.progress = newProgress;
         this.updatePosition();
      }
   }

   @Override
   public void updateLight(float partialTick) {
      super.updateLight(partialTick);
      this.relight(new FlatLit[]{this.hand, this.pole});
   }

   @Override
   protected void _delete() {
      super._delete();
      this.hand.delete();
      this.pole.delete();
   }

   private float getProgress(float partialTicks) {
      if (((DeployerBlockEntity)this.blockEntity).state == DeployerBlockEntity.State.EXPANDING) {
         float f = 1.0F
            - ((float)((DeployerBlockEntity)this.blockEntity).timer - partialTicks * (float)((DeployerBlockEntity)this.blockEntity).getTimerSpeed()) / 1000.0F;
         if (((DeployerBlockEntity)this.blockEntity).fistBump) {
            f *= f;
         }

         return f;
      } else {
         return ((DeployerBlockEntity)this.blockEntity).state == DeployerBlockEntity.State.RETRACTING
            ? ((float)((DeployerBlockEntity)this.blockEntity).timer - partialTicks * (float)((DeployerBlockEntity)this.blockEntity).getTimerSpeed()) / 1000.0F
            : 0.0F;
      }
   }

   private void updatePosition() {
      float handLength = this.currentHand == AllPartialModels.DEPLOYER_HAND_POINTING
         ? 0.0F
         : (this.currentHand == AllPartialModels.DEPLOYER_HAND_HOLDING ? 0.25F : 0.1875F);
      float distance = Math.min(Mth.clamp(this.progress, 0.0F, 1.0F) * (((DeployerBlockEntity)this.blockEntity).reach + handLength), 1.3125F);
      Vec3i facingVec = this.facing.getNormal();
      BlockPos blockPos = this.getVisualPosition();
      float x = (float)blockPos.getX() + (float)facingVec.getX() * distance;
      float y = (float)blockPos.getY() + (float)facingVec.getY() * distance;
      float z = (float)blockPos.getZ() + (float)facingVec.getZ() * distance;
      this.pole.position(x, y, z).setChanged();
      this.hand.position(x, y, z).setChanged();
   }

   static void updateRotation(OrientedInstance pole, OrientedInstance hand, float yRot, float xRot, float zRot) {
      Quaternionf q = com.mojang.math.Axis.YP.rotationDegrees(yRot);
      q.mul(com.mojang.math.Axis.XP.rotationDegrees(xRot));
      hand.rotation(q).setChanged();
      q.mul(com.mojang.math.Axis.ZP.rotationDegrees(zRot));
      pole.rotation(q).setChanged();
   }

   @Override
   public void collectCrumblingInstances(Consumer<Instance> consumer) {
      super.collectCrumblingInstances(consumer);
      consumer.accept(this.pole);
      consumer.accept(this.hand);
   }
}
