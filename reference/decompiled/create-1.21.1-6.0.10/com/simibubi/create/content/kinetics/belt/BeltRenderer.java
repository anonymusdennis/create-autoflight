package com.simibubi.create.content.kinetics.belt;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.AllSpriteShifts;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.belt.transport.BeltInventory;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.render.ShadowRenderHelper;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import java.util.Random;
import java.util.function.Supplier;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.levelWrappers.WrappedLevel;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SpriteShiftEntry;
import net.createmod.catnip.render.SuperByteBuffer;
import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class BeltRenderer extends SafeBlockEntityRenderer<BeltBlockEntity> {
   public BeltRenderer(Context context) {
   }

   public boolean shouldRenderOffScreen(BeltBlockEntity be) {
      return be.isController();
   }

   protected void renderSafe(BeltBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      if (!VisualizationManager.supportsVisualization(be.getLevel())) {
         BlockState blockState = be.getBlockState();
         if (!AllBlocks.BELT.has(blockState)) {
            return;
         }

         BeltSlope beltSlope = (BeltSlope)blockState.getValue(BeltBlock.SLOPE);
         BeltPart part = (BeltPart)blockState.getValue(BeltBlock.PART);
         Direction facing = (Direction)blockState.getValue(BeltBlock.HORIZONTAL_FACING);
         AxisDirection axisDirection = facing.getAxisDirection();
         boolean downward = beltSlope == BeltSlope.DOWNWARD;
         boolean upward = beltSlope == BeltSlope.UPWARD;
         boolean diagonal = downward || upward;
         boolean start = part == BeltPart.START;
         boolean end = part == BeltPart.END;
         boolean sideways = beltSlope == BeltSlope.SIDEWAYS;
         boolean alongX = facing.getAxis() == Axis.X;
         PoseStack localTransforms = new PoseStack();
         PoseTransformStack msr = TransformStack.of(localTransforms);
         VertexConsumer vb = buffer.getBuffer(RenderType.solid());
         float renderTick = AnimationTickHolder.getRenderTime(be.getLevel());
         ((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)msr.center())
                     .rotateYDegrees(AngleHelper.horizontalAngle(facing) + (float)(upward ? 180 : 0) + (float)(sideways ? 270 : 0)))
                  .rotateZDegrees(sideways ? 90.0F : 0.0F))
               .rotateXDegrees(!diagonal && beltSlope != BeltSlope.HORIZONTAL ? 90.0F : 0.0F))
            .uncenter();
         if (downward || beltSlope == BeltSlope.VERTICAL && axisDirection == AxisDirection.POSITIVE) {
            boolean b = start;
            start = end;
            end = b;
         }

         DyeColor color = be.color.orElse(null);

         for (boolean bottom : Iterate.trueAndFalse) {
            PartialModel beltPartial = getBeltPartial(diagonal, start, end, bottom);
            SuperByteBuffer beltBuffer = CachedBuffers.partial(beltPartial, blockState).light(light);
            SpriteShiftEntry spriteShift = getSpriteShiftEntry(color, diagonal, bottom);
            float speed = be.getSpeed();
            if (speed != 0.0F || be.color.isPresent()) {
               float time = renderTick * (float)axisDirection.getStep();
               if (diagonal && downward ^ alongX || !sideways && !diagonal && alongX || sideways && axisDirection == AxisDirection.NEGATIVE) {
                  speed = -speed;
               }

               float scrollMult = diagonal ? 0.375F : 0.5F;
               float spriteSize = spriteShift.getTarget().getV1() - spriteShift.getTarget().getV0();
               double scroll = (double)(speed * time) / 504.0 + (bottom ? 0.5 : 0.0);
               scroll -= Math.floor(scroll);
               scroll = scroll * (double)spriteSize * (double)scrollMult;
               beltBuffer.shiftUVScrolling(spriteShift, (float)scroll);
            }

            ((SuperByteBuffer)beltBuffer.transform(localTransforms)).renderInto(ms, vb);
            if (diagonal) {
               break;
            }
         }

         if (be.hasPulley()) {
            Direction dir = sideways ? Direction.UP : ((Direction)blockState.getValue(BeltBlock.HORIZONTAL_FACING)).getClockWise();
            Supplier<PoseStack> matrixStackSupplier = () -> {
               PoseStack stack = new PoseStack();
               PoseTransformStack stacker = TransformStack.of(stack);
               stacker.center();
               if (dir.getAxis() == Axis.X) {
                  stacker.rotateYDegrees(90.0F);
               }

               if (dir.getAxis() == Axis.Y) {
                  stacker.rotateXDegrees(90.0F);
               }

               stacker.rotateXDegrees(90.0F);
               stacker.uncenter();
               return stack;
            };
            SuperByteBuffer superBuffer = CachedBuffers.partialDirectional(AllPartialModels.BELT_PULLEY, blockState, dir, matrixStackSupplier);
            KineticBlockEntityRenderer.standardKineticRotationTransform(superBuffer, be, light).renderInto(ms, vb);
         }
      }

      this.renderItems(be, partialTicks, ms, buffer, light, overlay);
   }

   public static SpriteShiftEntry getSpriteShiftEntry(DyeColor color, boolean diagonal, boolean bottom) {
      if (color != null) {
         return (diagonal ? AllSpriteShifts.DYED_DIAGONAL_BELTS : (bottom ? AllSpriteShifts.DYED_OFFSET_BELTS : AllSpriteShifts.DYED_BELTS)).get(color);
      } else {
         return diagonal ? AllSpriteShifts.BELT_DIAGONAL : (bottom ? AllSpriteShifts.BELT_OFFSET : AllSpriteShifts.BELT);
      }
   }

   public static PartialModel getBeltPartial(boolean diagonal, boolean start, boolean end, boolean bottom) {
      if (diagonal) {
         if (start) {
            return AllPartialModels.BELT_DIAGONAL_START;
         } else {
            return end ? AllPartialModels.BELT_DIAGONAL_END : AllPartialModels.BELT_DIAGONAL_MIDDLE;
         }
      } else if (bottom) {
         if (start) {
            return AllPartialModels.BELT_START_BOTTOM;
         } else {
            return end ? AllPartialModels.BELT_END_BOTTOM : AllPartialModels.BELT_MIDDLE_BOTTOM;
         }
      } else if (start) {
         return AllPartialModels.BELT_START;
      } else {
         return end ? AllPartialModels.BELT_END : AllPartialModels.BELT_MIDDLE;
      }
   }

   protected void renderItems(BeltBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      if (be.isController()) {
         if (be.beltLength != 0) {
            ms.pushPose();
            Direction beltFacing = be.getBeltFacing();
            Vec3i directionVec = beltFacing.getNormal();
            Vec3 beltStartOffset = Vec3.atLowerCornerOf(directionVec).scale(-0.5).add(0.5, 0.9375, 0.5);
            ms.translate(beltStartOffset.x, beltStartOffset.y, beltStartOffset.z);
            BeltSlope slope = (BeltSlope)be.getBlockState().getValue(BeltBlock.SLOPE);
            int verticality = slope == BeltSlope.DOWNWARD ? -1 : (slope == BeltSlope.UPWARD ? 1 : 0);
            boolean slopeAlongX = beltFacing.getAxis() == Axis.X;
            boolean onContraption = be.getLevel() instanceof WrappedLevel;
            BeltInventory inventory = be.getInventory();

            for (TransportedItemStack transported : inventory.getTransportedItems()) {
               this.renderItem(
                  be,
                  partialTicks,
                  ms,
                  buffer,
                  light,
                  overlay,
                  beltFacing,
                  directionVec,
                  slope,
                  verticality,
                  slopeAlongX,
                  onContraption,
                  transported,
                  beltStartOffset
               );
            }

            if (inventory.getLazyClientItem() != null) {
               this.renderItem(
                  be,
                  partialTicks,
                  ms,
                  buffer,
                  light,
                  overlay,
                  beltFacing,
                  directionVec,
                  slope,
                  verticality,
                  slopeAlongX,
                  onContraption,
                  inventory.getLazyClientItem(),
                  beltStartOffset
               );
            }

            ms.popPose();
         }
      }
   }

   private void renderItem(
      BeltBlockEntity be,
      float partialTicks,
      PoseStack ms,
      MultiBufferSource buffer,
      int light,
      int overlay,
      Direction beltFacing,
      Vec3i directionVec,
      BeltSlope slope,
      int verticality,
      boolean slopeAlongX,
      boolean onContraption,
      TransportedItemStack transported,
      Vec3 beltStartOffset
   ) {
      Minecraft mc = Minecraft.getInstance();
      ItemRenderer itemRenderer = mc.getItemRenderer();
      MutableBlockPos mutablePos = new MutableBlockPos();
      float offset = Mth.lerp(partialTicks, transported.prevBeltPosition, transported.beltPosition);
      float sideOffset = Mth.lerp(partialTicks, transported.prevSideOffset, transported.sideOffset);
      float verticalMovement = (float)verticality;
      if (be.getSpeed() == 0.0F) {
         offset = transported.beltPosition;
         sideOffset = transported.sideOffset;
      }

      if ((double)offset < 0.5) {
         verticalMovement = 0.0F;
      } else {
         verticalMovement = (float)verticality * (Math.min(offset, (float)be.beltLength - 0.5F) - 0.5F);
      }

      Vec3 offsetVec = Vec3.atLowerCornerOf(directionVec).scale((double)offset);
      if (verticalMovement != 0.0F) {
         offsetVec = offsetVec.add(0.0, (double)verticalMovement, 0.0);
      }

      boolean onSlope = slope != BeltSlope.HORIZONTAL && Mth.clamp(offset, 0.5F, (float)be.beltLength - 0.5F) == offset;
      boolean tiltForward = (slope == BeltSlope.DOWNWARD ^ beltFacing.getAxisDirection() == AxisDirection.POSITIVE) == (beltFacing.getAxis() == Axis.Z);
      float slopeAngle = onSlope ? (tiltForward ? -45.0F : 45.0F) : 0.0F;
      Vec3 itemPos = beltStartOffset.add((double)be.getBlockPos().getX(), (double)be.getBlockPos().getY(), (double)be.getBlockPos().getZ()).add(offsetVec);
      if (!this.shouldCullItem(itemPos, be.getLevel())) {
         ms.pushPose();
         TransformStack.of(ms).nudge(transported.angle);
         ms.translate(offsetVec.x, offsetVec.y, offsetVec.z);
         boolean alongX = beltFacing.getClockWise().getAxis() == Axis.X;
         if (!alongX) {
            sideOffset *= -1.0F;
         }

         ms.translate(alongX ? sideOffset : 0.0F, 0.0F, alongX ? 0.0F : sideOffset);
         int stackLight;
         if (onContraption) {
            stackLight = light;
         } else {
            int segment = (int)Math.floor((double)offset);
            mutablePos.set(be.getBlockPos()).move(directionVec.getX() * segment, verticality * segment, directionVec.getZ() * segment);
            stackLight = LevelRenderer.getLightColor(be.getLevel(), mutablePos);
         }

         boolean renderUpright = BeltHelper.isItemUpright(transported.stack);
         BakedModel bakedModel = itemRenderer.getModel(transported.stack, be.getLevel(), null, 0);
         boolean blockItem = bakedModel.isGui3d();
         int count = 0;
         if (be.getLevel() instanceof PonderLevel || mc.player.getEyePosition(1.0F).distanceTo(itemPos) < 16.0) {
            count = Mth.log2(transported.stack.getCount()) / 2;
         }

         Random r = new Random((long)transported.angle);
         boolean slopeShadowOnly = renderUpright && onSlope;
         float slopeOffset = 0.125F;
         if (slopeShadowOnly) {
            ms.pushPose();
         }

         if (!renderUpright || slopeShadowOnly) {
            ms.mulPose((slopeAlongX ? com.mojang.math.Axis.ZP : com.mojang.math.Axis.XP).rotationDegrees(slopeAngle));
         }

         if (onSlope) {
            ms.translate(0.0F, slopeOffset, 0.0F);
         }

         ms.pushPose();
         ms.translate(0.0F, -0.12F, 0.0F);
         ShadowRenderHelper.renderShadow(ms, buffer, 0.75F, 0.2F);
         ms.popPose();
         if (slopeShadowOnly) {
            ms.popPose();
            ms.translate(0.0F, slopeOffset, 0.0F);
         }

         if (renderUpright) {
            Vec3 cameraPosition = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            Vec3 vectorForOffset = BeltHelper.getVectorForOffset(be, offset);
            Vec3 diff = vectorForOffset.subtract(cameraPosition);
            float yRot = (float)(Mth.atan2(diff.x, diff.z) + Math.PI);
            ms.mulPose(com.mojang.math.Axis.YP.rotation(yRot));
            ms.translate(0.0, 0.09375, 0.0625);
         }

         for (int i = 0; i <= count; i++) {
            ms.pushPose();
            boolean box = PackageItem.isPackage(transported.stack);
            ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees((float)transported.angle));
            if (!blockItem && !renderUpright) {
               ms.translate(0.0, -0.09375, 0.0);
               ms.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90.0F));
            }

            if (blockItem && !box) {
               ms.translate(r.nextFloat() * 0.0625F * (float)i, 0.0F, r.nextFloat() * 0.0625F * (float)i);
            }

            if (box) {
               ms.translate(0.0F, 0.25F, 0.0F);
               ms.scale(1.5F, 1.5F, 1.5F);
            } else {
               ms.scale(0.5F, 0.5F, 0.5F);
            }

            itemRenderer.render(transported.stack, ItemDisplayContext.FIXED, false, ms, buffer, stackLight, overlay, bakedModel);
            ms.popPose();
            if (!renderUpright) {
               if (!blockItem) {
                  ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(10.0F));
               }

               ms.translate(0.0, blockItem ? 0.015625 : 0.0625, 0.0);
            } else {
               ms.translate(0.0F, 0.0F, -0.0625F);
            }
         }

         ms.popPose();
      }
   }
}
