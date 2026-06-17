package com.simibubi.create.content.processing.basin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.data.IntAttached;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.platform.NeoForgeCatnipServices;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;

public class BasinRenderer extends SmartBlockEntityRenderer<BasinBlockEntity> {
   public BasinRenderer(Context context) {
      super(context);
   }

   protected void renderSafe(BasinBlockEntity basin, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      super.renderSafe(basin, partialTicks, ms, buffer, light, overlay);
      float fluidLevel = this.renderFluids(basin, partialTicks, ms, buffer, light, overlay);
      float level = Mth.clamp(fluidLevel - 0.3F, 0.125F, 0.6F);
      ms.pushPose();
      BlockPos pos = basin.getBlockPos();
      ms.translate(0.5, 0.2F, 0.5);
      TransformStack.of(ms).rotateYDegrees(basin.ingredientRotation.getValue(partialTicks));
      RandomSource r = RandomSource.create((long)pos.hashCode());
      Vec3 baseVector = new Vec3(0.125, (double)level, 0.0);
      IItemHandlerModifiable inv = basin.itemCapability;
      if (inv == null) {
         inv = new ItemStackHandler();
      }

      int itemCount = 0;

      for (int slot = 0; slot < inv.getSlots(); slot++) {
         if (!inv.getStackInSlot(slot).isEmpty()) {
            itemCount++;
         }
      }

      if (itemCount == 1) {
         baseVector = new Vec3(0.0, (double)level, 0.0);
      }

      float anglePartition = 360.0F / (float)itemCount;

      for (int slotx = 0; slotx < inv.getSlots(); slotx++) {
         ItemStack stack = inv.getStackInSlot(slotx);
         if (!stack.isEmpty()) {
            ms.pushPose();
            if (fluidLevel > 0.0F) {
               ms.translate(
                  0.0F, (Mth.sin(AnimationTickHolder.getRenderTime(basin.getLevel()) / 12.0F + anglePartition * (float)itemCount) + 1.5F) * 1.0F / 32.0F, 0.0F
               );
            }

            Vec3 itemPosition = VecHelper.rotate(baseVector, (double)(anglePartition * (float)itemCount), Axis.Y);
            ms.translate(itemPosition.x, itemPosition.y, itemPosition.z);
            ((PoseTransformStack)TransformStack.of(ms).rotateYDegrees(anglePartition * (float)itemCount + 35.0F)).rotateXDegrees(65.0F);

            for (int i = 0; i <= stack.getCount() / 8; i++) {
               ms.pushPose();
               Vec3 vec = VecHelper.offsetRandomly(Vec3.ZERO, r, 0.0625F);
               ms.translate(vec.x, vec.y, vec.z);
               this.renderItem(ms, buffer, light, overlay, stack);
               ms.popPose();
            }

            ms.popPose();
            itemCount--;
         }
      }

      ms.popPose();
      BlockState blockState = basin.getBlockState();
      if (blockState.getBlock() instanceof BasinBlock) {
         Direction direction = (Direction)blockState.getValue(BasinBlock.FACING);
         if (direction != Direction.DOWN) {
            Vec3 directionVec = Vec3.atLowerCornerOf(direction.getNormal());
            Vec3 outVec = VecHelper.getCenterOf(BlockPos.ZERO).add(directionVec.scale(0.55).subtract(0.0, 0.5, 0.0));
            boolean outToBasin = basin.getLevel().getBlockState(basin.getBlockPos().relative(direction)).getBlock() instanceof BasinBlock;

            for (IntAttached<ItemStack> intAttached : basin.visualizedOutputItems) {
               float progress = 1.0F - ((float)((Integer)intAttached.getFirst()).intValue() - partialTicks) / 10.0F;
               if (outToBasin || !(progress > 0.35F)) {
                  ms.pushPose();
                  ((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)TransformStack.of(ms).translate(outVec))
                              .translate(new Vec3(0.0, (double)Math.max(-0.55F, -(progress * progress * 2.0F)), 0.0)))
                           .translate(directionVec.scale((double)(progress * 0.5F))))
                        .rotateYDegrees(AngleHelper.horizontalAngle(direction)))
                     .rotateXDegrees(progress * 180.0F);
                  this.renderItem(ms, buffer, light, overlay, (ItemStack)intAttached.getValue());
                  ms.popPose();
               }
            }
         }
      }
   }

   protected void renderItem(PoseStack ms, MultiBufferSource buffer, int light, int overlay, ItemStack stack) {
      Minecraft mc = Minecraft.getInstance();
      mc.getItemRenderer().renderStatic(stack, ItemDisplayContext.GROUND, light, overlay, ms, buffer, mc.level, 0);
   }

   protected float renderFluids(BasinBlockEntity basin, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      SmartFluidTankBehaviour inputFluids = basin.getBehaviour(SmartFluidTankBehaviour.INPUT);
      SmartFluidTankBehaviour outputFluids = basin.getBehaviour(SmartFluidTankBehaviour.OUTPUT);
      SmartFluidTankBehaviour[] tanks = new SmartFluidTankBehaviour[]{inputFluids, outputFluids};
      float totalUnits = basin.getTotalFluidUnits(partialTicks);
      if (totalUnits < 1.0F) {
         return 0.0F;
      } else {
         float fluidLevel = Mth.clamp(totalUnits / 2000.0F, 0.0F, 1.0F);
         fluidLevel = 1.0F - (1.0F - fluidLevel) * (1.0F - fluidLevel);
         float xMin = 0.125F;
         float xMax = 0.125F;
         float yMin = 0.125F;
         float yMax = 0.125F + 0.75F * fluidLevel;
         float zMin = 0.125F;
         float zMax = 0.875F;

         for (SmartFluidTankBehaviour behaviour : tanks) {
            if (behaviour != null) {
               for (SmartFluidTankBehaviour.TankSegment tankSegment : behaviour.getTanks()) {
                  FluidStack renderedFluid = tankSegment.getRenderedFluid();
                  if (!renderedFluid.isEmpty()) {
                     float units = tankSegment.getTotalUnits(partialTicks);
                     if (!(units < 1.0F)) {
                        float partial = Mth.clamp(units / totalUnits, 0.0F, 1.0F);
                        xMax += partial * 12.0F / 16.0F;
                        NeoForgeCatnipServices.FLUID_RENDERER
                           .renderFluidBox(renderedFluid, xMin, 0.125F, 0.125F, xMax, yMax, 0.875F, buffer, ms, light, false, false);
                        xMin = xMax;
                     }
                  }
               }
            }
         }

         return yMax;
      }
   }

   public int getViewDistance() {
      return 16;
   }
}
