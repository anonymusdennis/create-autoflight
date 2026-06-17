package com.simibubi.create.content.kinetics.chainConveyor;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.foundation.render.SpecialModels;
import dev.engine_room.flywheel.api.instance.Instancer;
import dev.engine_room.flywheel.api.visual.TickableVisual.Context;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;
import dev.engine_room.flywheel.lib.visual.util.SmartRecycler;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

public class ChainConveyorVisual extends SingleAxisRotatingVisual<ChainConveyorBlockEntity> implements SimpleDynamicVisual, SimpleTickableVisual {
   private final List<TransformedInstance> guards = new ArrayList<>();
   private final SmartRecycler<ResourceLocation, TransformedInstance> boxes;
   private final SmartRecycler<ResourceLocation, TransformedInstance> rigging;

   public ChainConveyorVisual(VisualizationContext context, ChainConveyorBlockEntity blockEntity, float partialTick) {
      super(context, blockEntity, partialTick, Models.partial(AllPartialModels.CHAIN_CONVEYOR_SHAFT));
      this.setupGuards();
      this.boxes = new SmartRecycler(
         key -> (TransformedInstance)this.instancerProvider()
               .instancer(InstanceTypes.TRANSFORMED, Models.partial(AllPartialModels.PACKAGES.get(key)))
               .createInstance()
      );
      this.rigging = new SmartRecycler(
         key -> (TransformedInstance)this.instancerProvider()
               .instancer(InstanceTypes.TRANSFORMED, Models.partial(AllPartialModels.PACKAGE_RIGGING.get(key)))
               .createInstance()
      );
   }

   @Override
   public void update(float pt) {
      super.update(pt);
      this.setupGuards();
   }

   @Override
   public void tick(Context context) {
      ((ChainConveyorBlockEntity)this.blockEntity).tickBoxVisuals();
   }

   public void beginFrame(dev.engine_room.flywheel.api.visual.DynamicVisual.Context ctx) {
      float partialTicks = ctx.partialTick();
      this.boxes.resetCount();
      this.rigging.resetCount();

      for (ChainConveyorPackage box : ((ChainConveyorBlockEntity)this.blockEntity).loopingPackages) {
         this.setupBoxVisual((ChainConveyorBlockEntity)this.blockEntity, box, partialTicks);
      }

      for (Entry<BlockPos, List<ChainConveyorPackage>> entry : ((ChainConveyorBlockEntity)this.blockEntity).travellingPackages.entrySet()) {
         for (ChainConveyorPackage box : entry.getValue()) {
            this.setupBoxVisual((ChainConveyorBlockEntity)this.blockEntity, box, partialTicks);
         }
      }

      this.boxes.discardExtra();
      this.rigging.discardExtra();
   }

   private void setupBoxVisual(ChainConveyorBlockEntity be, ChainConveyorPackage box, float partialTicks) {
      if (box.worldPosition != null) {
         if (box.item != null && !box.item.isEmpty()) {
            ChainConveyorPackage.ChainConveyorPackagePhysicsData physicsData = box.physicsData(be.getLevel());
            if (physicsData.prevPos != null) {
               Vec3 position = physicsData.prevPos.lerp(physicsData.pos, (double)partialTicks);
               Vec3 targetPosition = physicsData.prevTargetPos.lerp(physicsData.targetPos, (double)partialTicks);
               float yaw = AngleHelper.angleLerp((double)partialTicks, (double)physicsData.prevYaw, (double)physicsData.yaw);
               Vec3 offset = new Vec3(
                  targetPosition.x - (double)this.pos.getX(), targetPosition.y - (double)this.pos.getY(), targetPosition.z - (double)this.pos.getZ()
               );
               BlockPos containingPos = BlockPos.containing(position);
               Level level = be.getLevel();
               int light = LightTexture.pack(level.getBrightness(LightLayer.BLOCK, containingPos), level.getBrightness(LightLayer.SKY, containingPos));
               if (physicsData.modelKey == null) {
                  ResourceLocation key = BuiltInRegistries.ITEM.getKey(box.item.getItem());
                  if (key == BuiltInRegistries.ITEM.getDefaultKey()) {
                     return;
                  }

                  physicsData.modelKey = key;
               }

               TransformedInstance rigBuffer = (TransformedInstance)this.rigging.get(physicsData.modelKey);
               TransformedInstance boxBuffer = (TransformedInstance)this.boxes.get(physicsData.modelKey);
               Vec3 dangleDiff = VecHelper.rotate(targetPosition.add(0.0, 0.5, 0.0).subtract(position), (double)(-yaw), Axis.Y);
               float zRot = Mth.wrapDegrees((float)Mth.atan2(-dangleDiff.x, dangleDiff.y) * (180.0F / (float)Math.PI)) / 2.0F;
               float xRot = Mth.wrapDegrees((float)Mth.atan2(dangleDiff.z, dangleDiff.y) * (180.0F / (float)Math.PI)) / 2.0F;
               zRot = Mth.clamp(zRot, -25.0F, 25.0F);
               xRot = Mth.clamp(xRot, -25.0F, 25.0F);

               for (TransformedInstance buf : new TransformedInstance[]{rigBuffer, boxBuffer}) {
                  buf.setIdentityTransform();
                  buf.translate(this.getVisualPosition());
                  buf.translate(offset);
                  buf.translate(0.0F, 0.625F, 0.0F);
                  buf.rotateYDegrees(yaw);
                  buf.rotateZDegrees(zRot);
                  buf.rotateXDegrees(xRot);
                  if (physicsData.flipped && buf == rigBuffer) {
                     buf.rotateYDegrees(180.0F);
                  }

                  buf.uncenter();
                  buf.translate(0.0F, -PackageItem.getHookDistance(box.item) + 0.4375F, 0.0F);
                  buf.light(light);
                  buf.setChanged();
               }
            }
         }
      }
   }

   private void deleteGuards() {
      for (TransformedInstance guard : this.guards) {
         guard.delete();
      }

      this.guards.clear();
   }

   private void setupGuards() {
      this.deleteGuards();
      Instancer<TransformedInstance> wheelInstancer = this.instancerProvider()
         .instancer(InstanceTypes.TRANSFORMED, SpecialModels.chunkDiffuse(AllPartialModels.CHAIN_CONVEYOR_WHEEL));
      Instancer<TransformedInstance> guardInstancer = this.instancerProvider()
         .instancer(InstanceTypes.TRANSFORMED, SpecialModels.chunkDiffuse(AllPartialModels.CHAIN_CONVEYOR_GUARD));
      TransformedInstance wheel = (TransformedInstance)wheelInstancer.createInstance();
      ((TransformedInstance)wheel.translate(this.getVisualPosition())).light(this.rotatingModel.light).setChanged();
      this.guards.add(wheel);

      for (BlockPos blockPos : ((ChainConveyorBlockEntity)this.blockEntity).connections) {
         ChainConveyorBlockEntity.ConnectionStats stats = ((ChainConveyorBlockEntity)this.blockEntity).connectionStats.get(blockPos);
         if (stats != null) {
            Vec3 diff = stats.end().subtract(stats.start());
            double yaw = 180.0F / (float)Math.PI * Mth.atan2(diff.x, diff.z);
            TransformedInstance guard = (TransformedInstance)guardInstancer.createInstance();
            ((TransformedInstance)((TransformedInstance)((TransformedInstance)((TransformedInstance)guard.translate(this.getVisualPosition())).center())
                     .rotateYDegrees((float)yaw))
                  .uncenter())
               .light(this.rotatingModel.light)
               .setChanged();
            this.guards.add(guard);
         }
      }
   }

   @Override
   public void updateLight(float partialTick) {
      super.updateLight(partialTick);

      for (TransformedInstance guard : this.guards) {
         this.relight(new FlatLit[]{guard});
      }
   }

   @Override
   protected void _delete() {
      super._delete();
      this.deleteGuards();
      this.boxes.delete();
      this.rigging.delete();
   }
}
